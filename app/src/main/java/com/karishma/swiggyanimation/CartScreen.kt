package com.karishma.swiggyanimation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

// ─── Design tokens ────────────────────────────────────────────
private val Bg0 = Color(0xFF0A0B0F)
private val Bg1 = Color(0xFF12141A)
private val Bg2 = Color(0xFF1A1D26)
private val LineColor = Color(0xFF2B303C)
private val Ink = Color(0xFFEEF0F6)
private val Mute = Color(0xFF8A92A3)
private val Accent = Color(0xFFFF6B2B)
private val Accent2 = Color(0xFFFF2D87)
private val GreenColor = Color(0xFF4AE3A1)
private val Amber = Color(0xFFFFC043)
private val Cyan = Color(0xFF5EC6FF)
private val Pink = Color(0xFFF363A5)

// ─── Data ─────────────────────────────────────────────────────
data class CartItem(
    val id: String,
    val name: String,
    val sub: String,
    val price: Int,
    val qty: Int,
    val hue: Float = 18f,
)

private val confettiColors = listOf(Accent, Accent2, GreenColor, Cyan, Amber)

// ─── Screen ───────────────────────────────────────────────────
@Composable
fun CartScreen(onBack: () -> Unit = {}) {
    val items = remember {
        mutableStateListOf(
            CartItem("biryani", "Hyderabadi Biryani", "Boneless · Medium spicy", 349, 1, 18f),
            CartItem("naan", "Garlic Butter Naan", "Tandoor · Pack of 2", 79, 2, 45f),
            CartItem("lassi", "Sweet Mango Lassi", "Chilled · 300ml", 119, 1, 55f),
        )
    }
    val removingIds = remember { mutableStateListOf<String>() }

    val billValue by remember { derivedStateOf { items.sumOf { it.price * it.qty } } }
    val freeDelivery by remember { derivedStateOf { billValue >= 500 } }
    val itemCount by remember { derivedStateOf { items.sumOf { it.qty } } }

    // Cart icon bounce
    var cartBounce by remember { mutableStateOf(false) }
    val cartScale by animateFloatAsState(
        targetValue = if (cartBounce) 1.35f else 1f,
        animationSpec = spring(dampingRatio = 0.25f, stiffness = Spring.StiffnessHigh),
        label = "cart_scale",
        finishedListener = { cartBounce = false },
    )
    val badgeScale by animateFloatAsState(
        targetValue = if (itemCount > 0) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.25f, stiffness = Spring.StiffnessMediumLow),
        label = "badge_scale",
    )

    // Bill total
    var billPulse by remember { mutableStateOf(false) }
    val billScale by animateFloatAsState(
        targetValue = if (billPulse) 1.12f else 1f,
        animationSpec = spring(dampingRatio = 0.25f, stiffness = Spring.StiffnessHigh),
        label = "bill_scale",
        finishedListener = { billPulse = false },
    )
    val animatedBill by animateIntAsState(
        targetValue = billValue,
        animationSpec = tween(durationMillis = 350),
        label = "bill_anim",
        finishedListener = { billPulse = true },
    )

    // FREE delivery pop-in
    val freeScale by animateFloatAsState(
        targetValue = if (freeDelivery) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.25f, stiffness = Spring.StiffnessMediumLow),
        label = "free_scale",
    )
    val freeAlpha by animateFloatAsState(
        targetValue = if (freeDelivery) 1f else 0f,
        animationSpec = tween(300),
        label = "free_alpha",
    )

    // Confetti
    var showConfetti by remember { mutableStateOf(false) }
    LaunchedEffect(freeDelivery) {
        if (freeDelivery) {
            showConfetti = true
            delay(2000)
            showConfetti = false
        }
    }

    // Flying food token
    val flyerProgress = remember { Animatable(0f) }
    var flyerActive by remember { mutableStateOf(false) }
    var flyerTargetName by remember { mutableStateOf("") }
    var flyerTargetPrice by remember { mutableStateOf(0) }

    val scope = rememberCoroutineScope()
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val density = LocalDensity.current

    fun addItem(name: String, price: Int, hue: Float) {
        if (flyerActive) return
        flyerTargetName = name
        flyerTargetPrice = price
        flyerActive = true
        scope.launch {
            flyerProgress.snapTo(0f)
            flyerProgress.animateTo(1f, tween(700, easing = FastOutSlowInEasing))
            // Commit add after token lands
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Bg0),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── Top bar ──────────────────────────────────────
            CartTopBar(
                onBack = onBack,
                cartScale = cartScale,
                itemCount = itemCount,
                badgeScale = badgeScale,
            )

            // ── Scrollable body ───────────────────────────────
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 16.dp),
            ) {
                item {
                    MenuTilesRow(onAdd = { name, price, hue -> addItem(name, price, hue) })
                }

                item {
                    Text(
                        "Your Items",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = Mute,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                    )
                }

                itemsIndexed(items, key = { _, item -> item.id }) { index, item ->
                    val isRemoving = item.id in removingIds
                    var visible by remember { mutableStateOf(false) }
                    LaunchedEffect(item.id) {
                        delay(index * 80L)
                        visible = true
                    }
                    AnimatedVisibility(
                        visible = visible && !isRemoving,
                        enter = slideInVertically(
                            animationSpec = spring(dampingRatio = 0.5f, stiffness = 250f),
                            initialOffsetY = { it },
                        ) + fadeIn(tween(250)),
                        exit = slideOutHorizontally(
                            animationSpec = tween(250),
                            targetOffsetX = { -it },
                        ) + shrinkVertically(tween(280, delayMillis = 50)) + fadeOut(tween(200)),
                    ) {
                        CartItemRow(
                            item = item,
                            onQtyChange = { newQty ->
                                val idx = items.indexOf(item)
                                if (idx < 0) return@CartItemRow
                                if (newQty <= 0) {
                                    removingIds.add(item.id)
                                    scope.launch {
                                        delay(350)
                                        items.remove(item)
                                        removingIds.remove(item.id)
                                    }
                                } else {
                                    items[idx] = item.copy(qty = newQty)
                                }
                                billPulse = true
                            },
                            onDismiss = {
                                if (item.id !in removingIds) {
                                    removingIds.add(item.id)
                                    scope.launch {
                                        delay(350)
                                        items.remove(item)
                                        removingIds.remove(item.id)
                                        billPulse = true
                                    }
                                }
                            },
                        )
                    }
                }
            }

            // ── Bill block ────────────────────────────────────
            BillBlock(
                bill = animatedBill,
                billScale = billScale,
                freeScale = freeScale,
                freeAlpha = freeAlpha,
                itemCount = itemCount,
            )
        }

        // ── Flying food token overlay ──────────────────────────
        if (flyerActive) {
            val progress = flyerProgress.value
            val swDp = with(density) { screenWidthDp.toPx() }
            val fromX = swDp * 0.45f
            val fromY = with(density) { 240.dp.toPx() }
            val toX = swDp * 0.86f
            val toY = with(density) { 60.dp.toPx() }
            val cpX = (fromX + toX) / 2f
            val cpY = minOf(fromY, toY) - with(density) { 100.dp.toPx() }
            val t = progress
            val x = (1 - t) * (1 - t) * fromX + 2 * (1 - t) * t * cpX + t * t * toX
            val y = (1 - t) * (1 - t) * fromY + 2 * (1 - t) * t * cpY + t * t * toY
            val tokenScale = 1.1f - progress * 0.8f
            val tokenAlpha = if (progress < 0.85f) 1f else (1f - (progress - 0.85f) / 0.15f)

            Box(
                modifier = Modifier
                    .zIndex(10f)
                    .offset { IntOffset((x - with(density) { 26.dp.toPx() }).roundToInt(), (y - with(density) { 26.dp.toPx() }).roundToInt()) }
                    .size(52.dp)
                    .graphicsLayer {
                        scaleX = tokenScale
                        scaleY = tokenScale
                        rotationZ = progress * 360f
                        alpha = tokenAlpha
                    }
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color(0xFFFFC764), Color(0xFFE87514)),
                        ),
                    ),
            )
        }

        // ── Confetti overlay ───────────────────────────────────
        if (showConfetti) {
            ConfettiBurst(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = (-160).dp)
                    .zIndex(9f),
            )
        }
    }
}

// ─── Top bar ──────────────────────────────────────────────────
@Composable
private fun CartTopBar(
    onBack: () -> Unit,
    cartScale: Float,
    itemCount: Int,
    badgeScale: Float,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg1)
            .drawBehind {
                drawLine(LineColor, Offset(0f, size.height), Offset(size.width, size.height), 1.dp.toPx())
            }
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Back button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Bg2)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Text("‹", fontSize = 22.sp, color = Ink, fontWeight = FontWeight.Light)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "YOUR CART",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Mute,
                )
                Text(
                    "Aangan Kitchen",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    letterSpacing = (-0.5).sp,
                )
            }

            // Animated cart icon + badge
            Box(
                modifier = Modifier.graphicsLayer { scaleX = cartScale; scaleY = cartScale },
                contentAlignment = Alignment.TopEnd,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Accent, Accent2),
                                start = Offset(0f, 0f),
                                end = Offset(100f, 100f),
                            ),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("🛒", fontSize = 22.sp)
                }
                // Badge
                Box(
                    modifier = Modifier
                        .offset(x = 4.dp, y = (-4).dp)
                        .size(22.dp)
                        .graphicsLayer { scaleX = badgeScale; scaleY = badgeScale }
                        .clip(CircleShape)
                        .background(GreenColor),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        itemCount.toString(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF003320),
                    )
                }
            }
        }
    }
}

// ─── Menu tiles ───────────────────────────────────────────────
@Composable
private fun MenuTilesRow(onAdd: (name: String, price: Int, hue: Float) -> Unit) {
    val tiles = listOf(
        Triple("Biryani", 349, 18f),
        Triple("Kebab", 289, 30f),
        Triple("Paratha", 149, 45f),
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        tiles.forEach { (name, price, hue) ->
            MenuTile(
                name = name,
                price = "₹$price",
                hue = hue,
                modifier = Modifier.weight(1f),
                onClick = { onAdd(name, price, hue) },
            )
        }
    }
}

@Composable
private fun MenuTile(
    name: String,
    price: String,
    hue: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMediumLow),
        label = "tile_scale",
        finishedListener = { pressed = false },
    )
    val tileColor = Color.hsl(hue, 0.7f, 0.45f)
    val tileColor2 = Color.hsl(hue, 0.6f, 0.3f)

    Box(
        modifier = modifier
            .height(76.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(tileColor, tileColor2), Offset(0f, 0f), Offset(0f, 300f)))
            .clickable {
                pressed = true
                onClick()
            }
            .padding(10.dp),
    ) {
        // "+" button
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("+", fontSize = 18.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Text(name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(price, fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
        }
    }
}

// ─── Cart item row ────────────────────────────────────────────
@Composable
private fun CartItemRow(
    item: CartItem,
    onQtyChange: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val swipeOffset = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val showDeleteHint by remember { derivedStateOf { swipeOffset.value < -60f } }

    // Qty stepper bounce
    var qtyBounce by remember { mutableStateOf(false) }
    val qtyScale by animateFloatAsState(
        targetValue = if (qtyBounce) 1.22f else 1f,
        animationSpec = spring(dampingRatio = 0.25f, stiffness = Spring.StiffnessHigh),
        label = "qty_scale",
        finishedListener = { qtyBounce = false },
    )

    val tileColor = Color.hsl(item.hue, 0.65f, 0.50f)
    val tileColor2 = Color.hsl(item.hue, 0.55f, 0.35f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
    ) {
        // Delete hint bg (revealed as row swipes left)
        if (showDeleteHint) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(0.35f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Accent2.copy(alpha = 0.4f), Accent2),
                        ),
                    ),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text("✕", fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(end = 16.dp))
            }
        }

        // Swipeable row card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { translationX = swipeOffset.value }
                .pointerInput(item.id) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            scope.launch {
                                if (swipeOffset.value < -180f) {
                                    swipeOffset.animateTo(-1200f, tween(200))
                                    onDismiss()
                                } else {
                                    swipeOffset.animateTo(0f, spring(dampingRatio = 0.5f, stiffness = 300f))
                                }
                            }
                        },
                        onDragCancel = {
                            scope.launch { swipeOffset.animateTo(0f, spring(0.5f, 300f)) }
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            swipeOffset.snapTo((swipeOffset.value + dragAmount).coerceAtMost(0f))
                        }
                    }
                }
                .clip(RoundedCornerShape(14.dp))
                .background(Brush.linearGradient(listOf(Bg2, Bg1), Offset(0f, 0f), Offset(0f, 300f)))
                .drawBehind {
                    drawRoundRect(
                        color = LineColor,
                        size = size,
                        cornerRadius = CornerRadius(14.dp.toPx()),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                    )
                }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Food colour swatch
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Brush.linearGradient(listOf(tileColor, tileColor2), Offset(0f, 0f), Offset(0f, 200f))),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(item.sub, fontSize = 11.sp, color = Mute)
                Text(
                    "₹${item.price * item.qty}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Amber,
                )
            }

            // Qty stepper with spring scale bounce
            Row(
                modifier = Modifier
                    .graphicsLayer { scaleX = qtyScale; scaleY = qtyScale }
                    .clip(RoundedCornerShape(999.dp))
                    .background(Bg0)
                    .drawBehind {
                        drawRoundRect(LineColor, cornerRadius = CornerRadius(999.dp.toPx()), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()))
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable {
                            qtyBounce = true
                            onQtyChange(item.qty - 1)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("−", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Ink)
                }
                Text(
                    item.qty.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    modifier = Modifier.width(22.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .clickable {
                            qtyBounce = true
                            onQtyChange(item.qty + 1)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text("+", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Accent)
                }
            }
        }
    }
}

// ─── Bill block ───────────────────────────────────────────────
@Composable
private fun BillBlock(
    bill: Int,
    billScale: Float,
    freeScale: Float,
    freeAlpha: Float,
    itemCount: Int,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg1)
            .drawBehind {
                drawLine(LineColor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx())
            }
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Total row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Total", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Mute)
            Text(
                "₹${bill.toLong().let { b -> if (b >= 1000) "${b / 1000},${"%03d".format(b % 1000)}" else b.toString() }}",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Ink,
                modifier = Modifier.graphicsLayer { scaleX = billScale; scaleY = billScale; transformOrigin = androidx.compose.ui.graphics.TransformOrigin(1f, 0.5f) },
            )
        }

        // Delivery row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Delivery", fontSize = 11.sp, color = Color(0xFF5C6473))
            // FREE badge — springs in when threshold reached
            Box(
                modifier = Modifier
                    .graphicsLayer { scaleX = freeScale; scaleY = freeScale; alpha = freeAlpha }
                    .clip(RoundedCornerShape(999.dp))
                    .background(GreenColor.copy(alpha = 0.15f))
                    .drawBehind {
                        drawRoundRect(
                            GreenColor.copy(alpha = 0.35f),
                            cornerRadius = CornerRadius(999.dp.toPx()),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                        )
                    }
                    .padding(horizontal = 10.dp, vertical = 3.dp),
            ) {
                Text("✨ FREE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GreenColor)
            }
        }

        Spacer(Modifier.height(12.dp))

        // CTA
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Accent, Accent2),
                        start = Offset(0f, 0f),
                        end = Offset(Float.POSITIVE_INFINITY, 0f),
                    ),
                )
                .clickable { /* proceed */ },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "Proceed to checkout  →",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

// ─── Confetti burst ───────────────────────────────────────────
@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(1600, easing = FastOutSlowInEasing))
    }

    val p = progress.value
    androidx.compose.foundation.Canvas(
        modifier = modifier.size(200.dp),
    ) {
        val radius = 90.dp.toPx() * p
        val particleW = 6.dp.toPx()
        val particleH = 10.dp.toPx()
        repeat(14) { i ->
            val angle = (i.toFloat() / 14f) * (PI * 2).toFloat()
            val cx = center.x + cos(angle) * radius
            val cy = center.y + sin(angle) * radius
            val alpha = (1f - p * 0.85f).coerceIn(0f, 1f)
            rotate(degrees = p * 720f, pivot = Offset(cx, cy)) {
                drawRoundRect(
                    color = confettiColors[i % confettiColors.size].copy(alpha = alpha),
                    topLeft = Offset(cx - particleW / 2f, cy - particleH / 2f),
                    size = Size(particleW, particleH),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
        }
    }
}
