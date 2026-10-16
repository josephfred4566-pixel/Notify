package com.example.ui.glass

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.ElectricBlue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GlassBorderHighlight
import com.example.ui.theme.GlassBorderProminent
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.GlassWhiteMedium
import com.example.ui.theme.GlassWhiteSubtle
import com.example.ui.theme.GlassWhiteUltraSubtle
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NotifyTheme
import com.example.ui.theme.TextHighContrast
import com.example.ui.theme.TextLowContrast
import com.example.ui.theme.TextMediumContrast
import kotlin.math.roundToInt

/**
 * High-contrast foundational text composable. Built without any Material dependencies.
 */
@Composable
fun GlassText(
  text: String,
  modifier: Modifier = Modifier,
  style: TextStyle = NotifyTheme.typography.bodyPrimary,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip
) {
  val resolvedColor = if (color != Color.Unspecified) color else style.color
  val resolvedStyle = style.copy(color = resolvedColor)

  BasicText(
    text = text,
    modifier = modifier,
    style = resolvedStyle,
    maxLines = maxLines,
    overflow = overflow
  )
}

@Composable
fun GlassText(
  text: AnnotatedString,
  modifier: Modifier = Modifier,
  style: TextStyle = NotifyTheme.typography.bodyPrimary,
  color: Color = Color.Unspecified,
  maxLines: Int = Int.MAX_VALUE,
  overflow: TextOverflow = TextOverflow.Clip
) {
  val resolvedColor = if (color != Color.Unspecified) color else style.color
  val resolvedStyle = style.copy(color = resolvedColor)

  BasicText(
    text = text,
    modifier = modifier,
    style = resolvedStyle,
    maxLines = maxLines,
    overflow = overflow
  )
}

/**
 * Helper to build an AnnotatedString with matching search query characters highlighted.
 */
fun buildHighlightedAnnotatedString(
  fullText: String,
  query: String,
  defaultColor: Color,
  highlightColor: Color = ElectricBlue
): AnnotatedString {
  if (query.isBlank()) {
    return AnnotatedString(fullText, spanStyle = SpanStyle(color = defaultColor))
  }

  val builder = AnnotatedString.Builder()
  val lowerText = fullText.lowercase()
  val lowerQuery = query.lowercase()

  var startIndex = 0
  var index = lowerText.indexOf(lowerQuery, startIndex)

  if (index == -1) {
    return AnnotatedString(fullText, spanStyle = SpanStyle(color = defaultColor))
  }

  while (index != -1) {
    if (index > startIndex) {
      builder.append(fullText.substring(startIndex, index))
      builder.addStyle(
        SpanStyle(color = defaultColor),
        builder.length - (index - startIndex),
        builder.length
      )
    }
    val matchEnd = index + query.length
    builder.append(fullText.substring(index, matchEnd))
    builder.addStyle(
      SpanStyle(
        color = highlightColor,
        fontWeight = FontWeight.Bold
      ),
      builder.length - query.length,
      builder.length
    )
    startIndex = matchEnd
    index = lowerText.indexOf(lowerQuery, startIndex)
  }

  if (startIndex < fullText.length) {
    builder.append(fullText.substring(startIndex))
    builder.addStyle(
      SpanStyle(color = defaultColor),
      builder.length - (fullText.length - startIndex),
      builder.length
    )
  }

  return builder.toAnnotatedString()
}

/**
 * Universal Frosted Glass Panel with spring-based press kinetics.
 * Utilizes multi-layer gradients and thin specular borders to emulate glassmorphism.
 */
@Composable
fun GlassPanel(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(22.dp),
  isInteractive: Boolean = true,
  isSelected: Boolean = false,
  glowColor: Color = Color.Transparent,
  onClick: (() -> Unit)? = null,
  onLongClick: (() -> Unit)? = null,
  onHoverChange: ((Boolean) -> Unit)? = null,
  content: @Composable BoxScope.() -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  // Spring physics for layout transitions and physical interactivity
  val scale by animateFloatAsState(
    targetValue = if (isInteractive && isPressed) 0.975f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "glassScale"
  )

  val borderColor = if (isSelected) {
    Color.White.copy(alpha = 0.85f)
  } else {
    Color.White.copy(alpha = 0.18f)
  }

  val backgroundColor = if (isSelected) {
    Color.White.copy(alpha = 0.12f)
  } else {
    Color.White.copy(alpha = 0.05f)
  }

  Box(
    modifier = modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .drawBehind {
        if (glowColor != Color.Transparent || isSelected) {
          val activeGlow = if (isSelected) Color.White.copy(alpha = 0.08f) else glowColor
          drawCircle(
            color = activeGlow,
            radius = size.maxDimension * 0.45f,
            center = Offset(size.width * 0.85f, size.height * 0.2f)
          )
        }
      }
      .clip(shape)
      .background(backgroundColor)
      .border(
        width = if (isSelected) 1.5.dp else 1.dp,
        color = borderColor,
        shape = shape
      )
      .then(
        if (onHoverChange != null) {
          Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
              while (true) {
                val event = awaitPointerEvent()
                when (event.type) {
                  PointerEventType.Enter -> onHoverChange(true)
                  PointerEventType.Exit -> onHoverChange(false)
                }
              }
            }
          }
        } else Modifier
      )
      .then(
        if (onLongClick != null) {
          Modifier.pointerInput(Unit) {
            detectTapGestures(
              onTap = { onClick?.invoke() },
              onLongPress = { onLongClick() }
            )
          }
        } else if (onClick != null) {
          Modifier.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
          )
        } else Modifier
      ),
    content = content
  )
}

/**
 * Frosted Glass Badge for trading indicators and alerts.
 */
@Composable
fun GlassBadge(
  text: String,
  modifier: Modifier = Modifier,
  tint: Color = Color.White,
  backgroundColor: Color = Color.White.copy(alpha = 0.12f)
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(10.dp))
      .background(backgroundColor)
      .border(0.8.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 4.dp),
    contentAlignment = Alignment.Center
  ) {
    GlassText(
      text = text,
      style = NotifyTheme.typography.caption.copy(
        fontSize = 10.5.sp,
        letterSpacing = 0.6.sp
      ),
      color = tint
    )
  }
}

/**
 * Glass Switch using pure Compose foundation primitives and Spring physics.
 */
@Composable
fun GlassSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  activeColor: Color = Color.White
) {
  val thumbOffset by animateFloatAsState(
    targetValue = if (checked) 22f else 2f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioLowBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "switchThumb"
  )

  val trackColor = if (checked) Color.White.copy(alpha = 0.25f) else Color.White.copy(alpha = 0.08f)
  val borderColor = if (checked) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.20f)

  Box(
    modifier = modifier
      .width(48.dp)
      .height(26.dp)
      .clip(CircleShape)
      .background(trackColor)
      .border(1.dp, borderColor, CircleShape)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { onCheckedChange(!checked) }
      )
      .padding(2.dp),
    contentAlignment = Alignment.CenterStart
  ) {
    Box(
      modifier = Modifier
        .offset { IntOffset(thumbOffset.dp.roundToPx(), 0) }
        .size(20.dp)
        .clip(CircleShape)
        .background(
          if (checked) Color.White else Color.White.copy(alpha = 0.35f)
        )
        .border(
          0.8.dp,
          if (checked) Color.White else Color.White.copy(alpha = 0.40f),
          CircleShape
        )
    )
  }
}

/**
 * Geometric Vector Icon Renderer using foundational Canvas.
 */
@Composable
fun GlassGeometricIcon(
  iconType: GeometricIconType,
  tint: Color = TextHighContrast,
  modifier: Modifier = Modifier.size(20.dp)
) {
  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height

    when (iconType) {
      GeometricIconType.MSS_SHIFT -> {
        // Z-shaped market structure shift break
        val path = Path().apply {
          moveTo(w * 0.15f, h * 0.75f)
          lineTo(w * 0.40f, h * 0.35f)
          lineTo(w * 0.60f, h * 0.60f)
          lineTo(w * 0.88f, h * 0.20f)
        }
        drawPath(path, tint, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
        // Break line
        drawLine(
          color = tint.copy(alpha = 0.5f),
          start = Offset(w * 0.25f, h * 0.35f),
          end = Offset(w * 0.95f, h * 0.35f),
          strokeWidth = 1.2.dp.toPx()
        )
      }
      GeometricIconType.BOS_TREND -> {
        // Stepping trend continuation
        val path = Path().apply {
          moveTo(w * 0.15f, h * 0.80f)
          lineTo(w * 0.45f, h * 0.45f)
          lineTo(w * 0.55f, h * 0.55f)
          lineTo(w * 0.85f, h * 0.20f)
        }
        drawPath(path, tint, style = Stroke(width = 2.4.dp.toPx(), cap = StrokeCap.Round))
        // Arrow head
        drawLine(tint, Offset(w * 0.85f, h * 0.20f), Offset(w * 0.65f, h * 0.20f), strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.85f, h * 0.20f), Offset(w * 0.85f, h * 0.40f), strokeWidth = 2.4.dp.toPx(), cap = StrokeCap.Round)
      }
      GeometricIconType.APEX_LEVEL -> {
        // High & low apex brackets
        drawCircle(tint, radius = 3.dp.toPx(), center = Offset(w * 0.5f, h * 0.25f))
        drawLine(tint, Offset(w * 0.2f, h * 0.25f), Offset(w * 0.8f, h * 0.25f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawCircle(tint.copy(alpha = 0.5f), radius = 3.dp.toPx(), center = Offset(w * 0.5f, h * 0.75f))
        drawLine(tint.copy(alpha = 0.5f), Offset(w * 0.2f, h * 0.75f), Offset(w * 0.8f, h * 0.75f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
      }
      GeometricIconType.FAIR_VALUE_GAP -> {
        // 3-bar gap box
        drawLine(tint, Offset(w * 0.2f, h * 0.8f), Offset(w * 0.2f, h * 0.2f), strokeWidth = 2.dp.toPx())
        drawLine(tint, Offset(w * 0.8f, h * 0.9f), Offset(w * 0.8f, h * 0.4f), strokeWidth = 2.dp.toPx())
        drawRect(
          color = tint.copy(alpha = 0.25f),
          topLeft = Offset(w * 0.2f, h * 0.35f),
          size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.25f)
        )
      }
      GeometricIconType.ORDER_BLOCK -> {
        // Institutional block
        drawRect(
          color = tint.copy(alpha = 0.35f),
          topLeft = Offset(w * 0.2f, h * 0.3f),
          size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.4f)
        )
        drawRect(
          color = tint,
          topLeft = Offset(w * 0.2f, h * 0.3f),
          size = androidx.compose.ui.geometry.Size(w * 0.6f, h * 0.4f),
          style = Stroke(width = 1.5.dp.toPx())
        )
      }
      GeometricIconType.CHECK -> {
        val path = Path().apply {
          moveTo(w * 0.2f, h * 0.5f)
          lineTo(w * 0.45f, h * 0.75f)
          lineTo(w * 0.85f, h * 0.25f)
        }
        drawPath(path, tint, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
      }
      GeometricIconType.CHEVRON_RIGHT -> {
        val path = Path().apply {
          moveTo(w * 0.35f, h * 0.2f)
          lineTo(w * 0.65f, h * 0.5f)
          lineTo(w * 0.35f, h * 0.8f)
        }
        drawPath(path, tint, style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round))
      }
      GeometricIconType.CLOSE -> {
        drawLine(tint, Offset(w * 0.25f, h * 0.25f), Offset(w * 0.75f, h * 0.75f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.75f, h * 0.25f), Offset(w * 0.25f, h * 0.75f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
      }
      GeometricIconType.BELL_NOTIFY -> {
        val path = Path().apply {
          moveTo(w * 0.5f, h * 0.2f)
          lineTo(w * 0.65f, h * 0.45f)
          lineTo(w * 0.75f, h * 0.65f)
          lineTo(w * 0.25f, h * 0.65f)
          lineTo(w * 0.35f, h * 0.45f)
          close()
        }
        drawPath(path, tint, style = Stroke(width = 1.8.dp.toPx()))
        drawCircle(tint, radius = 1.5.dp.toPx(), center = Offset(w * 0.5f, h * 0.78f))
      }
      GeometricIconType.PULSE_SIGNAL -> {
        val path = Path().apply {
          moveTo(w * 0.1f, h * 0.5f)
          lineTo(w * 0.3f, h * 0.5f)
          lineTo(w * 0.45f, h * 0.2f)
          lineTo(w * 0.6f, h * 0.8f)
          lineTo(w * 0.75f, h * 0.5f)
          lineTo(w * 0.9f, h * 0.5f)
        }
        drawPath(path, tint, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
      }
      GeometricIconType.STAR_FILLED -> {
        val path = createStarPath(w, h)
        drawPath(path, tint)
      }
      GeometricIconType.STAR_OUTLINE -> {
        val path = createStarPath(w, h)
        drawPath(path, tint, style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
      }
      GeometricIconType.INFO_CIRCLE -> {
        drawCircle(tint, radius = minOf(w, h) * 0.44f, center = Offset(w / 2f, h / 2f), style = Stroke(width = 1.4.dp.toPx()))
        drawCircle(tint, radius = 1.2.dp.toPx(), center = Offset(w / 2f, h * 0.32f))
        drawLine(tint, Offset(w / 2f, h * 0.45f), Offset(w / 2f, h * 0.70f), strokeWidth = 1.6.dp.toPx(), cap = StrokeCap.Round)
      }
      GeometricIconType.EXPORT_FILE -> {
        val path = Path().apply {
          moveTo(w * 0.2f, h * 0.62f)
          lineTo(w * 0.2f, h * 0.82f)
          lineTo(w * 0.8f, h * 0.82f)
          lineTo(w * 0.8f, h * 0.62f)
        }
        drawPath(path, tint, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        drawLine(tint, Offset(w * 0.5f, h * 0.22f), Offset(w * 0.5f, h * 0.64f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.36f, h * 0.50f), Offset(w * 0.5f, h * 0.64f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
        drawLine(tint, Offset(w * 0.64f, h * 0.50f), Offset(w * 0.5f, h * 0.64f), strokeWidth = 1.8.dp.toPx(), cap = StrokeCap.Round)
      }
      GeometricIconType.VIEW_CARDS -> {
        val rectTop = h * 0.16f
        val rectBottom = h * 0.84f
        val rectLeft = w * 0.18f
        val rectRight = w * 0.82f
        drawRoundRect(
          color = tint,
          topLeft = Offset(rectLeft, rectTop),
          size = Size(rectRight - rectLeft, rectBottom - rectTop),
          cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx()),
          style = Stroke(width = 1.6.dp.toPx())
        )
        drawLine(
          color = tint,
          start = Offset(rectLeft, h * 0.40f),
          end = Offset(rectRight, h * 0.40f),
          strokeWidth = 1.4.dp.toPx()
        )
        drawLine(
          color = tint.copy(alpha = 0.65f),
          start = Offset(rectLeft + 3.dp.toPx(), h * 0.62f),
          end = Offset(rectRight - 3.dp.toPx(), h * 0.62f),
          strokeWidth = 1.4.dp.toPx(),
          cap = StrokeCap.Round
        )
      }
      GeometricIconType.VIEW_GRID -> {
        val gap = 2.5.dp.toPx()
        val pad = 3.dp.toPx()
        val cellSize = (minOf(w, h) - 2 * pad - gap) / 2f
        val startX = (w - (2 * cellSize + gap)) / 2f
        val startY = (h - (2 * cellSize + gap)) / 2f

        for (row in 0..1) {
          for (col in 0..1) {
            val left = startX + col * (cellSize + gap)
            val top = startY + row * (cellSize + gap)
            drawRoundRect(
              color = tint,
              topLeft = Offset(left, top),
              size = Size(cellSize, cellSize),
              cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
              style = Stroke(width = 1.4.dp.toPx())
            )
          }
        }
      }
      GeometricIconType.VIEW_LIST -> {
        val startX = w * 0.16f
        val endX = w * 0.84f
        val lineStartX = w * 0.38f
        val yCoords = listOf(h * 0.28f, h * 0.50f, h * 0.72f)
        for (y in yCoords) {
          drawCircle(
            color = tint,
            radius = 1.5.dp.toPx(),
            center = Offset(startX + 1.5.dp.toPx(), y)
          )
          drawLine(
            color = tint,
            start = Offset(lineStartX, y),
            end = Offset(endX, y),
            strokeWidth = 1.8.dp.toPx(),
            cap = StrokeCap.Round
          )
        }
      }
    }
  }
}

private fun createStarPath(w: Float, h: Float): Path {
  val path = Path()
  val cx = w / 2f
  val cy = h / 2f
  val outerRadius = minOf(w, h) * 0.46f
  val innerRadius = outerRadius * 0.45f
  for (i in 0 until 10) {
    val angle = (i * 36.0 - 90.0) * (Math.PI / 180.0)
    val r = if (i % 2 == 0) outerRadius else innerRadius
    val x = cx + (r * Math.cos(angle)).toFloat()
    val y = cy + (r * Math.sin(angle)).toFloat()
    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
  }
  path.close()
  return path
}

enum class GeometricIconType {
  MSS_SHIFT,
  BOS_TREND,
  APEX_LEVEL,
  FAIR_VALUE_GAP,
  ORDER_BLOCK,
  CHECK,
  CHEVRON_RIGHT,
  CLOSE,
  BELL_NOTIFY,
  PULSE_SIGNAL,
  STAR_FILLED,
  STAR_OUTLINE,
  INFO_CIRCLE,
  EXPORT_FILE,
  VIEW_CARDS,
  VIEW_GRID,
  VIEW_LIST
}

/**
 * Glass Button with spring bounce and custom gradient sheen.
 */
@Composable
fun GlassButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  isPrimary: Boolean = true,
  icon: GeometricIconType? = null
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "btnScale"
  )

  val bgColor = if (isPrimary) {
    Color.White
  } else {
    Color.White.copy(alpha = 0.10f)
  }

  val borderColor = if (isPrimary) {
    Color.White
  } else {
    Color.White.copy(alpha = 0.25f)
  }

  val textColor = if (isPrimary) Color(0xFF070A0F) else Color.White

  Box(
    modifier = modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(RoundedCornerShape(16.dp))
      .background(bgColor)
      .border(
        width = 1.dp,
        color = borderColor,
        shape = RoundedCornerShape(16.dp)
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .padding(horizontal = 18.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        GlassGeometricIcon(
          iconType = icon,
          tint = textColor,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
      }
      GlassText(
        text = text,
        style = NotifyTheme.typography.headingMedium.copy(fontSize = 14.sp),
        color = textColor
      )
    }
  }
}
