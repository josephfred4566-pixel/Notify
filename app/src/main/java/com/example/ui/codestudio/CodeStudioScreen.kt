package com.example.ui.codestudio

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.trading.codegen.AppScreen
import com.example.trading.codegen.CodeType
import com.example.trading.codegen.CustomFramework
import com.example.trading.codegen.PineScriptVersion
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.glass.GeometricIconType
import com.example.ui.glass.GlassBadge
import com.example.ui.glass.GlassButton
import com.example.ui.glass.GlassGeometricIcon
import com.example.ui.glass.GlassPanel
import com.example.ui.glass.GlassText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.example.ui.theme.BgDarkDeep
import com.example.ui.theme.GlassBorderHighlight
import com.example.ui.theme.GlassBorderSubtle
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
fun CodeStudioScreen(
  viewModel: DashboardViewModel,
  modifier: Modifier = Modifier
) {
  val state by viewModel.uiState.collectAsState()
  val insets = WindowInsets.statusBars.asPaddingValues()
  val context = LocalContext.current

  var isCodeTypeDropdownOpen by remember { mutableStateOf(false) }
  var isFrameworkDropdownOpen by remember { mutableStateOf(false) }
  var isVersionDropdownOpen by remember { mutableStateOf(false) }

  // Predictive back navigation back to Tools Dashboard
  BackHandler {
    viewModel.navigateTo(AppScreen.DASHBOARD)
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(BgDarkDeep)
  ) {
    // Ambient Background Lighting (stationery pure white glass halos, no color gradients)
    CodeStudioAtmosphere()

    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(top = insets.calculateTopPadding())
    ) {
      // 1. Top Header Row with Title and Stacked Top-Right Dropdowns
      CodeStudioTopBar(
        selectedType = state.selectedCodeType,
        selectedFramework = state.selectedFramework,
        selectedVersion = state.pineScriptVersion,
        isTypeDropdownOpen = isCodeTypeDropdownOpen,
        isFrameworkDropdownOpen = isFrameworkDropdownOpen,
        isVersionDropdownOpen = isVersionDropdownOpen,
        onToggleTypeDropdown = {
          isCodeTypeDropdownOpen = !isCodeTypeDropdownOpen
          if (isCodeTypeDropdownOpen) {
            isFrameworkDropdownOpen = false
            isVersionDropdownOpen = false
          }
        },
        onToggleFrameworkDropdown = {
          isFrameworkDropdownOpen = !isFrameworkDropdownOpen
          if (isFrameworkDropdownOpen) {
            isCodeTypeDropdownOpen = false
            isVersionDropdownOpen = false
          }
        },
        onToggleVersionDropdown = {
          isVersionDropdownOpen = !isVersionDropdownOpen
          if (isVersionDropdownOpen) {
            isCodeTypeDropdownOpen = false
            isFrameworkDropdownOpen = false
          }
        },
        onSelectType = {
          viewModel.selectCodeType(it)
          isCodeTypeDropdownOpen = false
        },
        onSelectFramework = {
          viewModel.selectFramework(it)
          isFrameworkDropdownOpen = false
        },
        onSelectVersion = {
          viewModel.selectPineScriptVersion(it)
          isVersionDropdownOpen = false
        },
        onBack = { viewModel.navigateTo(AppScreen.DASHBOARD) },
        onExport = { viewModel.exportFavorites(context) }
      )

      // 2. Main Content
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
      ) {
        // Mode & Framework Target Banner with PineScript Version Selector
        FrameworkTargetBanner(
          framework = state.selectedFramework,
          codeType = state.selectedCodeType,
          pineVersion = state.pineScriptVersion,
          onSelectVersion = { viewModel.selectPineScriptVersion(it) }
        )

        // Indicator Modules Selection Chips
        EngineModulesSelectorRow(
          includeMSS = state.includeMSSInExport,
          includeBOS = state.includeBOSInExport,
          includeApex = state.includeApexInExport,
          onToggleMSS = { viewModel.toggleExportMSS() },
          onToggleBOS = { viewModel.toggleExportBOS() },
          onToggleApex = { viewModel.toggleExportApex() }
        )

        // High-Performance Glass Code Viewer
        GlassCodeEditor(
          code = state.generatedCode,
          framework = state.selectedFramework,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        )

        // Bottom Action Bar: Copy Script & Info
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          GlassButton(
            text = "Copy ${state.selectedFramework.syntaxTag} Code",
            onClick = {
              val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
              val clip = ClipData.newPlainText("Trading Code", state.generatedCode)
              clipboard.setPrimaryClip(clip)
              Toast.makeText(context, "Code copied to clipboard!", Toast.LENGTH_SHORT).show()
            },
            isPrimary = true,
            icon = GeometricIconType.CHECK,
            modifier = Modifier.weight(1f)
          )

          GlassButton(
            text = "Back to Tools",
            onClick = { viewModel.navigateTo(AppScreen.DASHBOARD) },
            isPrimary = false,
            modifier = Modifier.width(130.dp)
          )
        }
      }
    }
  }
}

/**
 * Top bar featuring the brand navigation, title, and stacked top-right dropdowns:
 * 1. Code Type ("Custom Indicator" / "Custom Strategy")
 * 2. Framework ("MetaEditor" / "PineScript")
 * 3. PineScript Version ("Version 5" / "Version 6") when PineScript is active
 */
@Composable
private fun CodeStudioTopBar(
  selectedType: CodeType,
  selectedFramework: CustomFramework,
  selectedVersion: PineScriptVersion,
  isTypeDropdownOpen: Boolean,
  isFrameworkDropdownOpen: Boolean,
  isVersionDropdownOpen: Boolean,
  onToggleTypeDropdown: () -> Unit,
  onToggleFrameworkDropdown: () -> Unit,
  onToggleVersionDropdown: () -> Unit,
  onSelectType: (CodeType) -> Unit,
  onSelectFramework: (CustomFramework) -> Unit,
  onSelectVersion: (PineScriptVersion) -> Unit,
  onBack: () -> Unit,
  onExport: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 20.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    // Left: Back button, Title & Export Icon Button
    Row(verticalAlignment = Alignment.CenterVertically) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(GlassWhiteSubtle)
          .border(0.8.dp, GlassBorderSubtle, CircleShape)
          .clickable { onBack() }
          .testTag("code_studio_back_button"),
        contentAlignment = Alignment.Center
      ) {
        GlassGeometricIcon(
          iconType = GeometricIconType.CHEVRON_RIGHT,
          tint = TextHighContrast,
          modifier = Modifier
            .size(16.dp)
            .graphicsLayer { rotationZ = 180f }
        )
      }

      Spacer(modifier = Modifier.width(12.dp))

      Column {
        GlassText(
          text = "CODE STUDIO",
          style = NotifyTheme.typography.brandTitle.copy(fontSize = 18.sp),
          color = TextHighContrast
        )
        GlassText(
          text = "SCRIPT COMPILER",
          style = NotifyTheme.typography.caption.copy(fontSize = 9.sp, letterSpacing = 0.8.sp),
          color = Color.White.copy(alpha = 0.75f)
        )
      }

      Spacer(modifier = Modifier.width(10.dp))

      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(GlassWhiteSubtle)
          .border(0.8.dp, GlassBorderSubtle, CircleShape)
          .clickable { onExport() }
          .testTag("code_studio_export_button"),
        contentAlignment = Alignment.Center
      ) {
        GlassGeometricIcon(
          iconType = GeometricIconType.EXPORT_FILE,
          tint = TextHighContrast,
          modifier = Modifier.size(16.dp)
        )
      }
    }

    // Top Right: Stacked Drop-Downs
    Column(
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      // Dropdown 1: Code Type ("Custom Indicator" / "Custom Strategy")
      Box {
        FrostedDropdownTrigger(
          label = selectedType.displayName,
          badge = selectedType.shortLabel,
          tint = Color.White,
          isOpen = isTypeDropdownOpen,
          onClick = onToggleTypeDropdown,
          testTag = "code_type_dropdown_trigger"
        )

        // Dropdown Menu Popover
        this@Column.AnimatedVisibility(
          visible = isTypeDropdownOpen,
          enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
              slideInVertically(initialOffsetY = { -15 }),
          exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
              slideOutVertically(targetOffsetY = { -15 })
        ) {
          FrostedDropdownMenu(
            items = CodeType.values().toList(),
            selectedItem = selectedType,
            itemLabel = { it.displayName },
            itemSubtitle = { it.description },
            tint = Color.White,
            onSelect = onSelectType
          )
        }
      }

      // Dropdown 2: "Custom Framework" (MetaEditor / PineScript)
      Box {
        FrostedDropdownTrigger(
          label = "Framework: ${selectedFramework.displayName}",
          badge = selectedFramework.syntaxTag,
          tint = Color.White,
          isOpen = isFrameworkDropdownOpen,
          onClick = onToggleFrameworkDropdown,
          testTag = "framework_dropdown_trigger"
        )

        // Framework Dropdown Menu Popover
        this@Column.AnimatedVisibility(
          visible = isFrameworkDropdownOpen,
          enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
              slideInVertically(initialOffsetY = { -15 }),
          exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
              slideOutVertically(targetOffsetY = { -15 })
        ) {
          FrostedDropdownMenu(
            items = CustomFramework.values().toList(),
            selectedItem = selectedFramework,
            itemLabel = { it.displayName },
            itemSubtitle = { "Generate for ${it.ideTarget}" },
            tint = Color.White,
            onSelect = onSelectFramework
          )
        }
      }

      // Dropdown 3: PineScript Version (v5 / v6) - displayed when PineScript is active
      if (selectedFramework == CustomFramework.PINE_SCRIPT) {
        Box {
          FrostedDropdownTrigger(
            label = "Version: ${selectedVersion.displayName}",
            badge = selectedVersion.shortTag,
            tint = Color.White,
            isOpen = isVersionDropdownOpen,
            onClick = onToggleVersionDropdown,
            testTag = "pinescript_version_dropdown_trigger"
          )

          this@Column.AnimatedVisibility(
            visible = isVersionDropdownOpen,
            enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideInVertically(initialOffsetY = { -15 }),
            exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                slideOutVertically(targetOffsetY = { -15 })
          ) {
            FrostedDropdownMenu(
              items = PineScriptVersion.values().toList(),
              selectedItem = selectedVersion,
              itemLabel = { "${it.displayName} (${it.shortTag})" },
              itemSubtitle = { "TradingView PineScript header ${it.versionHeader}" },
              tint = Color.White,
              onSelect = onSelectVersion
            )
          }
        }
      }
    }
  }
}

/**
 * Clean frosted glass button acting as a drop-down trigger.
 * Pure white translucent styling without color gradients.
 */
@Composable
private fun FrostedDropdownTrigger(
  label: String,
  badge: String,
  tint: Color,
  isOpen: Boolean,
  onClick: () -> Unit,
  testTag: String
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()

  val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.96f else 1.0f,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMediumLow
    ),
    label = "dropdownScale"
  )

  Box(
    modifier = Modifier
      .graphicsLayer {
        scaleX = scale
        scaleY = scale
      }
      .clip(RoundedCornerShape(12.dp))
      .background(if (isOpen) Color.White.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.08f))
      .border(
        width = 1.dp,
        color = if (isOpen) Color.White.copy(alpha = 0.70f) else Color.White.copy(alpha = 0.22f),
        shape = RoundedCornerShape(12.dp)
      )
      .clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
      )
      .padding(horizontal = 10.dp, vertical = 6.dp)
      .testTag(testTag)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      GlassBadge(
        text = badge,
        tint = Color.White,
        backgroundColor = Color.White.copy(alpha = 0.14f)
      )
      GlassText(
        text = label,
        style = NotifyTheme.typography.headingMedium.copy(fontSize = 12.sp),
        color = TextHighContrast
      )
      // Chevron indicator
      GlassGeometricIcon(
        iconType = GeometricIconType.CHEVRON_RIGHT,
        tint = Color.White,
        modifier = Modifier
          .size(11.dp)
          .graphicsLayer { rotationZ = if (isOpen) 90f else 0f }
      )
    }
  }
}

/**
 * Frosted popover menu for dropdown selection.
 * Edge is cleanly uncolored (white specular border only).
 */
@Composable
private fun <T> FrostedDropdownMenu(
  items: List<T>,
  selectedItem: T,
  itemLabel: (T) -> String,
  itemSubtitle: (T) -> String,
  tint: Color,
  onSelect: (T) -> Unit
) {
  Box(
    modifier = Modifier
      .padding(top = 38.dp)
      .width(260.dp)
      .clip(RoundedCornerShape(16.dp))
      .background(Color(0xF50B111D))
      .border(1.dp, Color.White.copy(alpha = 0.30f), RoundedCornerShape(16.dp))
      .padding(8.dp)
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
      items.forEach { item ->
        val isSelected = item == selectedItem
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) Color.White.copy(alpha = 0.16f) else GlassWhiteUltraSubtle)
            .border(
              0.8.dp,
              if (isSelected) Color.White.copy(alpha = 0.65f) else Color.Transparent,
              RoundedCornerShape(10.dp)
            )
            .clickable { onSelect(item) }
            .padding(horizontal = 10.dp, vertical = 8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              GlassText(
                text = itemLabel(item),
                style = NotifyTheme.typography.headingMedium.copy(fontSize = 13.sp),
                color = if (isSelected) Color.White else TextHighContrast
              )
              Spacer(modifier = Modifier.height(2.dp))
              GlassText(
                text = itemSubtitle(item),
                style = NotifyTheme.typography.caption.copy(fontSize = 10.sp),
                color = TextMediumContrast,
                maxLines = 2
              )
            }

            if (isSelected) {
              GlassGeometricIcon(
                iconType = GeometricIconType.CHECK,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
              )
            }
          }
        }
      }
    }
  }
}

/**
 * Summary banner displaying target IDE and script configuration with interactive PineScript version pills.
 */
@Composable
private fun FrameworkTargetBanner(
  framework: CustomFramework,
  codeType: CodeType,
  pineVersion: PineScriptVersion,
  onSelectVersion: (PineScriptVersion) -> Unit
) {
  GlassPanel(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(16.dp),
    isInteractive = false,
    glowColor = Color.Transparent
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            GlassBadge(
              text = if (framework == CustomFramework.PINE_SCRIPT) "PineScript (${pineVersion.shortTag})" else framework.syntaxTag,
              tint = NeonCyan,
              backgroundColor = NeonCyan.copy(alpha = 0.12f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            GlassText(
              text = codeType.displayName,
              style = NotifyTheme.typography.headingMedium.copy(fontSize = 15.sp),
              color = TextHighContrast
            )
          }
          Spacer(modifier = Modifier.height(4.dp))
          GlassText(
            text = "IDE Destination: ${framework.ideTarget} (${framework.fileExtension})",
            style = NotifyTheme.typography.caption.copy(fontSize = 11.sp),
            color = TextMediumContrast
          )
        }

        GlassBadge(
          text = "ACTIVE COMPILER",
          tint = NeonEmerald,
          backgroundColor = NeonEmerald.copy(alpha = 0.12f)
        )
      }

      // Interactive PineScript Version Switcher (v5 vs v6)
      if (framework == CustomFramework.PINE_SCRIPT) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          GlassText(
            text = "PineScript Version:",
            style = NotifyTheme.typography.caption.copy(fontSize = 11.sp),
            color = TextMediumContrast
          )

          PineScriptVersion.values().forEach { v ->
            val isCurrent = v == pineVersion
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isCurrent) NeonCyan.copy(alpha = 0.20f) else Color.White.copy(alpha = 0.05f))
                .border(
                  0.8.dp,
                  if (isCurrent) NeonCyan.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.18f),
                  RoundedCornerShape(8.dp)
                )
                .clickable { onSelectVersion(v) }
                .padding(horizontal = 10.dp, vertical = 4.dp)
                .testTag("version_pill_${v.shortTag}")
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                if (isCurrent) {
                  GlassGeometricIcon(
                    iconType = GeometricIconType.CHECK,
                    tint = NeonCyan,
                    modifier = Modifier.size(10.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                }
                GlassText(
                  text = "${v.displayName} (${v.shortTag})",
                  style = NotifyTheme.typography.caption.copy(
                    fontSize = 10.5.sp,
                    letterSpacing = 0.4.sp
                  ),
                  color = if (isCurrent) NeonCyan else TextMediumContrast
                )
              }
            }
          }
        }
      }
    }
  }
}

/**
 * Selector chips for indicator modules (MSS, BOS, Apex) with purposeful accent colors.
 */
@Composable
private fun EngineModulesSelectorRow(
  includeMSS: Boolean,
  includeBOS: Boolean,
  includeApex: Boolean,
  onToggleMSS: () -> Unit,
  onToggleBOS: () -> Unit,
  onToggleApex: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    GlassText(
      text = "Engines:",
      style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
      color = TextLowContrast
    )

    ModuleToggleChip(label = "MSS (Shift)", active = includeMSS, accentColor = NeonCyan, onClick = onToggleMSS)
    ModuleToggleChip(label = "BOS (Trend)", active = includeBOS, accentColor = NeonEmerald, onClick = onToggleBOS)
    ModuleToggleChip(label = "Apex (Liquidity)", active = includeApex, accentColor = NeonAmber, onClick = onToggleApex)
  }
}

@Composable
private fun ModuleToggleChip(
  label: String,
  active: Boolean,
  accentColor: Color,
  onClick: () -> Unit
) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(if (active) accentColor.copy(alpha = 0.18f) else GlassWhiteUltraSubtle)
      .border(
        0.8.dp,
        if (active) accentColor.copy(alpha = 0.70f) else GlassBorderSubtle,
        RoundedCornerShape(10.dp)
      )
      .clickable { onClick() }
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      if (active) {
        GlassGeometricIcon(
          iconType = GeometricIconType.CHECK,
          tint = accentColor,
          modifier = Modifier.size(10.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      GlassText(
        text = label,
        style = NotifyTheme.typography.caption.copy(fontSize = 10.5.sp),
        color = if (active) TextHighContrast else TextLowContrast
      )
    }
  }
}

/**
 * Fast Regex-based tokenization syntax highlighter for trading scripts and code.
 */
private fun highlightCodeSyntax(
  line: String,
  framework: CustomFramework
): AnnotatedString {
  if (line.isBlank()) return AnnotatedString(line)

  val trimmed = line.trimStart()
  if (trimmed.startsWith("//")) {
    return AnnotatedString(
      text = line,
      spanStyle = SpanStyle(color = Color(0xFF6C7A89), fontStyle = FontStyle.Italic)
    )
  }

  val builder = AnnotatedString.Builder(line)

  // Directives and version headers (e.g. //@version=5, #property, #include, @Composable)
  val directiveRegex = Regex("""(@[a-zA-Z0-9_=]+|#(?:property|include|import|define)[^\s]*)""")
  for (match in directiveRegex.findAll(line)) {
    builder.addStyle(
      SpanStyle(color = NeonViolet, fontWeight = FontWeight.SemiBold),
      match.range.first,
      match.range.last + 1
    )
  }

  // Keywords (control flow, declarations, types)
  val keywordRegex = Regex("""\b(fun|val|var|class|object|import|package|if|else|when|for|while|return|input|strategy|indicator|plot|plotshape|plotchar|color|series|bool|int|float|string|true|false|void|double|datetime|const|static)\b""")
  for (match in keywordRegex.findAll(line)) {
    builder.addStyle(
      SpanStyle(color = NeonCyan, fontWeight = FontWeight.SemiBold),
      match.range.first,
      match.range.last + 1
    )
  }

  // Built-in namespaces, price series, and parameters
  val builtinRegex = Regex("""\b(ta|strategy|math|request|time|close|open|high|low|volume|hl2|hlc3|ohlc4|bar_index|na|color\.[a-z]+)\b""")
  for (match in builtinRegex.findAll(line)) {
    builder.addStyle(
      SpanStyle(color = NeonAmber, fontWeight = FontWeight.Medium),
      match.range.first,
      match.range.last + 1
    )
  }

  // Function calls (identifier followed by '(')
  val functionRegex = Regex("""\b([a-zA-Z_][a-zA-Z0-9_]*)(?=\s*\()""")
  for (match in functionRegex.findAll(line)) {
    builder.addStyle(
      SpanStyle(color = NeonEmerald),
      match.range.first,
      match.range.last + 1
    )
  }

  // String literals ("..." or '...')
  val stringRegex = Regex(""""([^"\\]|\\.)*"|'([^'\\]|\\.)*'""")
  for (match in stringRegex.findAll(line)) {
    builder.addStyle(
      SpanStyle(color = NeonCoral),
      match.range.first,
      match.range.last + 1
    )
  }

  // Numeric literals
  val numberRegex = Regex("""\b\d+(?:\.\d+)?\b""")
  for (match in numberRegex.findAll(line)) {
    builder.addStyle(
      SpanStyle(color = Color(0xFFFFB74D)),
      match.range.first,
      match.range.last + 1
    )
  }

  // Trailing comments (// ...)
  val inlineCommentIndex = line.indexOf("//")
  if (inlineCommentIndex >= 0) {
    builder.addStyle(
      SpanStyle(color = Color(0xFF6C7A89), fontStyle = FontStyle.Italic),
      inlineCommentIndex,
      line.length
    )
  }

  return builder.toAnnotatedString()
}

/**
 * High-performance Glass Code Editor with multi-color syntax highlighting and line numbers.
 */
@Composable
private fun GlassCodeEditor(
  code: String,
  framework: CustomFramework,
  modifier: Modifier = Modifier
) {
  val lines = remember(code) { code.lines() }

  GlassPanel(
    modifier = modifier,
    shape = RoundedCornerShape(18.dp),
    isInteractive = false
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(12.dp)
    ) {
      // Top Editor Toolbar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // macOS/IDE window controls with subtle glowing glass colors
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFF5252).copy(alpha = 0.85f)))
          Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFFFFD54F).copy(alpha = 0.85f)))
          Box(modifier = Modifier.size(9.dp).clip(CircleShape).background(Color(0xFF00E676).copy(alpha = 0.85f)))
        }

        GlassText(
          text = "${framework.fileExtension} • ${lines.size} lines",
          style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 10.5.sp),
          color = TextLowContrast
        )
      }

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(1.dp)
          .background(GlassBorderSubtle)
      )

      Spacer(modifier = Modifier.height(8.dp))

      // Code Scroll Area
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 4.dp)
      ) {
        itemsIndexed(lines) { index, line ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(rememberScrollState())
          ) {
            // Line Number
            GlassText(
              text = "${index + 1}".padStart(3, ' '),
              style = NotifyTheme.typography.monospaceTelemetry.copy(fontSize = 11.sp),
              color = TextDisabled,
              modifier = Modifier.width(32.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            // Highlighted Syntax Line
            val highlightedLine = remember(line, framework) {
              highlightCodeSyntax(line, framework)
            }

            GlassText(
              text = highlightedLine,
              style = NotifyTheme.typography.monospaceTelemetry.copy(
                fontSize = 11.5.sp,
                lineHeight = 16.sp
              ),
              color = TextHighContrast
            )
          }
        }
      }
    }
  }
}

/**
 * Ambient background atmosphere with disciplined, subtle cyber-glass tints.
 */
@Composable
private fun CodeStudioAtmosphere() {
  Canvas(modifier = Modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    // Subtle neon cyan ambient halo (top-right)
    drawCircle(
      color = NeonCyan.copy(alpha = 0.035f),
      center = Offset(w * 0.90f, h * 0.10f),
      radius = w * 0.70f
    )

    // Subtle neon violet ambient halo (center-left)
    drawCircle(
      color = NeonViolet.copy(alpha = 0.025f),
      center = Offset(w * 0.10f, h * 0.50f),
      radius = w * 0.60f
    )

    // Subtle white glow (bottom-right)
    drawCircle(
      color = Color.White.copy(alpha = 0.015f),
      center = Offset(w * 0.85f, h * 0.85f),
      radius = w * 0.60f
    )
  }
}
