package com.example.trading.codegen

object CodeGeneratorEngine {

  fun generateCode(
    codeType: CodeType,
    framework: CustomFramework,
    pineVersion: PineScriptVersion = PineScriptVersion.V5,
    includeMSS: Boolean = true,
    includeBOS: Boolean = true,
    includeApex: Boolean = true,
    lookbackPeriod: Int = 3
  ): String {
    return when (framework) {
      CustomFramework.PINE_SCRIPT -> generatePineScript(codeType, pineVersion, includeMSS, includeBOS, includeApex, lookbackPeriod)
      CustomFramework.META_EDITOR -> generateMetaEditor(codeType, includeMSS, includeBOS, includeApex, lookbackPeriod)
    }
  }

  private fun generatePineScript(
    codeType: CodeType,
    pineVersion: PineScriptVersion,
    includeMSS: Boolean,
    includeBOS: Boolean,
    includeApex: Boolean,
    lookback: Int
  ): String {
    val isStrategy = codeType == CodeType.CUSTOM_STRATEGY

    return if (!isStrategy) {
      """
${pineVersion.versionHeader}
indicator("Notify - Precision Market Structure Suite", "Notify MSS & BOS", overlay=true)

// ==========================================
// 1. CONFIGURATION & ENGINE INPUTS
// ==========================================
lookback       = input.int($lookback, "Fractal Swing Lookback (k)", minval=2, maxval=10)
useDisplace    = input.bool(true, "Require Displacement Filter (>1.05x ATR)")
showMSS        = input.bool($includeMSS, "Plot Market Structure Shifts (MSS / ChoCh)")
showBOS        = input.bool($includeBOS, "Plot Break of Structure (BOS)")
showApex       = input.bool($includeApex, "Plot Apex Liquidity Bands")
colBullMSS     = input.color(#00F2FE, "Bullish MSS Color")
colBearMSS     = input.color(#FF3366, "Bearish MSS Color")
colBOS         = input.color(#00FF87, "BOS Trend Color")
colApex        = input.color(#FFB800, "Apex Liquidity Color")

// ==========================================
// 2. MATHEMATICAL FRACTAL SWING DETECTION
// ==========================================
swingHigh = ta.pivothigh(high, lookback, lookback)
swingLow  = ta.pivotlow(low, lookback, lookback)

var float lastHighPrice = na
var int   lastHighBar   = na
var float lastLowPrice  = na
var int   lastLowBar    = na

if not na(swingHigh)
    lastHighPrice := swingHigh
    lastHighBar   := bar_index[lookback]

if not na(swingLow)
    lastLowPrice  := swingLow
    lastLowBar    := bar_index[lookback]

// Displacement calculation
bodySize = math.abs(close - open)
atrRef   = ta.atr(14)
isDisplaced = not useDisplace or (bodySize > 1.05 * (atrRef / 2.0))

// ==========================================
// 3. MARKET STRUCTURE SHIFT (MSS / ChoCh)
// ==========================================
bullMSS = showMSS and not na(lastHighPrice) and close > lastHighPrice and close[1] <= lastHighPrice and isDisplaced
bearMSS = showMSS and not na(lastLowPrice) and close < lastLowPrice and close[1] >= lastLowPrice and isDisplaced

plotshape(bullMSS, title="Bullish MSS", style=shape.triangleup, location=location.belowbar, color=colBullMSS, size=size.small, text="MSS")
plotshape(bearMSS, title="Bearish MSS", style=shape.triangledown, location=location.abovebar, color=colBearMSS, size=size.small, text="MSS")

// Draw structural shift level line
if bullMSS
    line.new(lastHighBar, lastHighPrice, bar_index, lastHighPrice, color=colBullMSS, width=2, style=line.style_solid)

if bearMSS
    line.new(lastLowBar, lastLowPrice, bar_index, lastLowPrice, color=colBearMSS, width=2, style=line.style_solid)

// ==========================================
// 4. BREAK OF STRUCTURE (BOS) CONTINUATION
// ==========================================
bullBOS = showBOS and not na(lastHighPrice) and close > lastHighPrice and high[1] <= lastHighPrice
bearBOS = showBOS and not na(lastLowPrice) and close < lastLowPrice and low[1] >= lastLowPrice

plotshape(bullBOS and not bullMSS, title="Bullish BOS", style=shape.circle, location=location.belowbar, color=colBOS, size=size.tiny, text="BOS")
plotshape(bearBOS and not bearMSS, title="Bearish BOS", style=shape.circle, location=location.abovebar, color=colBOS, size=size.tiny, text="BOS")

// ==========================================
// 5. APEX LIQUIDITY EXTREMES
// ==========================================
var float apexHigh = na
var float apexLow  = na

if showApex and not na(swingHigh)
    if na(apexHigh) or swingHigh > apexHigh
        apexHigh := swingHigh

if showApex and not na(swingLow)
    if na(apexLow) or swingLow < apexLow
        apexLow := swingLow

plot(showApex ? apexHigh : na, "Apex High Liquidity", color=colApex, style=plot.style_circles, linewidth=2)
plot(showApex ? apexLow  : na, "Apex Low Liquidity",  color=colApex, style=plot.style_circles, linewidth=2)

// ==========================================
// 6. NOTIFY ALERT CONDITIONS
// ==========================================
alertcondition(bullMSS, title="Notify: Bullish MSS Confirmed", message="Bullish Market Structure Shift confirmed at {{close}}")
alertcondition(bearMSS, title="Notify: Bearish MSS Confirmed", message="Bearish Market Structure Shift confirmed at {{close}}")
alertcondition(bullBOS, title="Notify: Bullish BOS Continuation", message="Bullish BOS expansion confirmed at {{close}}")
alertcondition(bearBOS, title="Notify: Bearish BOS Continuation", message="Bearish BOS expansion confirmed at {{close}}")
      """.trimIndent()
    } else {
      """
${pineVersion.versionHeader}
strategy("Notify - Market Structure Strategy", "Notify MSS Strat", overlay=true, initial_capital=10000, default_qty_type=strategy.percent_of_equity, default_qty_value=25, commission_type=strategy.commission.percent, commission_value=0.04)

// ==========================================
// 1. ENGINE PARAMETERS
// ==========================================
lookback    = input.int($lookback, "Fractal Lookback (k)", minval=2, maxval=10)
riskReward  = input.float(2.5, "Risk-to-Reward Ratio (R:R)", minval=1.0)
useDisplace = input.bool(true, "Displacement Confirmation")

// ==========================================
// 2. FRACTAL DETECTIONS & SIGNALS
// ==========================================
swingHigh = ta.pivothigh(high, lookback, lookback)
swingLow  = ta.pivotlow(low, lookback, lookback)

var float lastHigh = na
var float lastLow  = na

if not na(swingHigh)
    lastHigh := swingHigh
if not na(swingLow)
    lastLow  := swingLow

bodyRange = math.abs(close - open)
atr14     = ta.atr(14)
isDisplaced = not useDisplace or (bodyRange > 1.05 * (atr14 / 2.0))

// Shift Triggers
bullMSS = not na(lastHigh) and close > lastHigh and close[1] <= lastHigh and isDisplaced
bearMSS = not na(lastLow) and close < lastLow and close[1] >= lastLow and isDisplaced

// ==========================================
// 3. EXECUTION ORDERS & RISK MANAGEMENT
// ==========================================
var float stopLossPrice = na
var float takeProfitPrice = na

if (bullMSS and strategy.position_size <= 0)
    stopLossPrice := not na(lastLow) ? lastLow : low - (atr14 * 1.5)
    riskDistance = close - stopLossPrice
    takeProfitPrice := close + (riskDistance * riskReward)
    strategy.entry("Long MSS", strategy.long)
    strategy.exit("TP/SL Long", "Long MSS", stop=stopLossPrice, limit=takeProfitPrice)

if (bearMSS and strategy.position_size >= 0)
    stopLossPrice := not na(lastHigh) ? lastHigh : high + (atr14 * 1.5)
    riskDistance = stopLossPrice - close
    takeProfitPrice := close - (riskDistance * riskReward)
    strategy.entry("Short MSS", strategy.short)
    strategy.exit("TP/SL Short", "Short MSS", stop=stopLossPrice, limit=takeProfitPrice)

// Trailing Apex Plotting
plot(strategy.position_size != 0 ? stopLossPrice : na, "Active Stop Loss", color=color.red, style=plot.style_linebr)
plot(strategy.position_size != 0 ? takeProfitPrice : na, "Active Take Profit", color=color.green, style=plot.style_linebr)
      """.trimIndent()
    }
  }

  private fun generateMetaEditor(
    codeType: CodeType,
    includeMSS: Boolean,
    includeBOS: Boolean,
    includeApex: Boolean,
    lookback: Int
  ): String {
    val isStrategy = codeType == CodeType.CUSTOM_STRATEGY

    return if (!isStrategy) {
      """
//+------------------------------------------------------------------+
//|                                     Notify_Market_Structure.mq5   |
//|                        Copyright 2026, Notify Algorithmic Suite  |
//|                                       https://notify-trader.ai   |
//+------------------------------------------------------------------+
#property copyright   "Notify Algorithmic Suite"
#property link        "https://notify-trader.ai"
#property version     "1.00"
#property indicator_chart_window
#property indicator_buffers 4
#property indicator_plots   2

//--- Plot definitions
#property indicator_label1  "Bullish MSS"
#property indicator_type1   DRAW_ARROW
#property indicator_color1  clrCyan
#property indicator_width1  2

#property indicator_label2  "Bearish MSS"
#property indicator_type2   DRAW_ARROW
#property indicator_color2  clrCrimson
#property indicator_width2  2

//--- Input parameters
input int      InpLookback     = $lookback;     // Fractal Lookback Window (k)
input bool     InpUseDisplace  = true;   // Require Displacement Body
input bool     InpEnableAlerts = true;   // Push Notification Alerts

//--- Indicator buffers
double BufferBullMSS[];
double BufferBearMSS[];
double BufferSwingHigh[];
double BufferSwingLow[];

//+------------------------------------------------------------------+
//| Custom indicator initialization function                         |
//+------------------------------------------------------------------+
int OnInit()
  {
   SetIndexBuffer(0, BufferBullMSS, INDICATOR_DATA);
   SetIndexBuffer(1, BufferBearMSS, INDICATOR_DATA);
   SetIndexBuffer(2, BufferSwingHigh, INDICATOR_CALCULATIONS);
   SetIndexBuffer(3, BufferSwingLow, INDICATOR_CALCULATIONS);

   PlotIndexSetInteger(0, PLOT_ARROW, 233); // Arrow up
   PlotIndexSetInteger(1, PLOT_ARROW, 234); // Arrow down

   IndicatorSetString(INDICATOR_SHORTNAME, "Notify MSS Engine (k=" + IntegerToString(InpLookback) + ")");
   return(INIT_SUCCEEDED);
  }

//+------------------------------------------------------------------+
//| Custom indicator iteration function                              |
//+------------------------------------------------------------------+
int OnCalculate(const int rates_total,
                const int prev_calculated,
                const datetime &time[],
                const double &open[],
                const double &high[],
                const double &low[],
                const double &close[],
                const long &tick_volume[],
                const long &volume[],
                const int &spread[])
  {
   if(rates_total < (InpLookback * 2 + 5))
      return(0);

   int start = prev_calculated - 1;
   if(start < InpLookback)
      start = InpLookback;

   for(int i = start; i < rates_total - InpLookback; i++)
     {
      BufferBullMSS[i] = EMPTY_VALUE;
      BufferBearMSS[i] = EMPTY_VALUE;

      // Check Swing High
      bool isHigh = true;
      for(int j = 1; j <= InpLookback; j++)
        {
         if(high[i] <= high[i - j] || high[i] < high[i + j])
           {
            isHigh = false;
            break;
           }
        }
      if(isHigh)
         BufferSwingHigh[i] = high[i];

      // Check Swing Low
      bool isLow = true;
      for(int j = 1; j <= InpLookback; j++)
        {
         if(low[i] >= low[i - j] || low[i] > low[i + j])
           {
            isLow = false;
            break;
           }
        }
      if(isLow)
         BufferSwingLow[i] = low[i];

      // Find most recent swing points
      double recentHigh = 0;
      double recentLow  = 0;
      for(int k = i - 1; k >= InpLookback; k--)
        {
         if(recentHigh == 0 && BufferSwingHigh[k] != EMPTY_VALUE && BufferSwingHigh[k] > 0)
            recentHigh = BufferSwingHigh[k];
         if(recentLow == 0 && BufferSwingLow[k] != EMPTY_VALUE && BufferSwingLow[k] > 0)
            recentLow = BufferSwingLow[k];
         if(recentHigh > 0 && recentLow > 0)
            break;
        }

      // Check Market Structure Shift Break
      if(recentHigh > 0 && close[i] > recentHigh && close[i - 1] <= recentHigh)
        {
         BufferBullMSS[i] = low[i] - (high[i] - low[i]) * 0.3;
         if(InpEnableAlerts && i == rates_total - InpLookback - 1)
            Alert("Notify: Bullish MSS Confirmed on " + _Symbol + " at " + DoubleToString(close[i], _Digits));
        }

      if(recentLow > 0 && close[i] < recentLow && close[i - 1] >= recentLow)
        {
         BufferBearMSS[i] = high[i] + (high[i] - low[i]) * 0.3;
         if(InpEnableAlerts && i == rates_total - InpLookback - 1)
            Alert("Notify: Bearish MSS Confirmed on " + _Symbol + " at " + DoubleToString(close[i], _Digits));
        }
     }

   return(rates_total);
  }
      """.trimIndent()
    } else {
      """
//+------------------------------------------------------------------+
//|                                      Notify_MSS_ExpertAdvisor.mq5 |
//|                        Copyright 2026, Notify Algorithmic Suite  |
//|                                       https://notify-trader.ai   |
//+------------------------------------------------------------------+
#property copyright   "Notify Algorithmic Suite"
#property link        "https://notify-trader.ai"
#property version     "1.00"

#include <Trade\Trade.mqh>

//--- Inputs
input group "=== ENGINE PARAMETERS ==="
input int      InpLookback       = $lookback;     // Swing Lookback (k)
input double   InpRiskPercent    = 1.0;   // Account Equity Risk %
input double   InpRiskReward     = 2.5;   // Risk-to-Reward Ratio
input ulong    InpMagicNumber    = 948205; // EA Magic Number

CTrade trade;

//+------------------------------------------------------------------+
//| Expert initialization function                                   |
//+------------------------------------------------------------------+
int OnInit()
  {
   trade.SetExpertMagicNumber(InpMagicNumber);
   Print("Notify MetaEditor EA initialized successfully.");
   return(INIT_SUCCEEDED);
  }

//+------------------------------------------------------------------+
//| Expert tick function                                             |
//+------------------------------------------------------------------+
void OnTick()
  {
   // Only execute on new bar arrival
   static datetime lastBarTime = 0;
   datetime currentBarTime = iTime(_Symbol, _Period, 0);
   if(currentBarTime == lastBarTime)
      return;
   lastBarTime = currentBarTime;

   // Check open positions for this EA
   if(PositionsTotal() > 0)
      return;

   // Exact swing scans across bar arrays
   double highs[], lows[], closes[], opens[];
   ArraySetAsSeries(highs, true);
   ArraySetAsSeries(lows, true);
   ArraySetAsSeries(closes, true);
   ArraySetAsSeries(opens, true);

   int copied = CopyHigh(_Symbol, _Period, 1, 50, highs);
   CopyLow(_Symbol, _Period, 1, 50, lows);
   CopyClose(_Symbol, _Period, 1, 50, closes);
   CopyOpen(_Symbol, _Period, 1, 50, opens);

   if(copied < 30)
      return;

   // Find key swing high and low
   double swingHigh = 0;
   double swingLow  = 0;
   for(int i = 2; i < 25; i++)
     {
      if(swingHigh == 0 && highs[i] > highs[i-1] && highs[i] > highs[i+1] && highs[i] > highs[i-2] && highs[i] > highs[i+2])
         swingHigh = highs[i];
      if(swingLow == 0 && lows[i] < lows[i-1] && lows[i] < lows[i+1] && lows[i] < lows[i-2] && lows[i] < lows[i+2])
         swingLow = lows[i];
      if(swingHigh > 0 && swingLow > 0)
         break;
     }

   // MSS Bullish Execution Trigger
   if(swingHigh > 0 && closes[0] > swingHigh && closes[1] <= swingHigh)
     {
      double sl = (swingLow > 0) ? swingLow : (closes[0] - 150 * _Point);
      double risk = closes[0] - sl;
      double tp = closes[0] + (risk * InpRiskReward);
      double lots = CalculateLots(risk);

      trade.Buy(lots, _Symbol, closes[0], sl, tp, "Notify Bullish MSS Entry");
      Print("Bullish MSS Buy Executed at: ", closes[0], " SL: ", sl, " TP: ", tp);
     }

   // MSS Bearish Execution Trigger
   if(swingLow > 0 && closes[0] < swingLow && closes[1] >= swingLow)
     {
      double sl = (swingHigh > 0) ? swingHigh : (closes[0] + 150 * _Point);
      double risk = sl - closes[0];
      double tp = closes[0] - (risk * InpRiskReward);
      double lots = CalculateLots(risk);

      trade.Sell(lots, _Symbol, closes[0], sl, tp, "Notify Bearish MSS Entry");
      Print("Bearish MSS Sell Executed at: ", closes[0], " SL: ", sl, " TP: ", tp);
     }
  }

//--- Position sizing helper
double CalculateLots(double riskPoints)
  {
   double accountEquity = AccountInfoDouble(ACCOUNT_EQUITY);
   double riskAmount = accountEquity * (InpRiskPercent / 100.0);
   double tickValue = SymbolInfoDouble(_Symbol, SYMBOL_TRADE_TICK_VALUE);
   if(tickValue <= 0 || riskPoints <= 0) return 0.01;
   double lot = riskAmount / (riskPoints * tickValue);
   return NormalizeDouble(MathMax(0.01, MathMin(10.0, lot)), 2);
  }
      """.trimIndent()
    }
  }
}
