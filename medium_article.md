# Building Swiggy-Style Cart Animations in Jetpack Compose

## A flying food token, spring-loaded list items, and swipe-to-delete — built entirely with Compose's animation APIs

---

You tap the "+" button on a menu item. Instead of the counter just ticking up silently, a small circular token lifts off the card, arcs through the air, spins once, and lands inside the cart icon — which bounces as if it felt the weight. The bill updates with a smooth number roll.

That's the animation I wanted to build.

Not because it's flashy. Because it gives users a physical metaphor for what's happening: *something real just moved into your cart.* It reduces the cognitive load of "did that actually add?" It feels good to use.

In this article I'll walk through three animation systems I built in a demo project that replicates this Swiggy-style experience:

1. **The Flying Token** — an item arcs from the menu tile to the cart icon using a Bezier path
2. **List Entry Animation** — new items spring into the cart list with a staggered slide
3. **Swipe-to-Delete** — drag an item left to reveal a delete hint, drag far enough to dismiss

The project is pure Jetpack Compose. No ViewModel, no MVI framework. State is managed with Compose primitives — `remember`, `mutableStateOf`, `Animatable`, `derivedStateOf`. I'll explain why that choice made sense for a demo project, and where you'd want to layer in a ViewModel for production.

---

## The Architecture: Compose State as the Source of Truth

Before the animations, let me set the stage quickly.

The cart screen owns its own state. Everything — the item list, animation flags, computed totals — lives in `remember` blocks inside the composable:

```kotlin
val items = remember {
    mutableStateListOf(
        CartItem("biryani", "Hyderabadi Biryani", "Boneless · Medium spicy", 349, 1),
        CartItem("naan", "Garlic Butter Naan", "Tandoor · Pack of 2", 79, 2),
        CartItem("lassi", "Sweet Mango Lassi", "Chilled · 300ml", 119, 1),
    )
}
```

Derived values are computed with `derivedStateOf`. This is important — without it, every scroll or recomposition would recompute the bill, causing unnecessary work:

```kotlin
val billValue by remember {
    derivedStateOf { items.sumOf { it.price * it.qty } }
}

val itemCount by remember {
    derivedStateOf { items.sumOf { it.qty } }
}
```

`derivedStateOf` runs its lambda on every read, but only triggers recomposition when the *result* changes. So `billValue` only causes a recompose when the total actually changes — not on every frame of an animation running elsewhere on screen.

For a production app you'd move this into a ViewModel with a UiState sealed class. The animations themselves wouldn't change. But for a focused animation demo, keeping everything local makes the code easier to follow, and there's no business logic that needs to survive configuration changes.

The `CartItem` model is a simple data class:

```kotlin
data class CartItem(
    val id: String,
    val name: String,
    val sub: String,
    val price: Int,
    val qty: Int,
    val hue: Float = 18f,
)
```

The `hue` field is the only non-obvious choice — it lets each item tile render with a distinct colour tint from a single HSL value, without storing a full colour in the model. Small thing, but it keeps the model clean.

Now, the animations.

---

## Animation 1: The Flying Food Token

This is the centrepiece. When the user taps "+" on a menu item, a circular token animates from that item's position, follows a curved arc, and lands on the cart icon.

### The State

Three pieces of state drive this animation:

```kotlin
val flyerProgress = remember { Animatable(0f) }
var flyerActive by remember { mutableStateOf(false) }
var flyerTargetName by remember { mutableStateOf("") }
var flyerTargetPrice by remember { mutableStateOf(0) }
```

`flyerProgress` is an `Animatable<Float>` — the raw progress value from 0 to 1 that drives the token's position along the Bezier curve. It's `Animatable` rather than `animateFloatAsState` because we need `snapTo()` to reset it before each new animation, which `animateFloatAsState` doesn't support directly.

### Triggering the Animation

When the user taps "+":

```kotlin
fun addItem(name: String, price: Int, hue: Float) {
    if (flyerActive) return           // Guard: one token in flight at a time
    flyerTargetName = name
    flyerTargetPrice = price
    flyerActive = true

    scope.launch {
        flyerProgress.snapTo(0f)      // Reset immediately, no animation
        flyerProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
        // Token has landed. Now commit the item add.
        val idx = items.indexOfFirst { it.id == name.lowercase() }
        if (idx >= 0) {
            items[idx] = items[idx].copy(qty = items[idx].qty + 1)
        } else {
            items.add(CartItem(name.lowercase(), name, "Fresh · Made to order", price, 1, hue))
        }
        cartBounce = true
        flyerActive = false
    }
}
```

There's a deliberate sequencing decision here: the item is **not** added to the list until after the token lands. This matches the user's mental model — the food is "in transit" while the token is flying. Adding it instantly and running the animation in parallel looks wrong because the list updates while the token is still in the air.

`FastOutSlowInEasing` makes the token accelerate out of the card and decelerate as it approaches the cart icon, which reads as weight and momentum.

### Drawing the Token on a Bezier Curve

The token isn't positioned with a layout — it floats above everything else in a `Box` with `zIndex(10f)`:

```kotlin
if (flyerActive) {
    val progress = flyerProgress.value
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
```

First, define start and end points. These are screen-space coordinates hardcoded relative to screen width — a real production implementation would use `onGloballyPositioned` to track the actual button and cart icon positions:

```kotlin
    val fromX = screenWidthPx * 0.45f
    val fromY = with(density) { 240.dp.toPx() }
    val toX   = screenWidthPx * 0.86f
    val toY   = with(density) { 60.dp.toPx() }
```

The control point makes the path arc upward instead of going in a straight line:

```kotlin
    val cpX = (fromX + toX) / 2f
    val cpY = minOf(fromY, toY) - with(density) { 100.dp.toPx() }
```

Now apply the quadratic Bezier formula. `t` is `flyerProgress.value` running from 0 to 1:

```kotlin
    val t = progress

    // P(t) = (1-t)²·P₀ + 2(1-t)t·C + t²·P₁
    val x = (1 - t) * (1 - t) * fromX + 2 * (1 - t) * t * cpX + t * t * toX
    val y = (1 - t) * (1 - t) * fromY + 2 * (1 - t) * t * cpY + t * t * toY
```

This gives us smooth curve-following with zero additional animation state. The curve shape is entirely determined by the three control points — change `cpY` to make the arc higher or lower.

Scale and alpha:

```kotlin
    // Shrink from 1.1x to 0.3x as it "enters" the cart icon
    val tokenScale = 1.1f - progress * 0.8f

    // Fade out only in the last 15% of the journey
    val tokenAlpha = if (progress < 0.85f) 1f
                     else (1f - (progress - 0.85f) / 0.15f)
```

Shrinking the token as it approaches the cart makes it look like it's landing *inside* the icon rather than crashing into it. The late fade (only after 85% of the path) means the token is fully visible through most of the flight — the disappearance feels like arrival, not like it just vanished.

Rendering:

```kotlin
    Box(
        modifier = Modifier
            .zIndex(10f)
            .offset { IntOffset(
                (x - 26.dp.toPx()).roundToInt(),
                (y - 26.dp.toPx()).roundToInt()
            )}
            .size(52.dp)
            .graphicsLayer {
                scaleX = tokenScale
                scaleY = tokenScale
                rotationZ = progress * 360f    // Full 360° spin during flight
                alpha = tokenAlpha
            }
            .clip(CircleShape)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFFFFC764), Color(0xFFE87514))
                )
            )
    )
}
```

`graphicsLayer` is the right place for scale, rotation, and alpha transforms on animated properties. It runs on the render thread and skips recomposition entirely.

The 360° rotation is a small flourish — it makes the token feel like it has energy. Without it, the Bezier arc alone still works but feels a little flat.

### The Cart Icon Bounce

When the token lands, `cartBounce = true` fires. The icon uses `animateFloatAsState` with a spring spec:

```kotlin
var cartBounce by remember { mutableStateOf(false) }

val cartScale by animateFloatAsState(
    targetValue = if (cartBounce) 1.35f else 1f,
    animationSpec = spring(
        dampingRatio = 0.25f,
        stiffness = Spring.StiffnessHigh
    ),
    label = "cart_scale",
    finishedListener = { cartBounce = false }
)
```

`dampingRatio = 0.25f` is an underdamped spring — it overshoots and oscillates before settling. That gives it the natural "something landed in here" feel. `finishedListener` resets the flag so the icon is ready for the next token.

---

## Animation 2: Item Slides Into the List

Once the token lands and the item is added to `items`, the list needs to show that new item. The entry animation should feel like the item is dropping into place — not just appearing.

### AnimatedVisibility with LazyColumn Keys

The list uses `LazyColumn` with `itemsIndexed`. The `key` lambda is critical — without it, Compose can't distinguish which item is new vs. which already existed, and the enter animation fires for everything on every recompose:

```kotlin
LazyColumn {
    itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
```

Inside each item, visibility is driven by a local boolean that starts `false` and flips to `true` after a stagger delay:

```kotlin
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(item.id) {
            delay(index * 80L)    // 80ms stagger between items
            visible = true
        }
```

`LaunchedEffect(item.id)` means this effect runs once when the composable enters composition — i.e., when the item is added. The stagger creates a cascade effect where each item slides in slightly after the previous one.

### The Enter and Exit Specs

```kotlin
        AnimatedVisibility(
            visible = visible && !isRemoving,
            enter = slideInVertically(
                animationSpec = spring(dampingRatio = 0.5f, stiffness = 250f),
                initialOffsetY = { it }     // Slide up from its own height below
            ) + fadeIn(tween(250)),
            exit = slideOutHorizontally(
                animationSpec = tween(250),
                targetOffsetX = { -it }     // Slide out to the left
            ) + shrinkVertically(
                animationSpec = tween(280, delayMillis = 50)
            ) + fadeOut(tween(200)),
        ) {
            CartItemRow(item = item, ...)
        }
```

The **enter** uses a spring instead of a tween. Springs don't have a fixed duration — they settle when the velocity drops below a threshold. `dampingRatio = 0.5f` gives a single small overshoot. It reads as a physical object settling into place.

The **exit** is different. It combines three animations in sequence:
- `slideOutHorizontally` moves the card left (matching the swipe gesture direction)
- `shrinkVertically` collapses the row's height to zero, so the rows below close the gap
- `fadeOut` fades it out

The `delayMillis = 50` on `shrinkVertically` means the row starts collapsing *after* it's already partially swiped out. This avoids the jarring effect of the row getting shorter while it's still partially visible.

---

## Animation 3: Swipe-to-Delete

Swiping a cart item off is the most gesture-driven animation of the three. The implementation uses `Animatable` for real-time drag tracking — not `animateFloatAsState`, because the gesture updates happen synchronously on every drag event.

### The Swipe State

```kotlin
val swipeOffset = remember { Animatable(0f) }
val scope = rememberCoroutineScope()

val showDeleteHint by remember {
    derivedStateOf { swipeOffset.value < -60f }
}
```

`showDeleteHint` uses `derivedStateOf` again — it only triggers recomposition when the threshold is crossed, not on every pixel of drag.

### The Delete Hint Background

Before the swipeable row, a background layer is rendered conditionally:

```kotlin
Box(modifier = Modifier.fillMaxSize()) {

    if (showDeleteHint) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxWidth(0.35f)
                .height(68.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Accent2.copy(alpha = 0.4f),
                            Accent2
                        )
                    )
                ),
            contentAlignment = Alignment.CenterEnd
        ) {
            Text("✕", fontSize = 20.sp, color = Color.White,
                 modifier = Modifier.padding(end = 16.dp))
        }
    }
```

The gradient fades from transparent on the left to solid pink on the right. As the row slides left, this background is revealed progressively. Using `fillMaxWidth(0.35f)` means only the rightmost 35% of the row is "delete territory" — which matches the visual treatment.

### Handling the Drag

```kotlin
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { translationX = swipeOffset.value }
            .pointerInput(item.id) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        scope.launch {
                            if (swipeOffset.value < -180f) {
                                // Committed: fling off screen, then remove
                                swipeOffset.animateTo(-1200f, tween(200))
                                onDismiss()
                            } else {
                                // Not far enough: snap back
                                swipeOffset.animateTo(
                                    0f,
                                    spring(dampingRatio = 0.5f, stiffness = 300f)
                                )
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            swipeOffset.animateTo(0f, spring(0.5f, 300f))
                        }
                    },
                ) { change, dragAmount ->
                    change.consume()
                    scope.launch {
                        swipeOffset.snapTo(
                            (swipeOffset.value + dragAmount).coerceAtMost(0f)
                        )
                    }
                }
            }
    )
```

Walk through the key decisions:

**`snapTo` during drag, `animateTo` on release.** During the active drag, `snapTo` moves the offset immediately — no interpolation. This is what makes the row feel directly attached to the user's finger. `animateTo` only runs on release to either snap back or fling away.

**`.coerceAtMost(0f)`.** This prevents swiping right. The row can only move left.

**Threshold at -180f.** Below 180dp of drag, release snaps back. Beyond that, the delete is committed. This is roughly 45% of a typical screen width — far enough that accidental deletes are unlikely, close enough that intentional ones don't feel laborious.

**Fling to -1200f.** Rather than fading out, the row exits by flying off the left edge. `-1200f` is well beyond any screen width, so the row is guaranteed to be off-screen before `onDismiss` fires. The 200ms tween is fast enough to feel snappy.

### The Dismiss Callback

`onDismiss` sets an `isRemoving` flag rather than removing the item immediately:

```kotlin
onDismiss = {
    removingIds.add(item.id)
    scope.launch {
        delay(320)              // Wait for AnimatedVisibility exit animation
        items.removeAll { it.id == item.id }
        removingIds.remove(item.id)
    }
}
```

The `visible = visible && !isRemoving` check in `AnimatedVisibility` triggers the exit animation (slide out + shrink). The 320ms delay gives the list collapse animation time to complete before actually removing the item from the data list.

Getting this order right matters. If you remove from `items` first, the list immediately jumps — the exit animation never plays because the composable is gone.

---

## Bonus: The Bill Counter Roll

The bill total updates with a number-roll animation:

```kotlin
val animatedBill by animateIntAsState(
    targetValue = billValue,
    animationSpec = tween(durationMillis = 350),
    label = "bill_anim",
    finishedListener = { billPulse = true }
)
```

`animateIntAsState` interpolates between integer values — so if the bill goes from ₹428 to ₹777, every integer between them gets rendered in sequence. At 350ms that's a fast scroll through intermediate values which reads as a satisfying "ka-ching."

When the animation finishes, `billPulse` triggers a separate spring that briefly scales the bill text to 1.12x — a subtle bump that draws the eye to the updated value.

---

## What I'd Change for Production

A few things I simplified for the demo that you'd want to address in a real app:

**Track actual button positions.** The flying token uses hardcoded screen coordinates. In production, use `onGloballyPositioned` with `LocalDensity` to get the real bounds of the "+" button and the cart icon at runtime. This makes the animation correct on all screen sizes and orientations.

**Move state to a ViewModel.** The `items` list and business logic should live in a ViewModel with a `UiState` data class. The animation flags (`flyerActive`, `cartBounce`, etc.) can stay local to the composable — they're purely visual state with no business meaning.

**Handle the one-token-at-a-time guard carefully.** Currently `if (flyerActive) return` silently drops rapid taps. A queue of pending additions would be more correct for a real cart flow.

---

## Wrapping Up

Three animations, one Compose file, zero custom Views.

What I found while building this: Compose's animation APIs are composable in the same way that layouts are. You can stack `spring` for enter, `tween` for exit, and a Bezier-driven `Animatable` for a flying token — and they don't fight each other. The `LazyColumn` key handling and the `AnimatedVisibility` exit sequencing were the parts that needed the most care.

If you build on this or extend it — a parallax hero image, a confetti burst on free delivery, a collapsing toolbar — the same primitives all the way down.

The full project is at [github.com/karishmaagr/SwiggyAnimation](https://github.com/karishmaagr/SwiggyAnimation).

Not a Medium member? Drop your email in the comments and I'll send you this article directly.

---

*If something in here was new to you, or if I got something wrong — I'd genuinely like to know. Comments are open.*
