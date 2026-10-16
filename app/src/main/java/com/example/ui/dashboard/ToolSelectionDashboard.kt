package com.example.ui.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.trading.codegen.AppScreen
import com.example.ui.codestudio.CodeStudioScreen
import com.example.ui.glass.buildHighlightedAnnotatedString
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trading.model.StructureType
import com.example.trading.model.TradingTool
import com.example.ui.chart.GlassTradingChart
import com.example.ui.glass.GeometricIconType
import com.example.ui.glass.GlassBadge
import com.example.ui.glass.GlassButton
import com.example.ui.glass.GlassGeometricIcon
import com.example.ui.glass.GlassPanel
import com.example.ui.glass.GlassSwitch
import com.example.ui.glass.GlassText
import com.example.ui.theme.BgDarkDeep
import com.example.ui.theme.GlassBorderHighlight
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassWhiteMedium
import com.example.ui.theme.GlassWhiteSubtle
import com.example.ui.theme.GlassWhiteUltraSubtle
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.NotifyTheme
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextHighContrast
import com.example.ui.theme.TextLowContrast
import com.example.ui.theme.TextMediumContrast

@Composable
fun ToolSelectionDashboard(
  viewModel: DashboardViewModel,
  modifier: Modifier = Modifier
) {
  val state by viewModel.uiState.collectAsState()
  val insets = WindowInsets.statusBars.asPaddingValues()
  val context = LocalContext.current

  // Switch to Code Studio page if selected
  if (state.currentScreen == AppScreen.CODE_STUDIO) {
    CodeStudioScreen(viewModel = viewModel, modifier = modifier)
    return
  }

  // Handle Android back button when inspecting a tool or viewing export dialog
  BackHandler(enabled = state.exportDialogState != null) {
    viewModel.dismissExportDialog()
  }

  BackHandler(enabled = state.inspectingTool != null) {
    viewModel.openToolInspection(null)
  }

  // Animate gentle background blur when Inspect Engine modal is open so background elements don't interfere
  val isInspecting = state.inspectingTool != null || state.exportDialogState != null
  val backgroundBlur by animateDpAsState(
    targetValue = if (isInspecting) 12.dp else 0.dp,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioNoBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "bgBlurAnim"
  )

  val favoriteTools = state.tools.filter { it.isFavorite }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BgDarkDeep)
  ) {
    // 1. Static ambient background circles that do NOT move with the scrolling cards
    AmbientGlassAtmosphere()

    // 2. Main content area (cards scroll over the static background, and blurs gently during inspection)
    Column(
      modifier = Modifier
        .fillMaxSize()
        .blur(backgroundBlur)
        .padding(top = insets.calculateTopPadding())
    ) {
      // Clean header with Search Indicator Bar placed directly inside and top-right Code & Export buttons
      DashboardHeader(
        searchQuery = state.searchQuery,
        onSearchChange = { viewModel.updateSearchQuery(it) },
        onNavigateToCode = { viewModel.navigateTo(AppScreen.CODE_STUDIO) }
      )

      // Scrollable list of cards
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f),
        contentPadding = PaddingValues(top = 10.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
      ) {
        // Only list tools above if the user has made them their favorite
        if (favoriteTools.isNotEmpty()) {
          item(key = "favorites_section") {
            FavoriteToolsBar(
              favorites = favoriteTools,
              onToolTap = { tool -> viewModel.openToolInspection(tool) }
            )
          }
        }

        // Live Market Candlestick Chart Overview with real-time tool detection & independent toggling
        item(key = "live_market_overview_chart") {
          LiveMarketOverviewChart(
            state = state,
            onSwitchTicker = { ticker -> viewModel.switchTicker(ticker) },
            onToggleTool = { toolId -> viewModel.toggleToolSelection(toolId) }
          )
        }

        // Section Title: "Tools" with interactive view mode switcher icons
        item(key = "section_title") {
          SectionTitleRow(
            activeCount = state.tools.count { it.isSelected },
            totalCount = state.tools.size,
            currentViewMode = state.toolViewMode,
            onSelectViewMode = { mode -> viewModel.setToolViewMode(mode) }
          )
        }

        // Filter tools based on search
        val filteredTools = state.tools.filter { tool ->
          state.searchQuery.isBlank() ||
              tool.name.contains(state.searchQuery, ignoreCase = true) ||
              tool.code.contains(state.searchQuery, ignoreCase = true)
        }

        when (state.toolViewMode) {
          ToolViewMode.CARDS -> {
            items(
              items = filteredTools,
              key = { "card_${it.id}" }
            ) { tool ->
              ToolSelectionCard(
                tool = tool,
                searchQuery = state.searchQuery,
                onSelect = { viewModel.toggleToolSelection(tool.id) },
                onToggleAlert = { viewModel.toggleToolAlert(tool.id) },
                onToggleFavorite = { viewModel.toggleToolFavorite(tool.id) },
                onInspect = { viewModel.openToolInspection(tool) }
              )
            }
          }

          ToolViewMode.GRID -> {
            val toolPairs = filteredTools.chunked(2)
            items(
              items = toolPairs,
              key = { pair -> "grid_row_${pair.joinToString("_") { it.id }}" }
            ) { pair ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                for (tool in pair) {
                  Box(modifier = Modifier.weight(1f)) {
                    GridToolCard(
                      tool = tool,
                      searchQuery = state.searchQuery,
                      onSelect = { viewModel.toggleToolSelection(tool.id) },
                      onToggleFavorite = { viewModel.toggleToolFavorite(tool.id) },
                      onInspect = { viewModel.openToolInspection(tool) }
                    )
                  }
                }
                if (pair.size == 1) {
                  Spacer(modifier = Modifier.weight(1f))
                }
              }
            }
          }

          ToolViewMode.LIST -> {
            items(
              items = filteredTools,
              key = { "list_${it.id}" }
            ) { tool ->
              ListToolRow(
                tool = tool,
                searchQuery = state.searchQuery,
                onSelect = { viewModel.toggleToolSelection(tool.id) },
                onToggleAlert = { viewModel.toggleToolAlert(tool.id) },
                onToggleFavorite = { viewModel.toggleToolFavorite(tool.id) },
                onInspect = { viewModel.openToolInspection(tool) }
              )
            }
          }
        }
      }
    }

    // 3. Inspect Engine Modal with animated top-down frosted blur sweep
    AnimatedVisibility(
      visible = state.inspectingTool != null,
      enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
          slideInVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            initialOffsetY = { it }
          ),
      exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
          slideOutVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
            targetOffsetY = { it }
          )
    ) {
      state.inspectingTool?.let { tool ->
        ToolInspectionModal(
          tool = tool,
          state = state,
          onClose = { viewModel.openToolInspection(null) },
          onToggleSelection = { viewModel.toggleToolSelection(tool.id) },
          onToggleAlert = { viewModel.toggleToolAlert(tool.id) },
          onToggleFavorite = { viewModel.toggleToolFavorite(tool.id) }
        )
      }
    }

    // 4. Export Favorites JSON Success Modal
    AnimatedVisibility(
      visible = state.exportDialogState != null,
      enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
          slideInVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
            initialOffsetY = { it }
          ),
      exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
          slideOutVertically(
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
            targetOffsetY = { it }
          )
    ) {
      state.exportDialogState?.let { exportInfo ->
        ExportSuccessModal(
          exportInfo = exportInfo,
          onClose = { viewModel.dismissExportDialog() }
        )
      }
    }
  }
}

/**
 * Static ambient background lighting system with disciplined frosted glow spheres.
 */
@Composable
private fun AmbientGlassAtmosphere() {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    // Subtle neon cyan ambient sphere (top-right)
    drawCircle(
      color = NeonCyan.copy(alpha = 0.035f),
      center = Offset(w * 0.88f, h * 0.12f),
      radius = w * 0.65f
    )

    // Subtle neon violet ambient sphere (center-left)
    drawCircle(
      color = NeonViolet.copy(alpha = 0.025f),
      center = Offset(w * 0.12f, h * 0.48f),
      radius = w * 0.60f
    )

    // Subtle emerald ambient sphere (bottom-right)
    drawCircle(
      color = NeonEmerald.copy(alpha = 0.025f),
      center = Offset(w * 0.85f, h * 0.82f),
      radius = w * 0.70f
    )
  }
}

/**
 * Clean dashboard header shifted up, with Search Bar directly integrated where currency and price lists were.
 */
@Composable
private fun DashboardHeader(
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  onNavigateToCode: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 6.dp)
  ) {
    // Brand title row with Top-Right Code Button
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(Color.White)
            .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(11.dp))
            .padding(3.dp),
          contentAlignment = Alignment.Center
        ) {
          Image(
            painter = painterResource(id = R.drawable.user_app_icon_1791989101956),
            contentDescription = "Notify Brand Logo",
            modifier = Modifier.fillMaxSize()
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            GlassText(
              text = "NOTIFY",
              style = NotifyTheme.typography.brandTitle.copy(fontSize = 22.sp),
              color = TextHighContrast
            )
            Spacer(modifier = Modifier.width(7.dp))
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(NeonCyan)
            )
          }
          GlassText(
            text = "PRECISION TRADING ARCHITECTURE",
            style = NotifyTheme.typography.caption.copy(fontSize = 9.5.sp, letterSpacing = 0.8.sp),
            color = Color.White.copy(alpha = 0.75f)
          )
        }
      }

      // Top-Right Action Button: Code Button with subtle neon cyan accent
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(NeonCyan.copy(alpha = 0.10f))
            .border(
              width = 1.dp,
              color = NeonCyan.copy(alpha = 0.35f),
              shape = RoundedCornerShape(12.dp)
            )
            .clickable { onNavigateToCode() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("code_navigation_button"),
          contentAlignment = Alignment.Center
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
          ) {
            GlassGeometricIcon(
              iconType = GeometricIconType.PULSE_SIGNAL,
              tint = NeonCyan,
              modifier = Modifier.size(13.dp)
            )
            GlassText(
              text = "Code",
              style = NotifyTheme.typography.headingMedium.copy(
                fontSize = 13.sp,
                letterSpacing = 0.3.sp
              ),
              color = TextHighContrast
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Search indicator bar shifted right where currency list and price were
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(GlassWhiteUltraSubtle)
        .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
        .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        GlassGeometricIcon(
          iconType = GeometricIconType.PULSE_SIGNAL,
          tint = NeonCyan,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        BasicTextField(
          value = searchQuery,
          onValueChange = onSearchChange,
          textStyle = NotifyTheme.typography.bodyPrimary.copy(fontSize = 13.5.sp),
          cursorBrush = SolidColor(NeonCyan),
          singleLine = true,
          modifier = Modifier
            .weight(1f)
            .testTag("search_tools_input"),
          decorationBox = { innerTextField ->
            if (searchQuery.isEmpty()) {
              GlassText(
                text = "Search tools (Market Structure, Break of Structure...)",
                style = NotifyTheme.typography.bodySecondary.copy(fontSize = 13.sp),
                color = TextLowContrast
              )
            }
            innerTextField()
          }
        )
      }
    }
  }
}

/**
 * Bar displayed ONLY when user has made at least one tool their favorite.
 */
@Composable
private fun FavoriteToolsBar(
  favorites: List<TradingTool>,
  onToolTap: (TradingTool) -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      GlassGeometricIcon(
        iconType = GeometricIconType.STAR_FILLED,
        tint = NeonAmber,
        modifier = Modifier.size(13.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      GlassText(
        text = "FAVORITE ENGINES",
        style = NotifyTheme.typography.caption.copy(
          fontSize = 10.5.sp,
          letterSpacing = 0.5.sp
        ),
        color = NeonAmber
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(rememberScrollState()),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      favorites.forEach { tool ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(
              1.dp,
              Color.White.copy(alpha = 0.22f),
              RoundedCornerShape(12.dp)
            )
            .clickable { onToolTap(tool) }
            .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            GlassGeometricIcon(
              iconType = GeometricIconType.STAR_FILLED,
              tint = Color.White,
              modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            GlassText(
              text = tool.code,
              style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 12.sp),
              color = TextHighContrast
            )
            Spacer(modifier = Modifier.width(6.dp))
            GlassText(
              text = tool.name,
              style = NotifyTheme.typography.bodySecondary.copy(fontSize = 12.sp),
              color = TextMediumContrast
            )
          }
        }
      }
    }
  }
}

/**
 * Live Market Candlestick Chart Overview displaying real-time independent tool logic
 * and instant one-tap tool activation chips.
 */
@Composable
private fun LiveMarketOverviewChart(
  state: DashboardUiState,
  onSwitchTicker: (String) -> Unit,
  onToggleTool: (String) -> Unit
) {
  val tickers = listOf("BTC/USDT", "ETH/USDT", "NQ100")

  GlassPanel(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .testTag("live_market_chart_overview"),
    shape = RoundedCornerShape(22.dp),
    isSelected = false,
    glowColor = NeonCyan.copy(alpha = 0.08f),
    isInteractive = false
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
    ) {
      // Top row: Title + Ticker Switchers + Live Price
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(NeonEmerald)
            )
            Spacer(modifier = Modifier.width(6.dp))
            GlassText(
              text = "LIVE CANDLESTICK ENGINE",
              style = NotifyTheme.typography.caption.copy(
                fontSize = 10.5.sp,
                letterSpacing = 0.6.sp
              ),
              color = NeonCyan
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Row(verticalAlignment = Alignment.Bottom) {
            GlassText(
              text = "\$${"%,.2f".format(state.currentPrice)}",
              style = NotifyTheme.typography.headingLarge.copy(fontSize = 18.sp),
              color = TextHighContrast
            )
            Spacer(modifier = Modifier.width(6.dp))
            GlassText(
              text = "+2.84%",
              style = NotifyTheme.typography.caption.copy(fontSize = 11.sp),
              color = NeonEmerald
            )
          }
        }

        // Ticker switcher pills
        Row(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          tickers.forEach { ticker ->
            val isActive = state.activeTicker == ticker
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isActive) NeonCyan.copy(alpha = 0.20f) else GlassWhiteUltraSubtle)
                .border(
                  width = 0.8.dp,
                  color = if (isActive) NeonCyan.copy(alpha = 0.70f) else GlassBorderSubtle,
                  shape = RoundedCornerShape(8.dp)
                )
                .clickable { onSwitchTicker(ticker) }
                .padding(horizontal = 8.dp, vertical = 5.dp)
                .testTag("ticker_chip_$ticker")
            ) {
              GlassText(
                text = ticker,
                style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 10.sp),
                color = if (isActive) NeonCyan else TextLowContrast
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // The Live Candlestick Chart reflecting independent tool states
      GlassTradingChart(
        candles = state.candles,
        swings = state.swings,
        events = state.events,
        modifier = Modifier
          .fillMaxWidth()
          .height(210.dp),
        showMSS = state.tools.find { it.id == "mss" }?.isSelected == true,
        showBOS = state.tools.find { it.id == "bos" }?.isSelected == true,
        showApex = state.tools.find { it.id == "apex" }?.isSelected == true,
        showFVG = state.tools.find { it.id == "fvg" }?.isSelected == true,
        showOrderBlock = state.tools.find { it.id == "order_block" }?.isSelected == true,
        showLiqSweep = state.tools.find { it.id == "liq_sweep" }?.isSelected == true
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Independent Tool Filter Pills: Tap to immediately toggle tool logic on the chart
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        state.tools.forEach { tool ->
          val toolAccent = when (tool.id) {
            "mss" -> NeonCyan
            "bos" -> NeonEmerald
            "apex" -> NeonAmber
            "fvg" -> NeonCoral
            "order_block" -> NeonViolet
            else -> NeonCoral
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(9.dp))
              .background(if (tool.isSelected) toolAccent.copy(alpha = 0.18f) else GlassWhiteUltraSubtle)
              .border(
                width = 0.8.dp,
                color = if (tool.isSelected) toolAccent.copy(alpha = 0.65f) else GlassBorderSubtle,
                shape = RoundedCornerShape(9.dp)
              )
              .clickable { onToggleTool(tool.id) }
              .padding(horizontal = 9.dp, vertical = 5.dp)
              .testTag("chart_filter_tool_${tool.id}")
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(if (tool.isSelected) toolAccent else Color.White.copy(alpha = 0.3f))
              )
              GlassText(
                text = tool.code,
                style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 10.5.sp),
                color = if (tool.isSelected) TextHighContrast else TextLowContrast
              )
              GlassBadge(
                text = "${tool.detectedEventsCount}",
                tint = if (tool.isSelected) toolAccent else TextDisabled,
                backgroundColor = if (tool.isSelected) toolAccent.copy(alpha = 0.15f) else Color.Transparent
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Section Title renamed to "Tools" with interactive view mode switcher icons (Cards, Grid, List).
 */
@Composable
private fun SectionTitleRow(
  activeCount: Int,
  totalCount: Int,
  currentViewMode: ToolViewMode,
  onSelectViewMode: (ToolViewMode) -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column(modifier = Modifier.weight(1f, fill = false)) {
      GlassText(
        text = "Tools",
        style = NotifyTheme.typography.headingLarge.copy(fontSize = 20.sp),
        color = TextHighContrast
      )
      GlassText(
        text = when (currentViewMode) {
          ToolViewMode.CARDS -> "Card layout • Default view with live charts"
          ToolViewMode.GRID -> "Grid layout • Compact multi-column matrix"
          ToolViewMode.LIST -> "List layout • Quick scanning and fast toggles"
        },
        style = NotifyTheme.typography.caption.copy(fontSize = 11.5.sp),
        color = TextLowContrast
      )
    }

    Spacer(modifier = Modifier.width(8.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // 3-Icon View Mode Switcher: Cards, Grid, List
      ViewModeSelector(
        currentMode = currentViewMode,
        onSelectMode = onSelectViewMode
      )

      GlassBadge(
        text = "$activeCount / $totalCount ON",
        tint = NeonCyan,
        backgroundColor = NeonCyan.copy(alpha = 0.12f)
      )
    }
  }
}

/**
 * Glassmorphic segmented switcher with one-tap icons for Card, Grid, and List layouts.
 */
@Composable
private fun ViewModeSelector(
  currentMode: ToolViewMode,
  onSelectMode: (ToolViewMode) -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .background(Color.White.copy(alpha = 0.05f))
      .border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.16f),
        shape = RoundedCornerShape(12.dp)
      )
      .padding(3.dp)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      val options = listOf(
        Triple(ToolViewMode.CARDS, GeometricIconType.VIEW_CARDS, "view_mode_cards_button"),
        Triple(ToolViewMode.GRID, GeometricIconType.VIEW_GRID, "view_mode_grid_button"),
        Triple(ToolViewMode.LIST, GeometricIconType.VIEW_LIST, "view_mode_list_button")
      )

      options.forEach { (mode, iconType, tag) ->
        val isSelected = currentMode == mode
        val scale by animateFloatAsState(
          targetValue = if (isSelected) 1.05f else 1.0f,
          animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
          ),
          label = "modeScale_${mode.name}"
        )
        val bgAlpha by animateFloatAsState(
          targetValue = if (isSelected) 0.22f else 0.0f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "modeBg_${mode.name}"
        )
        val borderAlpha by animateFloatAsState(
          targetValue = if (isSelected) 0.70f else 0.0f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "modeBorder_${mode.name}"
        )

        Box(
          modifier = Modifier
            .size(34.dp)
            .graphicsLayer {
              scaleX = scale
              scaleY = scale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(
              if (isSelected) NeonCyan.copy(alpha = bgAlpha) else Color.Transparent
            )
            .border(
              width = if (isSelected) 1.dp else 0.dp,
              color = if (isSelected) NeonCyan.copy(alpha = borderAlpha) else Color.Transparent,
              shape = RoundedCornerShape(8.dp)
            )
            .clickable { onSelectMode(mode) }
            .testTag(tag),
          contentAlignment = Alignment.Center
        ) {
          GlassGeometricIcon(
            iconType = iconType,
            tint = if (isSelected) NeonCyan else Color.White.copy(alpha = 0.55f),
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

/**
 * Frosted Glass Tool Card with favorite toggle star and spring physics.
 */
@Composable
private fun ToolSelectionCard(
  tool: TradingTool,
  searchQuery: String,
  onSelect: () -> Unit,
  onToggleAlert: () -> Unit,
  onToggleFavorite: () -> Unit,
  onInspect: () -> Unit
) {
  val iconType = when (tool.id) {
    "mss" -> GeometricIconType.MSS_SHIFT
    "bos" -> GeometricIconType.BOS_TREND
    "apex" -> GeometricIconType.APEX_LEVEL
    "fvg" -> GeometricIconType.FAIR_VALUE_GAP
    "order_block" -> GeometricIconType.ORDER_BLOCK
    else -> GeometricIconType.PULSE_SIGNAL
  }

  val accentColor = when (tool.id) {
    "mss" -> NeonCyan
    "bos" -> NeonEmerald
    "apex" -> NeonAmber
    "fvg" -> NeonCoral
    "order_block" -> NeonViolet
    else -> NeonCyan
  }

  var isHovered by remember { mutableStateOf(false) }
  var isLongPressed by remember { mutableStateOf(false) }
  val showTooltip = isHovered || isLongPressed

  GlassPanel(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp)
      .testTag("tool_card_${tool.id}"),
    shape = RoundedCornerShape(22.dp),
    isSelected = tool.isSelected,
    glowColor = accentColor.copy(alpha = 0.12f),
    onClick = onSelect,
    onLongClick = { isLongPressed = !isLongPressed },
    onHoverChange = { isHovered = it }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      // Top Row: Glyph + Code + Badges + Info Tooltip Trigger + Favorite Star + Selection Checkbox
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(accentColor.copy(alpha = 0.15f))
              .border(0.8.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
          ) {
            GlassGeometricIcon(
              iconType = iconType,
              tint = accentColor,
              modifier = Modifier.size(18.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          GlassBadge(
            text = tool.code,
            tint = accentColor,
            backgroundColor = accentColor.copy(alpha = 0.12f)
          )

          Spacer(modifier = Modifier.width(8.dp))

          GlassBadge(
            text = tool.statusBadge,
            tint = TextMediumContrast,
            backgroundColor = GlassWhiteUltraSubtle
          )

          Spacer(modifier = Modifier.width(8.dp))

          // Info icon button to inspect engine logic or reveal tooltip
          Box(
            modifier = Modifier
              .size(24.dp)
              .clip(CircleShape)
              .background(if (showTooltip) accentColor.copy(alpha = 0.25f) else GlassWhiteUltraSubtle)
              .border(0.7.dp, if (showTooltip) accentColor else GlassBorderSubtle, CircleShape)
              .clickable { isLongPressed = !isLongPressed }
              .testTag("tooltip_trigger_${tool.id}"),
            contentAlignment = Alignment.Center
          ) {
            GlassGeometricIcon(
              iconType = GeometricIconType.INFO_CIRCLE,
              tint = if (showTooltip) accentColor else TextLowContrast,
              modifier = Modifier.size(13.dp)
            )
          }
        }

        // Action controls: Star Favorite + Selection Checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
          // Favorite Star Button
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(if (tool.isFavorite) NeonAmber.copy(alpha = 0.18f) else GlassWhiteUltraSubtle)
              .border(0.8.dp, if (tool.isFavorite) NeonAmber.copy(alpha = 0.6f) else GlassBorderSubtle, CircleShape)
              .clickable { onToggleFavorite() },
            contentAlignment = Alignment.Center
          ) {
            GlassGeometricIcon(
              iconType = if (tool.isFavorite) GeometricIconType.STAR_FILLED else GeometricIconType.STAR_OUTLINE,
              tint = if (tool.isFavorite) NeonAmber else TextLowContrast,
              modifier = Modifier.size(15.dp)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          // Selection Checkmark Box
          Box(
            modifier = Modifier
              .size(26.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(if (tool.isSelected) accentColor else GlassWhiteUltraSubtle)
              .border(
                1.dp,
                if (tool.isSelected) accentColor else GlassBorderSubtle,
                RoundedCornerShape(8.dp)
              )
              .testTag("checkbox_${tool.id}"),
            contentAlignment = Alignment.Center
          ) {
            if (tool.isSelected) {
              GlassGeometricIcon(
                iconType = GeometricIconType.CHECK,
                tint = Color(0xFF070B12),
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Title & Algorithmic Summary
      val highlightedName = buildHighlightedAnnotatedString(
        fullText = tool.name,
        query = searchQuery,
        defaultColor = TextHighContrast,
        highlightColor = accentColor
      )
      GlassText(
        text = highlightedName,
        style = NotifyTheme.typography.headingMedium.copy(fontSize = 17.sp)
      )

      Spacer(modifier = Modifier.height(6.dp))

      GlassText(
        text = tool.summary,
        style = NotifyTheme.typography.bodySecondary.copy(fontSize = 13.sp),
        color = TextMediumContrast
      )

      // Hover / Long-Press Tooltip Box providing concise explanation of the analysis engine
      AnimatedVisibility(
        visible = showTooltip,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
            expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
            shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy))
      ) {
        FrostedEngineTooltip(
          tool = tool,
          accentColor = accentColor,
          onDismiss = {
            isLongPressed = false
            isHovered = false
          }
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Telemetry Data Bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(GlassWhiteUltraSubtle)
          .border(0.6.dp, GlassBorderSubtle, RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        MetricItem(label = "WIN RATE", value = "${tool.winRateEstimate}%", color = NeonEmerald)
        MetricItem(label = "LOOKBACK", value = "k=${tool.lookbackPeriod}", color = TextHighContrast)
        MetricItem(label = "SENSITIVITY", value = "${(tool.sensitivityScore * 100).toInt()}%", color = accentColor)
        MetricItem(label = "TRIGGERS", value = "${tool.detectedEventsCount}", color = TextHighContrast)
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Bottom Action Bar: Alert Switch & Inspect Button
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { onToggleAlert() }
        ) {
          GlassSwitch(
            checked = tool.isAlertActive,
            onCheckedChange = { onToggleAlert() },
            activeColor = accentColor
          )
          Spacer(modifier = Modifier.width(8.dp))
          GlassText(
            text = if (tool.isAlertActive) "Instant Alerts ON" else "Alerts Muted",
            style = NotifyTheme.typography.caption.copy(fontSize = 11.5.sp),
            color = if (tool.isAlertActive) TextHighContrast else TextLowContrast
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(GlassWhiteSubtle)
            .border(0.8.dp, GlassBorderSubtle, RoundedCornerShape(10.dp))
            .clickable { onInspect() }
            .padding(horizontal = 12.dp, vertical = 7.dp)
            .testTag("inspect_${tool.id}"),
          contentAlignment = Alignment.Center
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            GlassText(
              text = "Inspect Engine",
              style = NotifyTheme.typography.caption.copy(
                fontSize = 11.5.sp,
                color = NeonCyan
              )
            )
            Spacer(modifier = Modifier.width(4.dp))
            GlassGeometricIcon(
              iconType = GeometricIconType.CHEVRON_RIGHT,
              tint = NeonCyan,
              modifier = Modifier.size(12.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun MetricItem(label: String, value: String, color: Color) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    GlassText(
      text = label,
      style = NotifyTheme.typography.caption.copy(fontSize = 9.sp),
      color = TextLowContrast
    )
    Spacer(modifier = Modifier.height(2.dp))
    GlassText(
      text = value,
      style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 11.5.sp),
      color = color
    )
  }
}

/**
 * Concise analysis engine tooltip shown on hover or long-press.
 */
@Composable
private fun FrostedEngineTooltip(
  tool: TradingTool,
  accentColor: Color,
  onDismiss: () -> Unit
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .padding(top = 8.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xF50D1524))
      .border(
        width = 1.dp,
        color = Color.White.copy(alpha = 0.25f),
        shape = RoundedCornerShape(16.dp)
      )
      .padding(14.dp)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss
      )
      .testTag("engine_tooltip_${tool.id}")
  ) {
    Column {
      // Header of tooltip
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(6.dp)
              .clip(CircleShape)
              .background(accentColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          GlassText(
            text = "ENGINE SPECIFICATION",
            style = NotifyTheme.typography.caption.copy(
              fontSize = 10.sp,
              letterSpacing = 0.6.sp
            ),
            color = accentColor
          )
        }

        Box(
          modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(GlassWhiteSubtle)
            .clickable { onDismiss() },
          contentAlignment = Alignment.Center
        ) {
          GlassGeometricIcon(
            iconType = GeometricIconType.CLOSE,
            tint = TextLowContrast,
            modifier = Modifier.size(10.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Concise explanation
      GlassText(
        text = if (tool.engineTooltipExplanation.isNotBlank()) tool.engineTooltipExplanation else tool.summary,
        style = NotifyTheme.typography.bodySecondary.copy(
          fontSize = 12.5.sp,
          lineHeight = 17.sp
        ),
        color = TextHighContrast
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Micro telemetry parameters
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        GlassText(
          text = "Pivots: k=${tool.lookbackPeriod}",
          style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 10.5.sp),
          color = TextMediumContrast
        )
        GlassText(
          text = "•",
          style = NotifyTheme.typography.caption,
          color = TextLowContrast
        )
        GlassText(
          text = "Sensitivity: ${(tool.sensitivityScore * 100).toInt()}%",
          style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 10.5.sp),
          color = accentColor
        )
        GlassText(
          text = "•",
          style = NotifyTheme.typography.caption,
          color = TextLowContrast
        )
        GlassText(
          text = "Tap to dismiss",
          style = NotifyTheme.typography.caption.copy(fontSize = 9.5.sp),
          color = TextLowContrast
        )
      }
    }
  }
}

/**
 * Inspect Engine Modal with animated top-to-bottom frosted glass sweep.
 * Ensures the background is dimmed and softened without hiding colors, while the card background
 * sweeps from the top down and child text remains ultra-sharp.
 */
@Composable
private fun ToolInspectionModal(
  tool: TradingTool,
  state: DashboardUiState,
  onClose: () -> Unit,
  onToggleSelection: () -> Unit,
  onToggleAlert: () -> Unit,
  onToggleFavorite: () -> Unit
) {
  var animationTrigger by remember { mutableStateOf(false) }
  LaunchedEffect(tool.id) {
    animationTrigger = true
  }

  // Animation for the top-to-bottom frosted glass wash
  val frostedProgress by animateFloatAsState(
    targetValue = if (animationTrigger) 1f else 0f,
    animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing),
    label = "frostedProgressAnim"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0x75070A0F)) // Subtle translucent scrim so background colors show through softly
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClose
      ),
    contentAlignment = Alignment.BottomCenter
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {} // absorb taps inside modal
        )
        .testTag("tool_inspection_modal")
        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        .drawBehind {
          // Dynamic top-down frosted sweep
          val currentSweepH = size.height * frostedProgress

          // Translucent base frosted glass
          drawRect(
            color = Color(0xF2090E17),
            topLeft = Offset.Zero,
            size = size
          )

          // Top-to-bottom frosted wash (pure white translucent wash)
          drawRect(
            color = Color.White.copy(alpha = 0.06f * (1f - (frostedProgress * 0.5f))),
            topLeft = Offset.Zero,
            size = Size(size.width, currentSweepH)
          )

          // Specular wave highlight at leading sweep edge (pure white)
          if (frostedProgress < 0.98f) {
            drawLine(
              color = Color.White.copy(alpha = 0.85f),
              start = Offset(0f, currentSweepH),
              end = Offset(size.width, currentSweepH),
              strokeWidth = 2.dp.toPx()
            )
          }

          // Top ambient specular highlight
          drawLine(
            color = Color.White.copy(alpha = 0.40f),
            start = Offset(0f, 0f),
            end = Offset(size.width, 0f),
            strokeWidth = 1.5.dp.toPx()
          )
        }
        .border(
          width = 1.dp,
          color = Color.White.copy(alpha = 0.35f),
          shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        )
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(22.dp)
      ) {
        // Drag Pill Handle
        Box(
          modifier = Modifier
            .size(width = 44.dp, height = 4.dp)
            .clip(CircleShape)
            .background(GlassBorderHighlight)
            .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Modal Title Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              GlassBadge(text = tool.code, tint = NeonCyan)
              Spacer(modifier = Modifier.width(8.dp))
              GlassText(
                text = tool.name,
                style = NotifyTheme.typography.headingLarge.copy(fontSize = 19.sp),
                color = TextHighContrast
              )
            }
            GlassText(
              text = tool.category.title,
              style = NotifyTheme.typography.caption.copy(fontSize = 11.sp),
              color = NeonEmerald
            )
          }

          Row(verticalAlignment = Alignment.CenterVertically) {
            // Favorite Toggle inside Modal
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (tool.isFavorite) NeonAmber.copy(alpha = 0.2f) else GlassWhiteSubtle)
                .border(0.8.dp, if (tool.isFavorite) NeonAmber.copy(alpha = 0.6f) else GlassBorderSubtle, CircleShape)
                .clickable { onToggleFavorite() },
              contentAlignment = Alignment.Center
            ) {
              GlassGeometricIcon(
                iconType = if (tool.isFavorite) GeometricIconType.STAR_FILLED else GeometricIconType.STAR_OUTLINE,
                tint = if (tool.isFavorite) NeonAmber else TextMediumContrast,
                modifier = Modifier.size(15.dp)
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Close Button
            Box(
              modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(GlassWhiteSubtle)
                .border(0.8.dp, GlassBorderSubtle, CircleShape)
                .clickable { onClose() },
              contentAlignment = Alignment.Center
            ) {
              GlassGeometricIcon(
                iconType = GeometricIconType.CLOSE,
                tint = TextHighContrast,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Interactive Live Chart
        GlassText(
          text = "LIVE MATHEMATICAL DETECTION",
          style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
          color = TextLowContrast
        )

        Spacer(modifier = Modifier.height(8.dp))

        GlassTradingChart(
          candles = state.candles,
          swings = state.swings,
          events = state.events,
          modifier = Modifier
            .fillMaxWidth()
            .height(210.dp),
          showMSS = tool.id == "mss",
          showBOS = tool.id == "bos",
          showApex = tool.id == "apex",
          showFVG = tool.id == "fvg",
          showOrderBlock = tool.id == "order_block",
          showLiqSweep = tool.id == "liq_sweep",
          activeToolId = tool.id
        )

        val detectedToolEvents = state.events.filter { event ->
          when (tool.id) {
            "mss" -> event.type == StructureType.MSS_BULLISH || event.type == StructureType.MSS_BEARISH
            "bos" -> event.type == StructureType.BOS_BULLISH || event.type == StructureType.BOS_BEARISH
            "apex" -> event.type == StructureType.APEX_HIGH || event.type == StructureType.APEX_LOW
            "fvg" -> event.type == StructureType.FVG_BULLISH || event.type == StructureType.FVG_BEARISH
            "order_block" -> event.type == StructureType.ORDER_BLOCK_BULLISH || event.type == StructureType.ORDER_BLOCK_BEARISH
            "liq_sweep" -> event.type == StructureType.LIQ_SWEEP_HIGH || event.type == StructureType.LIQ_SWEEP_LOW
            else -> false
          }
        }

        if (detectedToolEvents.isNotEmpty()) {
          Spacer(modifier = Modifier.height(14.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            GlassText(
              text = "INDEPENDENT DETECTIONS ON CHART",
              style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
              color = NeonEmerald
            )
            GlassBadge(
              text = "${detectedToolEvents.size} Triggers Confirmed",
              tint = NeonEmerald,
              backgroundColor = NeonEmerald.copy(alpha = 0.12f)
            )
          }
          Spacer(modifier = Modifier.height(8.dp))
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(GlassWhiteUltraSubtle)
              .border(0.8.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
              .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            detectedToolEvents.take(4).forEach { ev ->
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(6.dp)
                      .clip(CircleShape)
                      .background(if (ev.type.isBullish) NeonEmerald else NeonCoral)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  GlassText(
                    text = ev.type.displayName,
                    style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 11.sp),
                    color = TextHighContrast
                  )
                }
                GlassText(
                  text = "\$${"%.2f".format(ev.triggerPrice)}",
                  style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 11.sp),
                  color = NeonCyan
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Algorithmic Specification Box
        GlassPanel(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          isInteractive = false
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            GlassText(
              text = "ALGORITHMIC SPECIFICATION",
              style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
              color = NeonCyan
            )
            Spacer(modifier = Modifier.height(6.dp))
            GlassText(
              text = tool.algorithmDetails,
              style = NotifyTheme.typography.bodySecondary.copy(fontSize = 12.5.sp),
              color = TextMediumContrast
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Action Buttons Row
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          GlassButton(
            text = if (tool.isSelected) "Disable Engine" else "Enable Engine",
            onClick = onToggleSelection,
            isPrimary = !tool.isSelected,
            icon = if (tool.isSelected) null else GeometricIconType.CHECK,
            modifier = Modifier.weight(1f)
          )

          GlassButton(
            text = if (tool.isAlertActive) "Mute Alerts" else "Enable Alerts",
            onClick = onToggleAlert,
            isPrimary = false,
            icon = GeometricIconType.BELL_NOTIFY,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}

/**
 * Frosted Glass Export Success Dialog confirming local JSON file creation.
 * Provides metadata details, configuration preview, and quick copy/share actions.
 */
@Composable
private fun ExportSuccessModal(
  exportInfo: ExportDialogState,
  onClose: () -> Unit
) {
  val context = LocalContext.current
  var animationTrigger by remember { mutableStateOf(false) }
  LaunchedEffect(exportInfo.fileName) {
    animationTrigger = true
  }

  val frostedProgress by animateFloatAsState(
    targetValue = if (animationTrigger) 1f else 0f,
    animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
    label = "exportFrostedProgressAnim"
  )

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(Color(0x75070A0F))
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onClose
      ),
    contentAlignment = Alignment.BottomCenter
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = null,
          onClick = {} // Absorb taps inside modal
        )
        .testTag("export_success_modal")
        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
        .drawBehind {
          val currentSweepH = size.height * frostedProgress
          drawRect(
            color = Color(0xF2090E17),
            topLeft = Offset.Zero,
            size = size
          )
          // Pure white frosted sweep
          drawRect(
            color = Color.White.copy(alpha = 0.06f * (1f - (frostedProgress * 0.5f))),
            topLeft = Offset.Zero,
            size = Size(size.width, currentSweepH)
          )
          if (frostedProgress < 0.98f) {
            drawLine(
              color = Color.White.copy(alpha = 0.85f),
              start = Offset(0f, currentSweepH),
              end = Offset(size.width, currentSweepH),
              strokeWidth = 2.dp.toPx()
            )
          }
        }
        .border(
          width = 1.dp,
          color = Color.White.copy(alpha = 0.35f),
          shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        )
        .padding(horizontal = 24.dp, vertical = 24.dp)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
      ) {
        // Modal Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(NeonEmerald.copy(alpha = 0.18f))
                .border(1.dp, NeonEmerald.copy(alpha = 0.70f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              GlassGeometricIcon(
                iconType = GeometricIconType.CHECK,
                tint = NeonEmerald,
                modifier = Modifier.size(16.dp)
              )
            }

            Column {
              GlassText(
                text = "CONFIG EXPORTED",
                style = NotifyTheme.typography.headingLarge.copy(fontSize = 17.sp),
                color = TextHighContrast
              )
              GlassText(
                text = "Saved to local JSON storage",
                style = NotifyTheme.typography.caption.copy(fontSize = 11.sp),
                color = NeonAmber
              )
            }
          }

          // Close button
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(CircleShape)
              .background(GlassWhiteSubtle)
              .border(0.8.dp, GlassBorderSubtle, CircleShape)
              .clickable { onClose() },
            contentAlignment = Alignment.Center
          ) {
            GlassGeometricIcon(
              iconType = GeometricIconType.CLOSE,
              tint = TextHighContrast,
              modifier = Modifier.size(14.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // File Details Panel
        GlassPanel(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          isInteractive = false
        ) {
          Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              GlassText(
                text = "FILE NAME",
                style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
                color = TextLowContrast
              )
              GlassBadge(
                text = exportInfo.fileName,
                tint = NeonCyan
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              GlassText(
                text = "TARGET PATH",
                style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
                color = TextLowContrast
              )
              GlassText(
                text = exportInfo.filePath,
                style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 10.5.sp),
                color = TextMediumContrast,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 200.dp)
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              GlassText(
                text = "EXPORTED TOOLS",
                style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
                color = TextLowContrast
              )
              GlassText(
                text = "${exportInfo.favoriteCount} Tools",
                style = NotifyTheme.typography.headingMedium.copy(fontSize = 12.5.sp),
                color = NeonEmerald
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              GlassText(
                text = "FILE SIZE",
                style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
                color = TextLowContrast
              )
              GlassText(
                text = exportInfo.fileSizeFormatted,
                style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 11.sp),
                color = TextHighContrast
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // JSON Code Preview Panel
        GlassText(
          text = "JSON CONFIGURATION PREVIEW",
          style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
          color = TextLowContrast
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xE005080E))
            .border(1.dp, GlassBorderSubtle, RoundedCornerShape(14.dp))
            .padding(12.dp)
        ) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .verticalScroll(rememberScrollState())
          ) {
            GlassText(
              text = exportInfo.jsonContent,
              style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 11.sp, lineHeight = 16.sp),
              color = NeonCyan
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Actions Row: Copy JSON, Share, and Done
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          GlassButton(
            text = "Copy JSON",
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Notify Config", exportInfo.jsonContent)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "JSON configuration copied to clipboard", Toast.LENGTH_SHORT).show()
            },
            isPrimary = false,
            icon = GeometricIconType.CHECK,
            modifier = Modifier.weight(1f)
          )

          GlassButton(
            text = "Share File",
            onClick = {
              val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Notify Tool Configuration")
                putExtra(Intent.EXTRA_TEXT, exportInfo.jsonContent)
              }
              context.startActivity(Intent.createChooser(shareIntent, "Share JSON Configuration"))
            },
            isPrimary = false,
            icon = GeometricIconType.EXPORT_FILE,
            modifier = Modifier.weight(1f)
          )

          GlassButton(
            text = "Done",
            onClick = onClose,
            isPrimary = true,
            modifier = Modifier.weight(1f)
          )
        }
      }
    }
  }
}



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
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInspect: () -> Unit,
    accentColor: Color = when (tool.id) {
        "mss" -> NeonCyan
        "bos" -> NeonEmerald
        "apex" -> NeonAmber
        "fvg" -> NeonCoral
        "order_block" -> NeonViolet
        else -> NeonCyan
    },
    iconType: GeometricIconType = when (tool.id) {
        "mss" -> GeometricIconType.MSS_SHIFT
        "bos" -> GeometricIconType.BOS_TREND
        "apex" -> GeometricIconType.APEX_LEVEL
        "fvg" -> GeometricIconType.FAIR_VALUE_GAP
        "order_block" -> GeometricIconType.ORDER_BLOCK
        else -> GeometricIconType.PULSE_SIGNAL
    }
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
                val winRateText = if (tool.winRateEstimate <= 1.0) {
                    "Win: ${(tool.winRateEstimate * 100).toInt()}%"
                } else {
                    "Win: ${tool.winRateEstimate.toInt()}%"
                }
                GlassBadge(
                    text = winRateText,
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

/**
 * Sleek glassmorphic list row for compact linear inspection and fast toggling.
 */
@Composable
private fun ListToolRow(
    tool: TradingTool,
    searchQuery: String,
    onSelect: () -> Unit,
    onToggleAlert: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInspect: () -> Unit
) {
    val iconType = when (tool.id) {
        "mss" -> GeometricIconType.MSS_SHIFT
        "bos" -> GeometricIconType.BOS_TREND
        "apex" -> GeometricIconType.APEX_LEVEL
        "fvg" -> GeometricIconType.FAIR_VALUE_GAP
        "order_block" -> GeometricIconType.ORDER_BLOCK
        else -> GeometricIconType.PULSE_SIGNAL
    }

    val accentColor = when (tool.id) {
        "mss" -> NeonCyan
        "bos" -> NeonEmerald
        "apex" -> NeonAmber
        "fvg" -> NeonCoral
        "order_block" -> NeonViolet
        else -> NeonCyan
    }

    GlassPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .testTag("list_tool_row_${tool.id}"),
        shape = RoundedCornerShape(16.dp),
        isSelected = tool.isSelected,
        glowColor = accentColor.copy(alpha = 0.14f),
        onClick = onSelect,
        onLongClick = onInspect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Tool icon in tinted glass container
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.16f))
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                GlassGeometricIcon(
                    iconType = iconType,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center: Code, Name, Win Rate & Summary
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GlassBadge(
                        text = tool.code,
                        tint = accentColor,
                        backgroundColor = accentColor.copy(alpha = 0.12f)
                    )
                    val highlightedName = buildHighlightedAnnotatedString(
                        fullText = tool.name,
                        query = searchQuery,
                        defaultColor = TextHighContrast,
                        highlightColor = accentColor
                    )
                    GlassText(
                        text = highlightedName,
                        style = NotifyTheme.typography.headingMedium.copy(fontSize = 14.5.sp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val winRateFormatted = if (tool.winRateEstimate <= 1.0) {
                        "${(tool.winRateEstimate * 100).toInt()}%"
                    } else {
                        "${tool.winRateEstimate.toInt()}%"
                    }
                    GlassBadge(
                        text = "Win: $winRateFormatted",
                        tint = NeonEmerald,
                        backgroundColor = NeonEmerald.copy(alpha = 0.10f)
                    )
                    GlassText(
                        text = tool.summary,
                        style = NotifyTheme.typography.caption.copy(fontSize = 11.sp),
                        color = TextLowContrast,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Right actions: Favorite star, Alert toggle, Inspect, Selection checkbox
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Favorite Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (tool.isFavorite) NeonAmber.copy(alpha = 0.2f) else GlassWhiteUltraSubtle)
                        .border(
                            0.8.dp,
                            if (tool.isFavorite) NeonAmber.copy(alpha = 0.6f) else GlassBorderSubtle,
                            CircleShape
                        )
                        .clickable { onToggleFavorite() }
                        .testTag("list_favorite_button_${tool.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    GlassGeometricIcon(
                        iconType = if (tool.isFavorite) GeometricIconType.STAR_FILLED else GeometricIconType.STAR_OUTLINE,
                        tint = if (tool.isFavorite) NeonAmber else TextLowContrast,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Alert Button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (tool.isAlertActive) NeonCyan.copy(alpha = 0.2f) else GlassWhiteUltraSubtle)
                        .border(
                            0.8.dp,
                            if (tool.isAlertActive) NeonCyan.copy(alpha = 0.6f) else GlassBorderSubtle,
                            CircleShape
                        )
                        .clickable { onToggleAlert() }
                        .testTag("list_alert_button_${tool.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    GlassGeometricIcon(
                        iconType = GeometricIconType.BELL_NOTIFY,
                        tint = if (tool.isAlertActive) NeonCyan else TextLowContrast,
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Inspect button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(GlassWhiteUltraSubtle)
                        .border(0.8.dp, GlassBorderSubtle, CircleShape)
                        .clickable { onInspect() }
                        .testTag("list_inspect_button_${tool.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    GlassGeometricIcon(
                        iconType = GeometricIconType.INFO_CIRCLE,
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier.size(13.dp)
                    )
                }

                // Selection Checkbox
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (tool.isSelected) accentColor else GlassWhiteUltraSubtle)
                        .border(
                            1.dp,
                            if (tool.isSelected) accentColor else GlassBorderSubtle,
                            RoundedCornerShape(7.dp)
                        )
                        .clickable { onSelect() }
                        .testTag("list_select_toggle_${tool.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (tool.isSelected) {
                        GlassGeometricIcon(
                            iconType = GeometricIconType.CHECK,
                            tint = Color(0xFF070B12),
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
            }
        }
    }
}
