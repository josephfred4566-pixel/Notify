package com.example.trading.codegen

enum class AppScreen {
  DASHBOARD,
  CODE_STUDIO
}

enum class CodeType(
  val displayName: String,
  val shortLabel: String,
  val description: String
) {
  CUSTOM_INDICATOR(
    displayName = "Custom Indicator",
    shortLabel = "Indicator",
    description = "Visualizes Market Structure Shifts (MSS), Break of Structure (BOS), and Apex Liquidity on charts with execution alerts."
  ),
  CUSTOM_STRATEGY(
    displayName = "Custom Strategy",
    shortLabel = "Strategy",
    description = "Automated backtesting model with orderflow entry triggers, stop-loss protection at apex swing levels, and risk parameters."
  )
}

enum class PineScriptVersion(
  val versionNumber: Int,
  val displayName: String,
  val shortTag: String,
  val versionHeader: String
) {
  V5(
    versionNumber = 5,
    displayName = "Version 5",
    shortTag = "v5",
    versionHeader = "//@version=5"
  ),
  V6(
    versionNumber = 6,
    displayName = "Version 6",
    shortTag = "v6",
    versionHeader = "//@version=6"
  )
}

enum class CustomFramework(
  val displayName: String,
  val ideTarget: String,
  val fileExtension: String,
  val syntaxTag: String
) {
  PINE_SCRIPT(
    displayName = "PineScript",
    ideTarget = "TradingView (PineScript)",
    fileExtension = ".pine",
    syntaxTag = "PineScript"
  ),
  META_EDITOR(
    displayName = "MetaEditor",
    ideTarget = "MetaTrader 5 (MQL5)",
    fileExtension = ".mq5",
    syntaxTag = "MQL5"
  )
}
