package com.karishma.swiggyanimation

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Data
// ---------------------------------------------------------------------------

data class MenuItem(
    val name: String,
    val description: String,
    val price: Int,
    val category: String,
)

private val menuItems = listOf(
    MenuItem("Handi Biryani",          "Slow-cooked dum biryani with saffron",          349, "Recommended"),
    MenuItem("Chicken Tikka",          "Tender chicken marinated in spices",             299, "Recommended"),
    MenuItem("Seekh Kebab",            "Minced lamb on skewers, charcoal grilled",       279, "Recommended"),
    MenuItem("Behrouz Special Biryani","Chef's signature dum biryani",                   449, "Biryani"),
    MenuItem("Mutton Biryani",         "Slow-cooked mutton with aromatic spices",        399, "Biryani"),
    MenuItem("Veg Biryani",            "Garden-fresh vegetables in dum biryani",         249, "Biryani"),
    MenuItem("Shami Kebab",
        "Minced lamb patties with mint chutney",          259, "Kebabs"),
    MenuItem("Galouti Kebab",          "Melt-in-mouth lamb kebab, Lucknow style",        319, "Kebabs"),
    MenuItem("Phirni",                 "Traditional rose-flavoured rice pudding",        149, "Desserts"),
    MenuItem("Gulab Jamun",            "Soft khoya balls in sugar syrup",               129, "Desserts"),
    MenuItem("Mango Lassi",            "Chilled yoghurt drink with Alphonso mango",       99, "Drinks"),
    MenuItem("Rose Sharbat",           "Chilled rose-flavoured drink",                   79, "Drinks"),
)

private val categories = listOf("Recommended", "Biryani", "Kebabs", "Desserts", "Drinks")

// ---------------------------------------------------------------------------
// Root screen
// ---------------------------------------------------------------------------

@Composable
fun RestaurantScreen(onViewCart: () -> Unit = {}) {
    val listState = rememberLazyListState()

    // Convert hero height once — stable across recompositions
    val heroHeightPx = with(LocalDensity.current) { 250.dp.toPx() }

    // ─────────────────────────────────────────────────
    // ⚠️ THE WRONG WAY — do not use this
    // ─────────────────────────────────────────────────
    // val showCart = listState.firstVisibleItemIndex > 3
    //
    // Why this is bad:
    // This line lives in the composition scope. LazyListState
    // updates firstVisibleItemIndex on EVERY scroll frame.
    // That means this condition — and every composable that
    // reads showCart — recomposes on every pixel scrolled.
    // On a mid-range device this causes visible jank.
    // ─────────────────────────────────────────────────
    // ✅ THE RIGHT WAY — what we actually use
    // ─────────────────────────────────────────────────
    val showCart by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 3 }
    }
    // derivedStateOf runs its lambda on every scroll frame,
    // but only triggers recomposition when the RESULT changes
    // (false → true or true → false). CartBottomBar recomposes
    // exactly twice per scroll session. Not 600 times.

    // mutableFloatStateOf is the primitive-optimised version of mutableStateOf(0f)
    var scrollOffset by remember { mutableFloatStateOf(0f) }

    // nestedScrollConnection intercepts every scroll event that bubbles up from
    // the LazyColumn before it reaches the parent. We read available.y but return
    // Offset.Zero so the column still handles its own scrolling unchanged.
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // available.y is negative when the user scrolls down (content moves up).
                // Subtracting it accumulates a positive offset as we scroll down.
                scrollOffset = (scrollOffset - available.y).coerceIn(0f, heroHeightPx)
                return Offset.Zero // don't consume — LazyColumn scrolls normally
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item { ParallaxHeroImage(scrollOffset, heroHeightPx) }
            item { RestaurantInfoSection() }
            stickyHeader { MenuTabRow() }
            items(menuItems) { item -> MenuItemCard(item) }
            // Extra space so the last card isn't hidden behind the cart bar
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        CollapsingToolbar(
            scrollOffset = scrollOffset,
            heroHeightPx = heroHeightPx,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        CartBottomBar(visible = showCart, onViewCart = onViewCart, modifier = Modifier.align(Alignment.BottomCenter))
    }
}

// ---------------------------------------------------------------------------
// Hero image — parallax + fade driven by scroll
// ---------------------------------------------------------------------------

@Composable
private fun ParallaxHeroImage(scrollOffset: Float, heroHeightPx: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
            // ─────────────────────────────────────────────────
            // ⚠️ THE WRONG WAY
            // ─────────────────────────────────────────────────
            // Modifier.offset(y = (-scrollOffset * 0.5f).dp)
            //
            // offset() is applied during the Layout phase.
            // Compose must re-layout AND re-draw on every frame.
            // ─────────────────────────────────────────────────
            // ✅ THE RIGHT WAY
            // ─────────────────────────────────────────────────
            // Modifier.graphicsLayer { translationY = -scrollOffset * 0.5f }
            //
            // graphicsLayer runs entirely on the RenderThread.
            // Zero recomposition. Zero re-layout. Just GPU transforms.
            .graphicsLayer {
                // The LazyColumn already scrolls this item upward at 1× speed.
                // A positive translationY partially cancels that upward motion,
                // so the net visual speed is 0.5× — the definition of parallax.
                translationY = scrollOffset * 0.5f

                // Fade to 0 exactly when the image has scrolled fully out of view
                alpha = (1f - scrollOffset / heroHeightPx).coerceIn(0f, 1f)
            }
            .background(Color(0xFFBB8A52)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "🍛  Restaurant Hero Image",
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
        )
    }
}

// ---------------------------------------------------------------------------
// Restaurant metadata row
// ---------------------------------------------------------------------------

@Composable
private fun RestaurantInfoSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text(
            text = "Behrouz Biryani",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Mughlai • Biryani  ·  4.5 ⭐  ·  30–40 min  ·  ₹200 for two",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------------------------------------------------------------------------
// Collapsing toolbar — fades in as the hero image scrolls away
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollapsingToolbar(
    scrollOffset: Float,
    heroHeightPx: Float,
    modifier: Modifier = Modifier,
) {
    val toolbarAlpha by animateFloatAsState(
        targetValue = (scrollOffset / heroHeightPx).coerceIn(0f, 1f),
        label = "toolbar_alpha",
    )
    TopAppBar(
        title = { Text("Behrouz Biryani") },
        // graphicsLayer is used instead of Modifier.alpha() because graphicsLayer
        // applies the alpha on the RenderThread after layout and composition are
        // already done, so animating it never triggers recomposition or re-layout
        // of the toolbar or anything else in the tree. Modifier.alpha() is a
        // draw-phase modifier that still participates in the normal composition
        // cycle — fine for static values, but wasteful when the value changes on
        // every scroll frame.
        modifier = modifier.graphicsLayer { alpha = toolbarAlpha },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

// ---------------------------------------------------------------------------
// Sticky category tab row stub (animated indicator wired up in next step)
// ---------------------------------------------------------------------------

@Composable
private fun MenuTabRow() {
    var selectedTab by remember { mutableIntStateOf(0) }
    ScrollableTabRow(
        selectedTabIndex = selectedTab,
        modifier = Modifier.fillMaxWidth(),
        indicator = { tabPositions ->
            val indicatorOffset by animateDpAsState(
                targetValue = tabPositions[selectedTab].left,
                label = "indicator_offset",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.BottomStart)
                    .offset(x = indicatorOffset)
                    .width(tabPositions[selectedTab].width)
                    .height(3.dp)
                    .background(MaterialTheme.colorScheme.primary),
            )
        },
    ) {
        categories.forEachIndexed { index, label ->
            Tab(
                selected = selectedTab == index,
                onClick = { selectedTab = index },
                text = { Text(label) },
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Cart bottom bar stub (AnimatedVisibility wired up in next step)
// ---------------------------------------------------------------------------

@Composable
private fun CartBottomBar(visible: Boolean, onViewCart: () -> Unit = {}, modifier: Modifier = Modifier) {
    val recomposeCount = remember { mutableIntStateOf(0) }
    SideEffect {
        recomposeCount.intValue++
        Log.d("SwiggyPerf", "CartBottomBar recomposed ${recomposeCount.intValue} times")
    }
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        modifier = modifier,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color(0xFF1B5E20),
            tonalElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("2 items | ₹480", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text("View Cart →", color = Color.White, fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(onClick = onViewCart))
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Menu item card
// ---------------------------------------------------------------------------

@Composable
private fun MenuItemCard(item: MenuItem) {
    val recomposeCount = remember { mutableIntStateOf(0) }
    SideEffect {
        recomposeCount.intValue++
        Log.d("SwiggyPerf", "MenuItemCard #${recomposeCount.intValue} recomposed")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(item.name, fontWeight = FontWeight.SemiBold)
            Text(
                item.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text("₹${item.price}", fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(12.dp))
        // Placeholder image box — replaced with AsyncImage / Coil in next step
        Box(
            modifier = Modifier
                .size(90.dp, 80.dp)
                .background(Color(0xFFEEEEEE)),
            contentAlignment = Alignment.Center,
        ) {
            Text("🍽", fontSize = 28.sp)
        }
    }
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
