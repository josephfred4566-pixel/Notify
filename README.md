# Notify — Trading Analysis Dashboard

Notify is an Android trading-analysis app built with Kotlin and Jetpack Compose. It presents a searchable dashboard of market-structure tools, an interactive chart/inspection flow, and a code studio for generating indicator or strategy code.

## Features in the codebase

- Browse, search, favorite, inspect, and filter tools, with card/grid/list dashboard modes.
- Analyze candle data for Market Structure Shifts (MSS), Break of Structure (BOS), Apex extremes, Fair Value Gaps (FVG), Institutional Order Blocks, and Liquidity Sweeps.
- Inspect detected structure events and view them on the app's trading chart UI.
- Generate custom indicator or strategy code for TradingView Pine Script v5/v6 and MetaTrader 5 (MQL5/MetaEditor).
- Configure which structure signals are included in generated output and export selected tools/configuration as JSON.
- Persist first-launch and dashboard display preferences with Android SharedPreferences.

## Technology

- Kotlin, AndroidX, and Jetpack Compose / Material 3
- A local `MarketStructureEngine` for swing-point and market-structure calculations
- A deterministic `CodeGeneratorEngine` for Pine Script and MQL5 templates

## Build and run

Open the repository root in Android Studio, allow the Gradle project to sync, and run the `app` configuration on an emulator or Android device. The project targets Android API 36 and declares a minimum Android API level of 24; use an Android Studio/JDK/Gradle setup compatible with the Android Gradle Plugin version in `gradle/libs.versions.toml`.

This checkout does not include a Gradle wrapper script. If building from a terminal, install a compatible Gradle version and run:

```bash
gradle assembleDebug
```

The debug APK is normally written to `app/build/outputs/apk/debug/app-debug.apk`.

## Market-data and trading limitations

The dashboard currently initializes its chart and market events from generated sample candle data (including a default BTC/USDT label). No exchange market-data connection or order execution flow was found in the app code reviewed. Treat displayed prices, detections, and alert-related UI as demonstration/research functionality, not live trading signals. Validate the algorithms and connect a trusted data source before using this project for decisions.

The repository includes a commented Gemini key template for AI Studio builds, but the dashboard calculations and code generation described above are local code paths. Never hard-code API keys or package private credentials in an APK.
