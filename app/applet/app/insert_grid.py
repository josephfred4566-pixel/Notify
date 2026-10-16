with open("app/src/main/java/com/example/ui/dashboard/ToolSelectionDashboard.kt", "r") as f:
    content = f.read()

# 1. Add import if not present
if "BoxWithConstraints" not in content:
    content = content.replace(
        "import androidx.compose.foundation.layout.Box",
        "import androidx.compose.foundation.layout.Box\nimport androidx.compose.foundation.layout.BoxWithConstraints",
        1
    )

# 2. Insert item into LazyColumn
target_lazy = 'item(key = "section_title")'
grid_item = '''        // Responsive grid layout for selecting between Market Structure Shift and Break of Structure tools
        item(key = "market_structure_grid") {
          MarketStructureToolGrid(
            tools = state.tools,
            searchQuery = state.searchQuery,
            onSelect = { id -> viewModel.toggleToolSelection(id) },
            onInspect = { tool -> viewModel.openToolInspection(tool) },
            onToggleFavorite = { id -> viewModel.toggleToolFavorite(id) }
          )
        }

        item(key = "section_title")'''

if "market_structure_grid" not in content and target_lazy in content:
    content = content.replace(target_lazy, grid_item, 1)

# 3. Append top-level functions at the end of the file
grid_functions = '''

/**
 * Responsive grid layout using glassmorphic theme utility for selecting between
 * Market Structure Shift (MSS) and Break of Structure (BOS) tools.
 */
@Composable
private fun MarketStructureToolGrid(
    tools: List<TradingTool>,
    searchQuery: String,
    onSelect: (String) -> Unit,
    onInspect: (TradingTool) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    val mssTool = tools.find { it.id == "mss" }
    val bosTool = tools.find { it.id == "bos" }
    if (mssTool == null || bosTool == null) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .testTag("market_structure_grid_section")
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                GlassText(
                    text = "MARKET STRUCTURE CORE",
                    style = NotifyTheme.typography.headingLarge.copy(fontSize = 17.sp),
                    color = TextHighContrast
                )
                GlassText(
                    text = "Responsive glass grid for MSS & BOS detection",
                    style = NotifyTheme.typography.caption.copy(fontSize = 11.sp),
                    color = TextLowContrast
                )
            }
            GlassBadge(
                text = "Responsive Grid",
                tint = NeonCyan,
                backgroundColor = NeonCyan.copy(alpha = 0.12f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val isWide = maxWidth >= 480.dp
            if (isWide) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        GridToolCard(
                            tool = mssTool,
                            searchQuery = searchQuery,
                            accentColor = NeonCyan,
                            iconType = GeometricIconType.MSS_SHIFT,
                            onSelect = { onSelect(mssTool.id) },
                            onToggleFavorite = { onToggleFavorite(mssTool.id) },
                            onInspect = { onInspect(mssTool) }
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        GridToolCard(
                            tool = bosTool,
                            searchQuery = searchQuery,
                            accentColor = NeonEmerald,
                            iconType = GeometricIconType.BOS_TREND,
                            onSelect = { onSelect(bosTool.id) },
                            onToggleFavorite = { onToggleFavorite(bosTool.id) },
                            onInspect = { onInspect(bosTool) }
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GridToolCard(
                        tool = mssTool,
                        searchQuery = searchQuery,
                        accentColor = NeonCyan,
                        iconType = GeometricIconType.MSS_SHIFT,
                        onSelect = { onSelect(mssTool.id) },
                        onToggleFavorite = { onToggleFavorite(mssTool.id) },
                        onInspect = { onInspect(mssTool) }
                    )
                    GridToolCard(
                        tool = bosTool,
                        searchQuery = searchQuery,
                        accentColor = NeonEmerald,
                        iconType = GeometricIconType.BOS_TREND,
                        onSelect = { onSelect(bosTool.id) },
                        onToggleFavorite = { onToggleFavorite(bosTool.id) },
                        onInspect = { onInspect(bosTool) }
                    )
                }
            }
        }
    }
}

@Composable
private fun GridToolCard(
    tool: TradingTool,
    searchQuery: String,
    accentColor: Color,
    iconType: GeometricIconType,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInspect: () -> Unit
) {
    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("grid_tool_card_${tool.id}"),
        shape = RoundedCornerShape(20.dp),
        isSelected = tool.isSelected,
        glowColor = accentColor.copy(alpha = 0.15f),
        onClick = onSelect,
        onLongClick = onInspect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.18f))
                            .border(0.8.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        GlassGeometricIcon(
                            iconType = iconType,
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    GlassBadge(
                        text = tool.code,
                        tint = accentColor,
                        backgroundColor = accentColor.copy(alpha = 0.12f)
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(if (tool.isFavorite) NeonAmber.copy(alpha = 0.2f) else GlassWhiteUltraSubtle)
                            .border(0.8.dp, if (tool.isFavorite) NeonAmber.copy(alpha = 0.6f) else GlassBorderSubtle, CircleShape)
                            .clickable { onToggleFavorite() },
                        contentAlignment = Alignment.Center
                    ) {
                        GlassGeometricIcon(
                            iconType = if (tool.isFavorite) GeometricIconType.STAR_FILLED else GeometricIconType.STAR_OUTLINE,
                            tint = if (tool.isFavorite) NeonAmber else TextLowContrast,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (tool.isSelected) accentColor else GlassWhiteUltraSubtle)
                            .border(1.dp, if (tool.isSelected) accentColor else GlassBorderSubtle, RoundedCornerShape(6.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (tool.isSelected) {
                            GlassGeometricIcon(
                                iconType = GeometricIconType.CHECK,
                                tint = Color(0xFF070B12),
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            val highlightedName = buildHighlightedAnnotatedString(
                fullText = tool.name,
                query = searchQuery,
                defaultColor = TextHighContrast,
                highlightColor = accentColor
            )
            GlassText(
                text = highlightedName,
                style = NotifyTheme.typography.headingMedium.copy(fontSize = 15.sp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            GlassText(
                text = tool.summary,
                style = NotifyTheme.typography.bodySecondary.copy(fontSize = 12.sp, lineHeight = 16.sp),
                color = TextMediumContrast,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GlassBadge(
                    text = "Win: ${(tool.winRateEstimate * 100).toInt()}%",
                    tint = NeonEmerald,
                    backgroundColor = NeonEmerald.copy(alpha = 0.1f)
                )
                GlassText(
                    text = "Tap to toggle",
                    style = NotifyTheme.typography.caption.copy(fontSize = 10.sp),
                    color = TextLowContrast
                )
            }
        }
    }
}
'''

if "MarketStructureToolGrid" not in content:
    content += grid_functions

with open("app/src/main/java/com/example/ui/dashboard/ToolSelectionDashboard.kt", "w") as f:
    f.write(content)

print("ToolSelectionDashboard.kt updated successfully.")
