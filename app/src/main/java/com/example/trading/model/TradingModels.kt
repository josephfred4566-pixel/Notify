package com.example.trading.model

enum class SwingType {
  HIGH,
  LOW
}

enum class StructureType(
  val displayName: String,
  val abbreviation: String,
  val isBullish: Boolean
) {
  MSS_BULLISH("Market Structure Shift (Bullish)", "MSS", true),
  MSS_BEARISH("Market Structure Shift (Bearish)", "MSS", false),
  BOS_BULLISH("Break of Structure (Bullish)", "BOS", true),
  BOS_BEARISH("Break of Structure (Bearish)", "BOS", false),
  APEX_HIGH("Apex Resistance Level", "APEX-H", false),
  APEX_LOW("Apex Support Level", "APEX-L", true),
  FVG_BULLISH("Fair Value Gap (Bullish)", "FVG", true),
  FVG_BEARISH("Fair Value Gap (Bearish)", "FVG", false),
  ORDER_BLOCK_BULLISH("Bullish Order Block", "OB+", true),
  ORDER_BLOCK_BEARISH("Bearish Order Block", "OB-", false),
  LIQ_SWEEP_HIGH("Buy-Side Liquidity Sweep (BSL)", "EQH", false),
  LIQ_SWEEP_LOW("Sell-Side Liquidity Sweep (SSL)", "EQL", true)
}

data class Candle(
  val timestamp: Long,
  val open: Double,
  val high: Double,
  val low: Double,
  val close: Double,
  val volume: Double
) {
  val isBullish: Boolean get() = close >= open
  val bodyTop: Double get() = maxOf(open, close)
  val bodyBottom: Double get() = minOf(open, close)
  val bodyRange: Double get() = bodyTop - bodyBottom
  val fullRange: Double get() = high - low
}

data class SwingPoint(
  val index: Int,
  val price: Double,
  val type: SwingType,
  val candle: Candle,
  val isApex: Boolean = false
)

data class StructureEvent(
  val type: StructureType,
  val triggerIndex: Int,
  val referenceIndex: Int,
  val triggerPrice: Double,
  val referencePrice: Double,
  val description: String,
  val displacementRatio: Double = 1.0,
  val isMitigated: Boolean = false,
  val secondaryPrice: Double = 0.0
)

enum class ToolCategory(val title: String) {
  ALL("All Tools"),
  STRUCTURE("Market Structure"),
  LIQUIDITY_IMBALANCE("Liquidity & Flow"),
  INSTITUTIONAL("Institutional Footprint")
}

data class TradingTool(
  val id: String,
  val name: String,
  val code: String,
  val category: ToolCategory,
  val summary: String,
  val algorithmDetails: String,
  val isSelected: Boolean = false,
  val isFavorite: Boolean = false,
  val isAlertActive: Boolean = true,
  val lookbackPeriod: Int = 3,
  val sensitivityScore: Double = 0.85,
  val winRateEstimate: Double = 73.4,
  val detectedEventsCount: Int = 14,
  val statusBadge: String = "OPTIMAL",
  val engineTooltipExplanation: String = ""
)
