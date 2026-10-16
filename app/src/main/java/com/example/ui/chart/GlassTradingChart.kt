package com.example.ui.chart

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.trading.model.Candle
import com.example.trading.model.StructureEvent
import com.example.trading.model.StructureType
import com.example.trading.model.SwingPoint
import com.example.trading.model.SwingType
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassWhiteSubtle
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.TextDisabled
import com.example.ui.theme.TextHighContrast
import com.example.ui.theme.TextLowContrast
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance, mathematically precise Canvas candlestick chart renderer.
 * Accurately plots Candlesticks, Fractal Swings, MSS, BOS, Apex Pools,
 * Fair Value Gaps (FVG) with 50% CE, Institutional Order Blocks (OB), and Liquidity Sweeps.
 */
@Composable
fun GlassTradingChart(
  candles: List<Candle>,
  swings: List<SwingPoint>,
  events: List<StructureEvent>,
  modifier: Modifier = Modifier,
  showMSS: Boolean = true,
  showBOS: Boolean = true,
  showApex: Boolean = true,
  showFVG: Boolean = true,
  showOrderBlock: Boolean = true,
  showLiqSweep: Boolean = true,
  activeToolId: String? = null,
  onCandleSelected: ((Candle?) -> Unit)? = null
) {
  if (candles.isEmpty()) return

  var selectedIndex by remember { mutableStateOf<Int?>(null) }
  val textPaint = remember {
    Paint().apply {
      isAntiAlias = true
      textSize = 22f
      typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
    }
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(Color.White.copy(alpha = 0.04f))
      .border(1.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(18.dp))
  ) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp)
        .pointerInput(candles) {
          detectTapGestures(
            onTap = { offset ->
              val total = candles.size
              val slotW = size.width / total
              val idx = (offset.x / slotW).toInt().coerceIn(0, total - 1)
              selectedIndex = idx
              onCandleSelected?.invoke(candles[idx])
            }
          )
        }
        .pointerInput(candles) {
          detectDragGestures(
            onDragStart = { offset ->
              val total = candles.size
              val slotW = size.width / total
              val idx = (offset.x / slotW).toInt().coerceIn(0, total - 1)
              selectedIndex = idx
              onCandleSelected?.invoke(candles[idx])
            },
            onDrag = { change, _ ->
              val total = candles.size
              val slotW = size.width / total
              val idx = (change.position.x / slotW).toInt().coerceIn(0, total - 1)
              selectedIndex = idx
              onCandleSelected?.invoke(candles[idx])
            },
            onDragEnd = {
              // keep selection for inspection
            }
          )
        }
    ) {
      val w = size.width
      val h = size.height
      val n = candles.size

      val minPrice = candles.minOf { it.low }
      val maxPrice = candles.maxOf { it.high }
      val priceRange = max(1.0, maxPrice - minPrice)

      val volH = h * 0.18f
      val chartH = h - volH - 10f
      val maxVol = candles.maxOf { it.volume }.coerceAtLeast(1.0)

      fun priceToY(price: Double): Float {
        val norm = (price - minPrice) / priceRange
        return (chartH - (norm * chartH)).toFloat() + 5f
      }

      val candleSlotW = w / n
      val candleBodyW = max(3f, candleSlotW * 0.65f)

      // 1. Subtle horizontal price grid lines (all pure white alpha)
      val gridSteps = 4
      for (g in 0..gridSteps) {
        val y = chartH * (g.toFloat() / gridSteps)
        drawLine(
          color = Color.White.copy(alpha = 0.08f),
          start = Offset(0f, y),
          end = Offset(w, y),
          strokeWidth = 1f,
          pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        )
      }

      // 2. Draw Institutional Order Blocks (OB) under candlesticks
      if (showOrderBlock) {
        val obEvents = events.filter {
          it.type == StructureType.ORDER_BLOCK_BULLISH || it.type == StructureType.ORDER_BLOCK_BEARISH
        }
        for (ob in obEvents) {
          if (ob.referenceIndex in 0 until n) {
            val startX = ob.referenceIndex * candleSlotW + (candleSlotW / 2f)
            val endX = if (ob.isMitigated) {
              minOf(w, (ob.triggerIndex + 14) * candleSlotW)
            } else {
              w
            }
            val topPrice = maxOf(ob.triggerPrice, ob.referencePrice)
            val bottomPrice = minOf(ob.triggerPrice, ob.referencePrice)
            val topY = priceToY(topPrice)
            val bottomY = priceToY(bottomPrice)
            val zoneH = max(3f, bottomY - topY)

            val isBull = ob.type == StructureType.ORDER_BLOCK_BULLISH
            val baseColor = if (isBull) NeonViolet else NeonAmber
            val isFocused = activeToolId == "order_block"

            // Shaded institutional demand / supply zone
            drawRect(
              color = baseColor.copy(alpha = if (isFocused) 0.28f else 0.16f),
              topLeft = Offset(startX, topY),
              size = Size(max(12f, endX - startX), zoneH)
            )

            // Zone top and bottom boundaries
            drawLine(
              color = baseColor.copy(alpha = if (isFocused) 0.95f else 0.65f),
              start = Offset(startX, topY),
              end = Offset(endX, topY),
              strokeWidth = 1.4.dp.toPx()
            )
            drawLine(
              color = baseColor.copy(alpha = if (isFocused) 0.95f else 0.65f),
              start = Offset(startX, bottomY),
              end = Offset(endX, bottomY),
              strokeWidth = 1.4.dp.toPx()
            )

            // Mean threshold (50% midpoint) dashed line
            val midY = (topY + bottomY) / 2f
            drawLine(
              color = baseColor.copy(alpha = 0.55f),
              start = Offset(startX, midY),
              end = Offset(endX, midY),
              strokeWidth = 1f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
            )

            // Text Label
            textPaint.textSize = 19f
            textPaint.color = if (isBull) android.graphics.Color.parseColor("#C084FC") else android.graphics.Color.parseColor("#FBBF24")
            drawContext.canvas.nativeCanvas.drawText(
              if (isBull) "OB+ Demand" else "OB- Supply",
              startX + 6f,
              topY + 16f,
              textPaint
            )
            textPaint.textSize = 16f
            textPaint.color = android.graphics.Color.parseColor("#CBD5E1")
            drawContext.canvas.nativeCanvas.drawText("50% MT", startX + 6f, midY - 3f, textPaint)
          }
        }
      }

      // 3. Draw Fair Value Gaps (FVG) / 3-candle imbalance zones
      if (showFVG) {
        val fvgEvents = events.filter {
          it.type == StructureType.FVG_BULLISH || it.type == StructureType.FVG_BEARISH
        }
        for (fvg in fvgEvents) {
          if (fvg.referenceIndex in 0 until n) {
            val startX = fvg.referenceIndex * candleSlotW + (candleSlotW / 2f)
            val endX = if (fvg.isMitigated) {
              minOf(w, (fvg.triggerIndex + 12) * candleSlotW)
            } else {
              w
            }
            val topPrice = maxOf(fvg.triggerPrice, fvg.referencePrice)
            val bottomPrice = minOf(fvg.triggerPrice, fvg.referencePrice)
            val topY = priceToY(topPrice)
            val bottomY = priceToY(bottomPrice)
            val zoneH = max(2.5f, bottomY - topY)

            val isBull = fvg.type == StructureType.FVG_BULLISH
            val fvgColor = if (isBull) NeonCyan else NeonCoral
            val isFocused = activeToolId == "fvg"

            // Shaded imbalance zone
            drawRect(
              color = fvgColor.copy(alpha = if (isFocused) 0.26f else 0.15f),
              topLeft = Offset(startX, topY),
              size = Size(max(10f, endX - startX), zoneH)
            )

            drawLine(
              color = fvgColor.copy(alpha = 0.75f),
              start = Offset(startX, topY),
              end = Offset(endX, topY),
              strokeWidth = 1.2.dp.toPx()
            )
            drawLine(
              color = fvgColor.copy(alpha = 0.75f),
              start = Offset(startX, bottomY),
              end = Offset(endX, bottomY),
              strokeWidth = 1.2.dp.toPx()
            )

            // 50% Consequent Encroachment (CE) level
            val cePrice = fvg.secondaryPrice.takeIf { it > 0.0 } ?: ((topPrice + bottomPrice) / 2.0)
            val ceY = priceToY(cePrice)
            drawLine(
              color = NeonAmber.copy(alpha = 0.75f),
              start = Offset(startX, ceY),
              end = Offset(endX, ceY),
              strokeWidth = 1f,
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f), 0f)
            )

            // Text Label
            textPaint.textSize = 19f
            textPaint.color = if (isBull) android.graphics.Color.parseColor("#22D3EE") else android.graphics.Color.parseColor("#FB7185")
            drawContext.canvas.nativeCanvas.drawText(
              if (isBull) "+FVG" else "-FVG",
              startX + 6f,
              topY + 16f,
              textPaint
            )
            textPaint.textSize = 16f
            textPaint.color = android.graphics.Color.parseColor("#FBBF24")
            drawContext.canvas.nativeCanvas.drawText("50% CE", startX + 6f, ceY - 3f, textPaint)
          }
        }
      }

      // 4. Draw Apex Levels (Resting Buy-side BSL / Sell-side SSL liquidity pools)
      if (showApex) {
        val apexEvents = events.filter { it.type == StructureType.APEX_HIGH || it.type == StructureType.APEX_LOW }
        for (apex in apexEvents) {
          val apexY = priceToY(apex.triggerPrice)
          val isHigh = apex.type == StructureType.APEX_HIGH
          val apexColor = NeonAmber

          drawLine(
            color = apexColor.copy(alpha = 0.85f),
            start = Offset(0f, apexY),
            end = Offset(w, apexY),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
          )

          // Apex zone highlight band
          drawRect(
            color = apexColor.copy(alpha = 0.07f),
            topLeft = Offset(0f, if (isHigh) apexY - 14f else apexY),
            size = Size(w, 14f)
          )

          // Text Label
          textPaint.textSize = 19f
          textPaint.color = android.graphics.Color.parseColor("#FBBF24")
          drawContext.canvas.nativeCanvas.drawText(
            if (isHigh) "APEX-H (BSL Pool)" else "APEX-L (SSL Pool)",
            w - 200f,
            if (isHigh) apexY - 6f else apexY + 20f,
            textPaint
          )
        }
      }

      // 5. Draw Candlesticks & Volume with authentic NeonEmerald & NeonCoral colors
      for (i in 0 until n) {
        val c = candles[i]
        val centerX = i * candleSlotW + (candleSlotW / 2f)

        val openY = priceToY(c.open)
        val closeY = priceToY(c.close)
        val highY = priceToY(c.high)
        val lowY = priceToY(c.low)

        val isBull = c.close >= c.open
        val candleColor = if (isBull) NeonEmerald else NeonCoral
        val candleFill = if (isBull) NeonEmerald.copy(alpha = 0.85f) else NeonCoral.copy(alpha = 0.85f)

        // Wick
        drawLine(
          color = candleColor,
          start = Offset(centerX, highY),
          end = Offset(centerX, lowY),
          strokeWidth = 1.4.dp.toPx()
        )

        // Body
        val topY = min(openY, closeY)
        val bodyH = max(2.5f, kotlin.math.abs(closeY - openY))

        drawRect(
          color = candleFill,
          topLeft = Offset(centerX - (candleBodyW / 2f), topY),
          size = Size(candleBodyW, bodyH)
        )

        // Volume Bar
        val volNorm = (c.volume / maxVol).toFloat()
        val barH = max(2f, volNorm * volH)
        drawRect(
          color = if (isBull) NeonEmerald.copy(alpha = 0.25f) else NeonCoral.copy(alpha = 0.20f),
          topLeft = Offset(centerX - (candleBodyW / 2f), h - barH),
          size = Size(candleBodyW, barH)
        )
      }

      // 6. Draw Swing Points (Fractal Highs & Lows)
      for (sp in swings) {
        if (sp.index in 0 until n) {
          val centerX = sp.index * candleSlotW + (candleSlotW / 2f)
          val spY = priceToY(sp.price)
          val isHigh = sp.type == SwingType.HIGH

          drawCircle(
            color = if (isHigh) NeonAmber.copy(alpha = 0.5f) else NeonCyan.copy(alpha = 0.5f),
            radius = 3.5.dp.toPx(),
            center = Offset(centerX, spY)
          )
          drawCircle(
            color = Color.White,
            radius = 1.8.dp.toPx(),
            center = Offset(centerX, spY)
          )
        }
      }

      // 7. Draw Market Structure Shifts (MSS) Break Geometry
      if (showMSS) {
        val mssEvents = events.filter { it.type == StructureType.MSS_BULLISH || it.type == StructureType.MSS_BEARISH }
        for (mss in mssEvents) {
          if (mss.referenceIndex in 0 until n && mss.triggerIndex in 0 until n) {
            val startX = mss.referenceIndex * candleSlotW + (candleSlotW / 2f)
            val endX = mss.triggerIndex * candleSlotW + (candleSlotW / 2f)
            val levelY = priceToY(mss.referencePrice)
            val triggerY = priceToY(mss.triggerPrice)

            // Horizontal structural pivot line
            drawLine(
              color = NeonCyan,
              start = Offset(startX, levelY),
              end = Offset(endX + 18f, levelY),
              strokeWidth = 2.dp.toPx(),
              cap = StrokeCap.Round
            )

            // Shift impulse vector arrow
            drawLine(
              color = NeonCyan.copy(alpha = 0.65f),
              start = Offset(startX, levelY),
              end = Offset(endX, triggerY),
              strokeWidth = 1.2.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 4f), 0f)
            )

            // Marker dot at trigger
            drawCircle(
              color = NeonCyan.copy(alpha = 0.35f),
              radius = 5.5.dp.toPx(),
              center = Offset(endX, triggerY)
            )
            drawCircle(
              color = NeonCyan,
              radius = 2.8.dp.toPx(),
              center = Offset(endX, triggerY)
            )

            // MSS Text Tag
            textPaint.textSize = 20f
            textPaint.color = android.graphics.Color.parseColor("#00F2FE")
            drawContext.canvas.nativeCanvas.drawText(
              if (mss.type == StructureType.MSS_BULLISH) "▲ MSS" else "▼ MSS",
              endX + 6f,
              triggerY - 6f,
              textPaint
            )
          }
        }
      }

      // 8. Draw Break of Structure (BOS) Geometry
      if (showBOS) {
        val bosEvents = events.filter { it.type == StructureType.BOS_BULLISH || it.type == StructureType.BOS_BEARISH }
        for (bos in bosEvents) {
          if (bos.referenceIndex in 0 until n && bos.triggerIndex in 0 until n) {
            val startX = bos.referenceIndex * candleSlotW + (candleSlotW / 2f)
            val endX = bos.triggerIndex * candleSlotW + (candleSlotW / 2f)
            val levelY = priceToY(bos.referencePrice)
            val triggerY = priceToY(bos.triggerPrice)

            // Trend continuation step line
            drawLine(
              color = NeonEmerald,
              start = Offset(startX, levelY),
              end = Offset(endX + 16f, levelY),
              strokeWidth = 1.8.dp.toPx(),
              cap = StrokeCap.Round
            )

            drawCircle(
              color = NeonEmerald.copy(alpha = 0.35f),
              radius = 5.dp.toPx(),
              center = Offset(endX, triggerY)
            )
            drawCircle(
              color = NeonEmerald,
              radius = 2.5.dp.toPx(),
              center = Offset(endX, triggerY)
            )

            // BOS Text Tag
            textPaint.textSize = 19f
            textPaint.color = android.graphics.Color.parseColor("#00FF87")
            drawContext.canvas.nativeCanvas.drawText(
              "BOS",
              endX + 6f,
              triggerY - 6f,
              textPaint
            )
          }
        }
      }

      // 9. Draw Liquidity Sweeps (EQH / EQL)
      if (showLiqSweep) {
        val sweepEvents = events.filter {
          it.type == StructureType.LIQ_SWEEP_HIGH || it.type == StructureType.LIQ_SWEEP_LOW
        }
        for (sw in sweepEvents) {
          if (sw.referenceIndex in 0 until n && sw.triggerIndex in 0 until n) {
            val startX = sw.referenceIndex * candleSlotW + (candleSlotW / 2f)
            val endX = sw.triggerIndex * candleSlotW + (candleSlotW / 2f)
            val levelY = priceToY(sw.referencePrice)
            val wickY = priceToY(sw.triggerPrice)

            // Equal level boundary line
            drawLine(
              color = NeonCoral.copy(alpha = 0.85f),
              start = Offset(startX, levelY),
              end = Offset(endX + 12f, levelY),
              strokeWidth = 1.4.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(5f, 4f), 0f)
            )

            // Sweep wick grab point
            drawCircle(
              color = NeonCoral,
              radius = 3.5.dp.toPx(),
              center = Offset(endX, wickY)
            )

            // Liquidity Sweep Text Tag
            textPaint.textSize = 19f
            textPaint.color = android.graphics.Color.parseColor("#FB7185")
            drawContext.canvas.nativeCanvas.drawText(
              if (sw.type == StructureType.LIQ_SWEEP_HIGH) "BSL Sweep" else "SSL Sweep",
              endX + 6f,
              if (sw.type == StructureType.LIQ_SWEEP_HIGH) wickY - 6f else wickY + 18f,
              textPaint
            )
          }
        }
      }

      // 10. Interactive Crosshair Scrub with Telemetry Header
      selectedIndex?.let { idx ->
        if (idx in 0 until n) {
          val selCandle = candles[idx]
          val crossX = idx * candleSlotW + (candleSlotW / 2f)
          val crossY = priceToY(selCandle.close)

          // Vertical scrub line
          drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(crossX, 0f),
            end = Offset(crossX, h),
            strokeWidth = 1.2.dp.toPx()
          )

          // Horizontal price line
          drawLine(
            color = Color.White.copy(alpha = 0.6f),
            start = Offset(0f, crossY),
            end = Offset(w, crossY),
            strokeWidth = 1.2.dp.toPx()
          )

          drawCircle(
            color = Color.White,
            radius = 4.dp.toPx(),
            center = Offset(crossX, crossY)
          )

          // Telemetry box on top
          val scrubText = "C: \$${"%.2f".format(selCandle.close)} | O: \$${"%.2f".format(selCandle.open)} | H: \$${"%.2f".format(selCandle.high)} | L: \$${"%.2f".format(selCandle.low)}"
          textPaint.textSize = 21f
          textPaint.color = android.graphics.Color.WHITE
          drawContext.canvas.nativeCanvas.drawText(scrubText, 14f, 22f, textPaint)
        }
      }
    }
  }
}
