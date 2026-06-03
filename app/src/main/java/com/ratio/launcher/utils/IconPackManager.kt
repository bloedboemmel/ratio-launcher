package com.ratio.launcher.utils

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import org.xmlpull.v1.XmlPullParser

data class IconPack(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

object IconPackManager {

    private const val PREFS = "ratio_prefs"
    private const val KEY_ICON_PACK = "icon_pack"

    private var iconMap: Map<String, String>? = null
    private var loadedPackage: String? = null

    fun getInstalledIconPacks(context: Context): List<IconPack> {
        val pm = context.packageManager
        val packs = mutableListOf<IconPack>()

        val intents = listOf(
            Intent("com.novalauncher.THEME"),
            Intent("org.adw.launcher.THEMES"),
            Intent("com.teslacoilsw.launcher.THEME"),
            Intent("com.gau.go.launcherex.theme"),
            Intent("org.adw.launcher.icons.ACTION_PICK_ICON")
        )

        val seen = mutableSetOf<String>()
        for (intent in intents) {
            val resolved = pm.queryIntentActivities(intent, 0)
            for (ri in resolved) {
                val pkg = ri.activityInfo.packageName
                if (seen.add(pkg)) {
                    packs.add(IconPack(
                        packageName = pkg,
                        label = ri.loadLabel(pm).toString(),
                        icon = ri.loadIcon(pm)
                    ))
                }
            }
        }
        return packs
    }

    fun getCurrentIconPack(context: Context): String? {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ICON_PACK, null)
    }

    fun setIconPack(context: Context, packageName: String?) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY_ICON_PACK, packageName).apply()
        iconMap = null
        loadedPackage = null
    }

    fun getIconForPackage(context: Context, appPackageName: String): Drawable? {
        val packPackage = getCurrentIconPack(context) ?: return null

        if (iconMap == null || loadedPackage != packPackage) {
            loadIconMap(context, packPackage)
        }

        val componentName = iconMap?.get(appPackageName) ?: return null

        return try {
            val res = context.packageManager.getResourcesForApplication(packPackage)
            val resId = res.getIdentifier(componentName, "drawable", packPackage)
            if (resId != 0) res.getDrawable(resId, null) else null
        } catch (_: Exception) {
            null
        }
    }

    private fun loadIconMap(context: Context, packPackage: String) {
        val map = mutableMapOf<String, String>()

        try {
            val res = context.packageManager.getResourcesForApplication(packPackage)
            val appfilterResId = res.getIdentifier("appfilter", "xml", packPackage)

            if (appfilterResId != 0) {
                val parser = res.getXml(appfilterResId)
                while (parser.next() != XmlPullParser.END_DOCUMENT) {
                    if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                        val component = parser.getAttributeValue(null, "component")
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (component != null && drawable != null) {
                            val pkg = extractPackage(component)
                            if (pkg != null) {
                                map[pkg] = drawable
                            }
                        }
                    }
                }
            } else {
                val assets = context.packageManager
                    .getResourcesForApplication(packPackage).assets
                val inputStream = try { assets.open("appfilter.xml") } catch (_: Exception) { null }
                if (inputStream != null) {
                    val factory = org.xmlpull.v1.XmlPullParserFactory.newInstance()
                    val parser = factory.newPullParser()
                    parser.setInput(inputStream, "UTF-8")
                    while (parser.next() != XmlPullParser.END_DOCUMENT) {
                        if (parser.eventType == XmlPullParser.START_TAG && parser.name == "item") {
                            val component = parser.getAttributeValue(null, "component")
                            val drawable = parser.getAttributeValue(null, "drawable")
                            if (component != null && drawable != null) {
                                val pkg = extractPackage(component)
                                if (pkg != null) {
                                    map[pkg] = drawable
                                }
                            }
                        }
                    }
                    inputStream.close()
                }
            }
        } catch (_: Exception) {}

        iconMap = map
        loadedPackage = packPackage
    }

    private fun extractPackage(component: String): String? {
        val match = Regex("ComponentInfo\\{(.+?)/").find(component)
        return match?.groupValues?.get(1)
    }
}
