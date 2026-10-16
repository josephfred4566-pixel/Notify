package com.example.trading.algorithm

import org.junit.Assert.assertTrue
import org.junit.Test

class MarketStructureEngineTest {

  @Test
  fun testAllToolsDetectSignalsIndependently() {
    val candles = MarketStructureEngine.generateMarketCycleDataset(94250.0)
    assertTrue("Candles dataset should have sufficient data", candles.size >= 40)

    val swings = MarketStructureEngine.calculateSwingPoints(candles, lookback = 2)
    println("Swings count: ${swings.size}")
    assertTrue("Should detect multiple swing points", swings.size >= 4)

    // 1. MSS (Market Structure Shift)
    val mssEvents = MarketStructureEngine.detectMarketStructureShifts(candles, swings)
    println("MSS events count: ${mssEvents.size}")
    for (e in mssEvents) println("  MSS: ${e.description}")
    assertTrue("Should detect at least one MSS event", mssEvents.isNotEmpty())

    // 2. BOS (Break of Structure)
    val bosEvents = MarketStructureEngine.detectBreakOfStructure(candles, swings)
    println("BOS events count: ${bosEvents.size}")
    for (e in bosEvents) println("  BOS: ${e.description}")
    assertTrue("Should detect at least one BOS event", bosEvents.isNotEmpty())

    // 3. APEX (Apex Extremes)
    val apexEvents = MarketStructureEngine.detectApexLevels(candles, swings)
    println("Apex events count: ${apexEvents.size}")
    for (e in apexEvents) println("  APEX: ${e.description}")
    assertTrue("Should detect apex high and low", apexEvents.size >= 2)

    // 4. FVG (Fair Value Gaps)
    val fvgEvents = MarketStructureEngine.detectFairValueGaps(candles)
    println("FVG events count: ${fvgEvents.size}")
    for (e in fvgEvents) println("  FVG: ${e.description}")
    assertTrue("Should detect FVG imbalances", fvgEvents.isNotEmpty())

    // 5. Order Blocks (OB)
    val obEvents = MarketStructureEngine.detectOrderBlocks(candles, mssEvents + bosEvents)
    println("OB events count: ${obEvents.size}")
    for (e in obEvents) println("  OB: ${e.description}")
    assertTrue("Should detect Order Blocks", obEvents.isNotEmpty())

    // 6. Liquidity Sweeps
    val liqEvents = MarketStructureEngine.detectLiquiditySweeps(candles, swings)
    println("Liquidity Sweep events count: ${liqEvents.size}")
    for (e in liqEvents) println("  LIQ: ${e.description}")
    assertTrue("Should detect Liquidity Sweeps", liqEvents.isNotEmpty())
  }
}
