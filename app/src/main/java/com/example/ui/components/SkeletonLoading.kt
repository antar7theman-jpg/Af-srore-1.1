package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier for Skeleton loading animations.
 * Provides a luminous smooth gradient sweeping horizontally.
 */
@Composable
fun Modifier.shimmerEffect(
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    highlightColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
    durationMillis: Int = 1100
): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer_transition")
    val translateAnim by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    val brush = Brush.linearGradient(
        colors = listOf(
            baseColor,
            highlightColor,
            baseColor
        ),
        start = Offset(translateAnim - 300f, translateAnim - 300f),
        end = Offset(translateAnim + 300f, translateAnim + 300f)
    )

    return this
        .clip(shape)
        .background(brush)
}

/**
 * Standard Atomic Shimmer Box Placeholder.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(8.dp),
    baseColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
    highlightColor: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
) {
    Box(
        modifier = modifier.shimmerEffect(shape = shape, baseColor = baseColor, highlightColor = highlightColor)
    )
}

// ==========================================
// 1. INVENTORY SKELETON LOADERS
// ==========================================

/**
 * Skeleton card mirroring the layout and geometry of an Inventory Product Card.
 */
@Composable
fun InventoryProductCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Section: Image placeholder + Title/Barcode placeholders + Stock status badge placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Thumbnail Box
                ShimmerBox(
                    modifier = Modifier.size(56.dp),
                    shape = RoundedCornerShape(14.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(18.dp),
                        shape = RoundedCornerShape(6.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth(0.45f)
                            .height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                }

                // Stock Badge Placeholder
                ShimmerBox(
                    modifier = Modifier
                        .width(72.dp)
                        .height(26.dp),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // Middle Section: Pricing & Profit info container (3 metrics)
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ShimmerBox(
                            modifier = Modifier
                                .width(70.dp)
                                .height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(70.dp)
                                .height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                        ShimmerBox(
                            modifier = Modifier
                                .width(80.dp)
                                .height(28.dp),
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
            }

            // Bottom Action Buttons Strip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(4) {
                    ShimmerBox(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }
        }
    }
}

/**
 * Vertical list of inventory item skeletons.
 */
@Composable
fun InventoryListSkeleton(
    count: Int = 5,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 88.dp)
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(count) {
            InventoryProductCardSkeleton()
        }
    }
}

// ==========================================
// 2. POS SCREEN SKELETON LOADERS
// ==========================================

/**
 * Skeleton card mirroring the POS Grid product item.
 */
@Composable
fun PosProductCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Stock badge placeholder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(55.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(8.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(42.dp)
                        .height(20.dp),
                    shape = RoundedCornerShape(6.dp)
                )
            }

            // Product Image Placeholder
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp),
                shape = RoundedCornerShape(10.dp)
            )

            // Product Title Placeholder
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(15.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(13.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Price and Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(4.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    ShimmerBox(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape
                    )
                    ShimmerBox(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape
                    )
                }
            }
        }
    }
}

/**
 * Adaptive grid of POS product card skeletons.
 */
@Composable
fun PosProductsGridSkeleton(
    count: Int = 6,
    modifier: Modifier = Modifier,
    minSize: Dp = 160.dp,
    contentPadding: PaddingValues = PaddingValues(bottom = 88.dp)
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = minSize),
        modifier = modifier.fillMaxSize(),
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(count) {
            PosProductCardSkeleton()
        }
    }
}

// ==========================================
// 3. REPORTS SKELETON LOADERS
// ==========================================

/**
 * Skeleton for Report Stat Summary Card.
 */
@Composable
fun ReportsStatCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                ShimmerBox(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape
                )
            }

            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(22.dp),
                shape = RoundedCornerShape(6.dp)
            )
        }
    }
}

/**
 * Skeleton for Report Valuation Card (Cost / Sale / Profit).
 */
@Composable
fun ReportsValuationCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier.size(24.dp),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(8.dp))
                ShimmerBox(
                    modifier = Modifier
                        .width(140.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) {
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(11.dp),
                                shape = RoundedCornerShape(3.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .width(70.dp)
                                    .height(16.dp),
                                shape = RoundedCornerShape(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Skeleton for the Interactive Chart Card.
 */
@Composable
fun ReportsChartCardSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(130.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(70.dp)
                        .height(30.dp),
                    shape = RoundedCornerShape(8.dp)
                )
            }

            // Simulated Chart Bars Area
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(12.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val barHeights = listOf(0.4f, 0.65f, 0.85f, 0.5f, 0.95f, 0.7f, 0.6f)
                    barHeights.forEach { fraction ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            ShimmerBox(
                                modifier = Modifier
                                    .width(26.dp)
                                    .fillMaxHeight(fraction),
                                shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                            )
                            ShimmerBox(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(10.dp),
                                shape = RoundedCornerShape(3.dp)
                            )
                        }
                    }
                }
            }

            // Highlight bar underneath
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            )
        }
    }
}

/**
 * Skeleton for Recent Sales Item.
 */
@Composable
fun ReportsRecentSaleItemSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ShimmerBox(
                        modifier = Modifier
                            .width(110.dp)
                            .height(14.dp),
                        shape = RoundedCornerShape(4.dp)
                    )
                    ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(11.dp),
                        shape = RoundedCornerShape(3.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(70.dp)
                        .height(15.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(50.dp)
                        .height(11.dp),
                    shape = RoundedCornerShape(3.dp)
                )
            }
        }
    }
}

/**
 * Complete Skeleton UI for the Reports Screen.
 */
@Composable
fun ReportsScreenSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Period Segmented Buttons Shimmer
        item {
            Spacer(modifier = Modifier.height(4.dp))
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(22.dp)
            )
        }

        // 2 Stat Summary Cards Shimmer
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ReportsStatCardSkeleton(modifier = Modifier.weight(1f))
                ReportsStatCardSkeleton(modifier = Modifier.weight(1f))
            }
        }

        // Inventory Valuation Card Shimmer
        item {
            ReportsValuationCardSkeleton()
        }

        // Chart Card Shimmer
        item {
            ReportsChartCardSkeleton()
        }

        // Recent Invoices Header Shimmer
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(
                    modifier = Modifier
                        .width(120.dp)
                        .height(18.dp),
                    shape = RoundedCornerShape(4.dp)
                )
                ShimmerBox(
                    modifier = Modifier
                        .width(60.dp)
                        .height(14.dp),
                    shape = RoundedCornerShape(4.dp)
                )
            }
        }

        // Recent Sales Items Shimmer
        items(3) {
            ReportsRecentSaleItemSkeleton()
        }
    }
}
