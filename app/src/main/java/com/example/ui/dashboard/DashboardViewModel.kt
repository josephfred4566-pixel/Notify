package com.example.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trading.algorithm.MarketStructureEngine
import com.example.trading.model.Candle
import com.example.trading.model.StructureEvent
import com.example.trading.model.SwingPoint
import com.example.trading.model.ToolCategory
import com.example.trading.model.TradingTool
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

import android.content.Context
import java.io.File
import com.example.trading.codegen.AppScreen
import com.example.trading.codegen.CodeGeneratorEngine
import com.example.trading.codegen.CodeType
import com.example.trading.codegen.CustomFramework
import com.example.trading.codegen.PineScriptVersion

enum class ToolViewMode {
  CARDS,
  GRID,
  LIST
}

data class ExportDialogState(
  val fileName: String,
  val filePath: String,
  val jsonContent: String,
  val favoriteCount: Int,
  val fileSizeFormatted: String
)

data class DashboardUiState(
  val currentScreen: AppScreen = AppScreen.DASHBOARD,
  val selectedCodeType: CodeType = CodeType.CUSTOM_INDICATOR,
  val selectedFramework: CustomFramework = CustomFramework.PINE_SCRIPT,
  val pineScriptVersion: PineScriptVersion = PineScriptVersion.V5,
  val generatedCode: String = "",
  val includeMSSInExport: Boolean = true,
  val includeBOSInExport: Boolean = true,
  val includeApexInExport: Boolean = true,
  val tools: List<TradingTool> = emptyList(),
  val selectedToolId: String = "mss",
  val selectedCategory: ToolCategory = ToolCategory.ALL,
  val searchQuery: String = "",
  val toolViewMode: ToolViewMode = ToolViewMode.CARDS,
  val candles: List<Candle> = emptyList(),
  val swings: List<SwingPoint> = emptyList(),
  val events: List<StructureEvent> = emptyList(),
  val activeTicker: String = "BTC/USDT",
  val currentPrice: Double = 94820.50,
  val priceChange24h: Double = 3.42,
  val inspectingTool: TradingTool? = null,
  val selectedCandle: Candle? = null,
  val isLiveFeedActive: Boolean = true,
  val exportDialogState: ExportDialogState? = null,
  val isFirstLaunch: Boolean = true
)

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

  private val prefs = application.getSharedPreferences("notify_prefs", Context.MODE_PRIVATE)

  private val _uiState = MutableStateFlow(DashboardUiState())
  val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

  init {
    val isFirst = prefs.getBoolean("is_first_launch", true)
    val savedMode = prefs.getString("tool_view_mode", ToolViewMode.CARDS.name) ?: ToolViewMode.CARDS.name
    val initialViewMode = try {
      ToolViewMode.valueOf(savedMode)
    } catch (_: Exception) {
      ToolViewMode.CARDS
    }
    _uiState.update { it.copy(isFirstLaunch = isFirst, toolViewMode = initialViewMode) }
    initializeTools()
    loadMarketData()
  }

  fun setToolViewMode(mode: ToolViewMode) {
    prefs.edit().putString("tool_view_mode", mode.name).apply()
    _uiState.update { it.copy(toolViewMode = mode) }
  }

  fun completeOnboarding() {
    prefs.edit().putBoolean("is_first_launch", false).apply()
    _uiState.update { it.copy(isFirstLaunch = false) }
  }

  private fun initializeTools() {
    val initialTools = listOf(
      TradingTool(
        id = "mss",
        name = "Market Structure Shift",
        code = "MSS",
        category = ToolCategory.STRUCTURE,
        summary = "Displacement breaks beyond preceding swing pivots indicating a definitive change of character (ChoCh) and orderflow bias reversal.",
        algorithmDetails = "Mathematical lookback fractal scanner (k=3 to 7). Evaluates candle close displacement vs. Average True Body Range (>1.05x). Emits confirmed reversal alert when prior opposing swing high (bullish) or swing low (bearish) is closed through.",
        isSelected = true,
        isFavorite = true,
        isAlertActive = true,
        lookbackPeriod = 3,
        sensitivityScore = 0.92,
        winRateEstimate = 78.6,
        detectedEventsCount = 18,
        statusBadge = "PRIME SIGNAL",
        engineTooltipExplanation = "Scans k=3 fractal pivots. Flags reversal shifts when a candle closes beyond the prior opposing swing level with >1.05x ATR displacement impulse."
      ),
      TradingTool(
        id = "bos",
        name = "Break of Structure",
        code = "BOS",
        category = ToolCategory.STRUCTURE,
        summary = "Continuation signals confirming expansion beyond historical structural swing points within an established dominant trend.",
        algorithmDetails = "Scans continuous swing sequences (HH/HL or LH/LL). Flags candle bodies terminating beyond prior established swing peaks in direction of macro trend. Filters wicks to eliminate liquidity sweeps.",
        isSelected = true,
        isAlertActive = true,
        lookbackPeriod = 3,
        sensitivityScore = 0.88,
        winRateEstimate = 82.1,
        detectedEventsCount = 24,
        statusBadge = "CONTINUATION",
        engineTooltipExplanation = "Scans established trend sequences (HH/HL or LH/LL). Flags continuation when candle bodies close beyond prior swing peaks in trend direction."
      ),
      TradingTool(
        id = "apex",
        name = "Apex Extreme Levels",
        code = "APEX",
        category = ToolCategory.LIQUIDITY_IMBALANCE,
        summary = "Unmitigated extreme macro swing highs and lows harboring dense clusters of resting institutional buy/sell stop liquidity.",
        algorithmDetails = "Computes unmitigated fractal extremities across multi-day periods. Projects horizontal boundary bands representing pool liquidity. Triggers alerts when price wicks breach levels without body settlement.",
        isSelected = false,
        isAlertActive = true,
        lookbackPeriod = 5,
        sensitivityScore = 0.84,
        winRateEstimate = 74.3,
        detectedEventsCount = 8,
        statusBadge = "LIQUIDITY POOL",
        engineTooltipExplanation = "Computes unmitigated macro price extremities. Projects resting buy-side (BSL) and sell-side (SSL) stop liquidity bands."
      ),
      TradingTool(
        id = "fvg",
        name = "Fair Value Gap",
        code = "FVG",
        category = ToolCategory.LIQUIDITY_IMBALANCE,
        summary = "Three-candle price delivery imbalances revealing institutional inefficiency and expected 50% mean mitigation retests.",
        algorithmDetails = "Evaluates 3-candle windows where Low(n) > High(n-2) for bullish imbalances or High(n) < Low(n-2) for bearish imbalances. Automatically maps 0.5 consequence encroachment level.",
        isSelected = false,
        isAlertActive = false,
        lookbackPeriod = 3,
        sensitivityScore = 0.79,
        winRateEstimate = 71.8,
        detectedEventsCount = 15,
        statusBadge = "IMBALANCE",
        engineTooltipExplanation = "Evaluates 3-candle imbalance windows where Low(n) > High(n-2) or High(n) < Low(n-2) to detect institutional fair value gaps."
      ),
      TradingTool(
        id = "order_block",
        name = "Institutional Order Block",
        code = "OB",
        category = ToolCategory.INSTITUTIONAL,
        summary = "Last contrarian candle preceding violent structural displacement and market structure shift.",
        algorithmDetails = "Correlates high-volume impulsive displacement candles with preceding opposite-color candle bodies. Maintains valid zone until 100% volume mitigation occurs.",
        isSelected = false,
        isAlertActive = false,
        lookbackPeriod = 4,
        sensitivityScore = 0.86,
        winRateEstimate = 76.2,
        detectedEventsCount = 11,
        statusBadge = "SMART MONEY",
        engineTooltipExplanation = "Detects the last opposing candle body prior to an impulsive displacement move that breached market structure."
      ),
      TradingTool(
        id = "liq_sweep",
        name = "Liquidity Pool Sweeps",
        code = "LIQ",
        category = ToolCategory.LIQUIDITY_IMBALANCE,
        summary = "Equal Highs (EQH) and Equal Lows (EQL) engineered to lure retail stops before decisive reversal moves.",
        algorithmDetails = "Calculates delta variance across double/triple tops within 0.05% price tolerance. Emits instantaneous alerts on rejection wick sweep patterns.",
        isSelected = false,
        isAlertActive = true,
        lookbackPeriod = 4,
        sensitivityScore = 0.81,
        winRateEstimate = 73.0,
        detectedEventsCount = 9,
        statusBadge = "STOP HUNT",
        engineTooltipExplanation = "Identifies Equal Highs (EQH) and Equal Lows (EQL) within 0.05% price delta tolerance subject to stop-sweep liquidity runs."
      )
    )

    _uiState.update { it.copy(tools = initialTools) }
  }

  private fun loadMarketData() {
    viewModelScope.launch {
      val candles = MarketStructureEngine.generateMarketCycleDataset(94250.0)
      val swings = MarketStructureEngine.calculateSwingPoints(candles, lookback = 2)
      val mssEvents = MarketStructureEngine.detectMarketStructureShifts(candles, swings)
      val bosEvents = MarketStructureEngine.detectBreakOfStructure(candles, swings)
      val apexEvents = MarketStructureEngine.detectApexLevels(candles, swings)
      val fvgEvents = MarketStructureEngine.detectFairValueGaps(candles)
      val obEvents = MarketStructureEngine.detectOrderBlocks(candles, mssEvents + bosEvents)
      val liqEvents = MarketStructureEngine.detectLiquiditySweeps(candles, swings)

      val allEvents = (mssEvents + bosEvents + apexEvents + fvgEvents + obEvents + liqEvents).sortedBy { it.triggerIndex }

      _uiState.update { state ->
        val updatedTools = state.tools.map { tool ->
          val count = when (tool.id) {
            "mss" -> mssEvents.size
            "bos" -> bosEvents.size
            "apex" -> apexEvents.size
            "fvg" -> fvgEvents.size
            "order_block" -> obEvents.size
            "liq_sweep" -> liqEvents.size
            else -> tool.detectedEventsCount
          }
          tool.copy(detectedEventsCount = count)
        }
        state.copy(
          candles = candles,
          swings = swings,
          events = allEvents,
          tools = updatedTools,
          currentPrice = candles.lastOrNull()?.close ?: 94820.50
        )
      }
    }
  }

  fun toggleToolSelection(toolId: String) {
    _uiState.update { state ->
      val updatedTools = state.tools.map { tool ->
        if (tool.id == toolId) {
          tool.copy(isSelected = !tool.isSelected)
        } else tool
      }
      val updatedInspecting = if (state.inspectingTool?.id == toolId) {
        state.inspectingTool.copy(isSelected = !state.inspectingTool.isSelected)
      } else state.inspectingTool
      state.copy(tools = updatedTools, selectedToolId = toolId, inspectingTool = updatedInspecting)
    }
  }

  fun toggleToolFavorite(toolId: String) {
    _uiState.update { state ->
      val updatedTools = state.tools.map { tool ->
        if (tool.id == toolId) {
          tool.copy(isFavorite = !tool.isFavorite)
        } else tool
      }
      val updatedInspecting = if (state.inspectingTool?.id == toolId) {
        state.inspectingTool.copy(isFavorite = !state.inspectingTool.isFavorite)
      } else state.inspectingTool
      state.copy(tools = updatedTools, inspectingTool = updatedInspecting)
    }
  }

  fun toggleToolAlert(toolId: String) {
    _uiState.update { state ->
      val updatedTools = state.tools.map { tool ->
        if (tool.id == toolId) {
          tool.copy(isAlertActive = !tool.isAlertActive)
        } else tool
      }
      val updatedInspecting = if (state.inspectingTool?.id == toolId) {
        state.inspectingTool.copy(isAlertActive = !state.inspectingTool.isAlertActive)
      } else state.inspectingTool
      state.copy(tools = updatedTools, inspectingTool = updatedInspecting)
    }
  }

  fun selectCategory(category: ToolCategory) {
    _uiState.update { it.copy(selectedCategory = category) }
  }

  fun updateSearchQuery(query: String) {
    _uiState.update { it.copy(searchQuery = query) }
  }

  fun openToolInspection(tool: TradingTool?) {
    _uiState.update { it.copy(inspectingTool = tool) }
  }

  fun selectCandle(candle: Candle?) {
    _uiState.update { it.copy(selectedCandle = candle) }
  }

  fun switchTicker(ticker: String) {
    val base = when (ticker) {
      "ETH/USDT" -> 3420.0
      "NQ100" -> 19850.0
      else -> 94250.0
    }
    val candles = MarketStructureEngine.generateMarketCycleDataset(base)
    val swings = MarketStructureEngine.calculateSwingPoints(candles, lookback = 2)
    val mssEvents = MarketStructureEngine.detectMarketStructureShifts(candles, swings)
    val bosEvents = MarketStructureEngine.detectBreakOfStructure(candles, swings)
    val apexEvents = MarketStructureEngine.detectApexLevels(candles, swings)
    val fvgEvents = MarketStructureEngine.detectFairValueGaps(candles)
    val obEvents = MarketStructureEngine.detectOrderBlocks(candles, mssEvents + bosEvents)
    val liqEvents = MarketStructureEngine.detectLiquiditySweeps(candles, swings)

    val allEvents = (mssEvents + bosEvents + apexEvents + fvgEvents + obEvents + liqEvents).sortedBy { ev -> ev.triggerIndex }

    _uiState.update { state ->
      val updatedTools = state.tools.map { tool ->
        val count = when (tool.id) {
          "mss" -> mssEvents.size
          "bos" -> bosEvents.size
          "apex" -> apexEvents.size
          "fvg" -> fvgEvents.size
          "order_block" -> obEvents.size
          "liq_sweep" -> liqEvents.size
          else -> tool.detectedEventsCount
        }
        tool.copy(detectedEventsCount = count)
      }
      state.copy(
        activeTicker = ticker,
        candles = candles,
        swings = swings,
        events = allEvents,
        tools = updatedTools,
        currentPrice = candles.lastOrNull()?.close ?: base
      )
    }
  }

  fun navigateTo(screen: AppScreen) {
    if (_uiState.value.generatedCode.isEmpty()) {
      refreshGeneratedCode()
    }
    _uiState.update { it.copy(currentScreen = screen) }
  }

  fun selectCodeType(type: CodeType) {
    _uiState.update { state ->
      val newCode = CodeGeneratorEngine.generateCode(
        codeType = type,
        framework = state.selectedFramework,
        pineVersion = state.pineScriptVersion,
        includeMSS = state.includeMSSInExport,
        includeBOS = state.includeBOSInExport,
        includeApex = state.includeApexInExport
      )
      state.copy(selectedCodeType = type, generatedCode = newCode)
    }
  }

  fun selectFramework(framework: CustomFramework) {
    _uiState.update { state ->
      val newCode = CodeGeneratorEngine.generateCode(
        codeType = state.selectedCodeType,
        framework = framework,
        pineVersion = state.pineScriptVersion,
        includeMSS = state.includeMSSInExport,
        includeBOS = state.includeBOSInExport,
        includeApex = state.includeApexInExport
      )
      state.copy(selectedFramework = framework, generatedCode = newCode)
    }
  }

  fun selectPineScriptVersion(version: PineScriptVersion) {
    _uiState.update { state ->
      val newCode = CodeGeneratorEngine.generateCode(
        codeType = state.selectedCodeType,
        framework = state.selectedFramework,
        pineVersion = version,
        includeMSS = state.includeMSSInExport,
        includeBOS = state.includeBOSInExport,
        includeApex = state.includeApexInExport
      )
      state.copy(pineScriptVersion = version, generatedCode = newCode)
    }
  }

  fun toggleExportMSS() {
    _uiState.update { state ->
      val updated = !state.includeMSSInExport
      val newCode = CodeGeneratorEngine.generateCode(
        codeType = state.selectedCodeType,
        framework = state.selectedFramework,
        pineVersion = state.pineScriptVersion,
        includeMSS = updated,
        includeBOS = state.includeBOSInExport,
        includeApex = state.includeApexInExport
      )
      state.copy(includeMSSInExport = updated, generatedCode = newCode)
    }
  }

  fun toggleExportBOS() {
    _uiState.update { state ->
      val updated = !state.includeBOSInExport
      val newCode = CodeGeneratorEngine.generateCode(
        codeType = state.selectedCodeType,
        framework = state.selectedFramework,
        pineVersion = state.pineScriptVersion,
        includeMSS = state.includeMSSInExport,
        includeBOS = updated,
        includeApex = state.includeApexInExport
      )
      state.copy(includeBOSInExport = updated, generatedCode = newCode)
    }
  }

  fun toggleExportApex() {
    _uiState.update { state ->
      val updated = !state.includeApexInExport
      val newCode = CodeGeneratorEngine.generateCode(
        codeType = state.selectedCodeType,
        framework = state.selectedFramework,
        pineVersion = state.pineScriptVersion,
        includeMSS = state.includeMSSInExport,
        includeBOS = state.includeBOSInExport,
        includeApex = updated
      )
      state.copy(includeApexInExport = updated, generatedCode = newCode)
    }
  }

  private fun refreshGeneratedCode() {
    _uiState.update { state ->
      val code = CodeGeneratorEngine.generateCode(
        codeType = state.selectedCodeType,
        framework = state.selectedFramework,
        pineVersion = state.pineScriptVersion,
        includeMSS = state.includeMSSInExport,
        includeBOS = state.includeBOSInExport,
        includeApex = state.includeApexInExport
      )
      state.copy(generatedCode = code)
    }
  }

  fun exportFavorites(context: Context) {
    val favorites = _uiState.value.tools.filter { it.isFavorite }
    val toolsToExport = if (favorites.isNotEmpty()) favorites else _uiState.value.tools.filter { it.isSelected }

    val jsonString = buildFavoritesJson(toolsToExport)
    val fileName = "notify_favorites_config.json"
    val file = File(context.filesDir, fileName)
    file.writeText(jsonString)

    val sizeKb = "%.1f KB".format(file.length() / 1024.0)

    _uiState.update {
      it.copy(
        exportDialogState = ExportDialogState(
          fileName = fileName,
          filePath = file.absolutePath,
          jsonContent = jsonString,
          favoriteCount = toolsToExport.size,
          fileSizeFormatted = sizeKb
        )
      )
    }
  }

  fun dismissExportDialog() {
    _uiState.update { it.copy(exportDialogState = null) }
  }

  private fun buildFavoritesJson(tools: List<TradingTool>): String {
    val sb = StringBuilder()
    sb.append("{\n")
    sb.append("  \"app\": \"Notify\",\n")
    sb.append("  \"version\": \"1.0\",\n")
    sb.append("  \"exportedAtTimestamp\": ${System.currentTimeMillis()},\n")
    sb.append("  \"toolCount\": ${tools.size},\n")
    sb.append("  \"tools\": [\n")
    tools.forEachIndexed { index, tool ->
      sb.append("    {\n")
      sb.append("      \"id\": \"${tool.id}\",\n")
      sb.append("      \"name\": \"${tool.name}\",\n")
      sb.append("      \"code\": \"${tool.code}\",\n")
      sb.append("      \"category\": \"${tool.category.name}\",\n")
      sb.append("      \"isSelected\": ${tool.isSelected},\n")
      sb.append("      \"isFavorite\": ${tool.isFavorite},\n")
      sb.append("      \"isAlertActive\": ${tool.isAlertActive},\n")
      sb.append("      \"lookbackPeriod\": ${tool.lookbackPeriod},\n")
      sb.append("      \"sensitivityScore\": ${tool.sensitivityScore},\n")
      sb.append("      \"winRateEstimate\": ${tool.winRateEstimate},\n")
      sb.append("      \"detectedEventsCount\": ${tool.detectedEventsCount},\n")
      sb.append("      \"statusBadge\": \"${tool.statusBadge}\",\n")
      sb.append("      \"engineTooltipExplanation\": \"${tool.engineTooltipExplanation.replace("\"", "\\\"")}\"\n")
      sb.append("    }${if (index < tools.size - 1) "," else ""}\n")
    }
    sb.append("  ]\n")
    sb.append("}")
    return sb.toString()
  }
}
