package com.example.trading.cache

import android.content.Context
import android.util.Log
import com.example.trading.codegen.CodeType
import com.example.trading.codegen.CustomFramework
import com.example.trading.codegen.PineScriptVersion
import com.example.trading.model.TradingTool
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Lightweight data transfer object for cached tool state.
 */
data class CachedToolConfig(
  val id: String,
  val isSelected: Boolean,
  val isFavorite: Boolean,
  val isAlertActive: Boolean,
  val lookbackPeriod: Int,
  val sensitivityScore: Double
)

/**
 * Represents the complete offline cached state of the trading precision tools.
 */
data class CachedAppState(
  val cachedTimestamp: Long,
  val activeTicker: String,
  val selectedFramework: CustomFramework,
  val pineScriptVersion: PineScriptVersion,
  val selectedCodeType: CodeType,
  val includeMSS: Boolean,
  val includeBOS: Boolean,
  val includeApex: Boolean,
  val toolConfigs: Map<String, CachedToolConfig>
)

/**
 * Manages local persistent caching of trading tool configurations and user favorites.
 * Ensures the application remains fully functional when offline or without internet access.
 */
class LocalToolCacheRepository(private val context: Context) {

  private val cacheFileName = "notify_tool_cache.json"
  private val cacheFile: File
    get() = File(context.filesDir, cacheFileName)

  /**
   * Persists all tool configurations and app state to device-local storage.
   * Enables 100% offline functionality and instantaneous restore on reboot/offline relaunch.
   */
  fun saveCache(
    tools: List<TradingTool>,
    activeTicker: String,
    framework: CustomFramework,
    pineVersion: PineScriptVersion,
    codeType: CodeType,
    includeMSS: Boolean,
    includeBOS: Boolean,
    includeApex: Boolean
  ): Boolean {
    return try {
      val root = JSONObject()
      root.put("version", 1)
      root.put("cachedTimestamp", System.currentTimeMillis())
      root.put("activeTicker", activeTicker)
      root.put("framework", framework.name)
      root.put("pineVersion", pineVersion.name)
      root.put("codeType", codeType.name)
      root.put("includeMSS", includeMSS)
      root.put("includeBOS", includeBOS)
      root.put("includeApex", includeApex)

      val toolsArray = JSONArray()
      for (tool in tools) {
        val toolObj = JSONObject()
        toolObj.put("id", tool.id)
        toolObj.put("isSelected", tool.isSelected)
        toolObj.put("isFavorite", tool.isFavorite)
        toolObj.put("isAlertActive", tool.isAlertActive)
        toolObj.put("lookbackPeriod", tool.lookbackPeriod)
        toolObj.put("sensitivityScore", tool.sensitivityScore)
        toolsArray.put(toolObj)
      }
      root.put("tools", toolsArray)

      // Write to disk
      cacheFile.writeText(root.toString(2))
      true
    } catch (e: Exception) {
      Log.e("LocalToolCache", "Failed to save offline tool cache", e)
      false
    }
  }

  /**
   * Loads cached tool configurations and app settings from local storage.
   */
  fun loadCache(): CachedAppState? {
    return try {
      if (!cacheFile.exists() || cacheFile.length() == 0L) {
        return null
      }
      val text = cacheFile.readText()
      if (text.isBlank()) return null

      val root = JSONObject(text)
      val cachedTimestamp = root.optLong("cachedTimestamp", 0L)
      val activeTicker = root.optString("activeTicker", "BTC/USDT")

      val frameworkStr = root.optString("framework", CustomFramework.PINE_SCRIPT.name)
      val framework = try {
        CustomFramework.valueOf(frameworkStr)
      } catch (_: Exception) {
        CustomFramework.PINE_SCRIPT
      }

      val pineVersionStr = root.optString("pineVersion", PineScriptVersion.V5.name)
      val pineVersion = try {
        PineScriptVersion.valueOf(pineVersionStr)
      } catch (_: Exception) {
        PineScriptVersion.V5
      }

      val codeTypeStr = root.optString("codeType", CodeType.CUSTOM_INDICATOR.name)
      val codeType = try {
        CodeType.valueOf(codeTypeStr)
      } catch (_: Exception) {
        CodeType.CUSTOM_INDICATOR
      }

      val includeMSS = root.optBoolean("includeMSS", true)
      val includeBOS = root.optBoolean("includeBOS", true)
      val includeApex = root.optBoolean("includeApex", true)

      val toolsArray = root.optJSONArray("tools")
      val toolMap = mutableMapOf<String, CachedToolConfig>()

      if (toolsArray != null) {
        for (i in 0 until toolsArray.length()) {
          val obj = toolsArray.getJSONObject(i)
          val id = obj.getString("id")
          toolMap[id] = CachedToolConfig(
            id = id,
            isSelected = obj.optBoolean("isSelected", false),
            isFavorite = obj.optBoolean("isFavorite", false),
            isAlertActive = obj.optBoolean("isAlertActive", true),
            lookbackPeriod = obj.optInt("lookbackPeriod", 3),
            sensitivityScore = obj.optDouble("sensitivityScore", 0.85)
          )
        }
      }

      CachedAppState(
        cachedTimestamp = cachedTimestamp,
        activeTicker = activeTicker,
        selectedFramework = framework,
        pineScriptVersion = pineVersion,
        selectedCodeType = codeType,
        includeMSS = includeMSS,
        includeBOS = includeBOS,
        includeApex = includeApex,
        toolConfigs = toolMap
      )
    } catch (e: Exception) {
      Log.e("LocalToolCache", "Failed to load offline tool cache", e)
      null
    }
  }

  fun hasCache(): Boolean = cacheFile.exists() && cacheFile.length() > 0L

  fun getCacheSizeFormatted(): String {
    return if (cacheFile.exists()) {
      "%.1f KB".format(cacheFile.length() / 1024.0)
    } else {
      "0 KB"
    }
  }

  fun clearCache(): Boolean {
    return try {
      if (cacheFile.exists()) cacheFile.delete() else true
    } catch (e: Exception) {
      false
    }
  }
}
