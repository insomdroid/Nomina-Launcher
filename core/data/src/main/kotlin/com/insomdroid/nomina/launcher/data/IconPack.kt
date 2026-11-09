package com.insomdroid.nomina.data

import java.util.concurrent.atomic.AtomicInteger
import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.util.Xml
import androidx.core.content.res.ResourcesCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import org.xmlpull.v1.XmlPullParser
import javax.inject.Inject
import javax.inject.Singleton

data class IconPackInfo(val packageName: String, val label: String)

/** Parsed ADW/Nova appfilter mappings and the optional iconback colour. */
class IconPack(
    private val packageName: String,
    private val res: Resources,
    private val componentMap: Map<String, String>,
) {
    fun getIcon(component: ComponentName): Drawable? {
        val name = drawableName(component) ?: return null
        val id = res.getIdentifier(name, "drawable", packageName).takeIf { it != 0 } ?: return null
        return runCatching { ResourcesCompat.getDrawable(res, id, null) }.getOrNull()
    }

    fun drawableName(component: ComponentName): String? =
        componentMap["${component.packageName}/${component.className}"]

    fun mappedDrawables(): List<String> = componentMap.values.distinct()

    /** Average colour of opaque pixels in the pack's iconback image; transparent pixels are ignored. */
    val iconBackColor: Int? by lazy(LazyThreadSafetyMode.PUBLICATION) { readIconBackColor() }

    @SuppressLint("DiscouragedApi")
    private fun readIconBackColor(): Int? = runCatching {
        val id = res.getIdentifier("iconback", "drawable", packageName)
            .takeIf { it != 0 }
            ?: res.getIdentifier("iconback", "mipmap", packageName).takeIf { it != 0 }
            ?: return null
        val drawable = ResourcesCompat.getDrawable(res, id, null) ?: return null
        val size = 48
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))
            var red = 0L
            var green = 0L
            var blue = 0L
            var weight = 0L
            for (y in 0 until size) for (x in 0 until size) {
                val pixel = bitmap.getPixel(x, y)
                val alpha = Color.alpha(pixel)
                if (alpha > 16) {
                    val w = alpha.toLong()
                    red += Color.red(pixel) * w
                    green += Color.green(pixel) * w
                    blue += Color.blue(pixel) * w
                    weight += w
                }
            }
            if (weight == 0L) null else Color.rgb(
                (red / weight).toInt(),
                (green / weight).toInt(),
                (blue / weight).toInt(),
            )
        } finally {
            bitmap.recycle()
        }
    }.getOrNull()
}

internal fun parseDrawableList(parser: XmlPullParser): List<String> {
    val names = LinkedHashSet<String>()
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && parser.name == "item") {
            parser.getAttributeValue(null, "drawable")?.takeIf { it.isNotBlank() }?.let(names::add)
        }
        event = parser.next()
    }
    return names.toList()
}

@Singleton
class IconPackRepository internal constructor(
    private val context: Context,
    private val loader: (String) -> IconPack?,
) {
    @Inject constructor(@ApplicationContext context: Context) : this(context, AppFilterLoader(context)::load)

    private val cache = HashMap<String, IconPack?>()
    private val generation = AtomicInteger()
    private val loading = HashMap<String, Int>()
    private val resources = HashMap<String, Resources?>()
    private val names = HashMap<String, List<String>>()

    fun installedPacks(): List<IconPackInfo> {
        val pm = context.packageManager
        val packages = LinkedHashSet<String>()
        val intents = THEME_ACTIONS.map { Intent(it) } +
            THEME_CATEGORIES.map { Intent(Intent.ACTION_MAIN).addCategory(it) }
        for (intent in intents) {
            val list = runCatching { pm.queryIntentActivities(intent, 0) }.getOrElse { emptyList() }
            list.forEach { packages.add(it.activityInfo.packageName) }
        }
        return packages.mapNotNull { pkg ->
            runCatching {
                IconPackInfo(pkg, pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString())
            }.getOrNull()
        }.sortedBy { it.label.lowercase() }
    }

    @Synchronized
    fun invalidate(pkg: String): Boolean {
        val wasCached = cache.containsKey(pkg) || resources.containsKey(pkg) || names.containsKey(pkg) ||
            (loading[pkg] ?: 0) > 0
        cache.remove(pkg)
        resources.remove(pkg)
        names.remove(pkg)
        generation.incrementAndGet()
        return wasCached
    }

    @SuppressLint("DiscouragedApi")
    fun iconNames(pkg: String): List<String> {
        synchronized(this) { names[pkg]?.let { return it } }
        val res = packResources(pkg) ?: return emptyList()
        val listed = runCatching { readDrawableList(pkg, res) }.getOrNull().orEmpty()
        val all = listed.ifEmpty { get(pkg)?.mappedDrawables().orEmpty() }
            .filter { res.getIdentifier(it, "drawable", pkg) != 0 }
        synchronized(this) { names[pkg] = all }
        return all
    }

    fun suggestion(pkg: String, component: ComponentName): String? = get(pkg)?.drawableName(component)

    @SuppressLint("DiscouragedApi")
    private fun readDrawableList(pkg: String, res: Resources): List<String> {
        val xmlId = res.getIdentifier("drawable", "xml", pkg)
        if (xmlId != 0) return res.getXml(xmlId).use(::parseDrawableList)
        return context.createPackageContext(pkg, 0).assets.open("drawable.xml").use { stream ->
            parseDrawableList(Xml.newPullParser().apply { setInput(stream, null) })
        }
    }

    @SuppressLint("DiscouragedApi")
    fun drawable(pkg: String, name: String): Drawable? = runCatching {
        val res = packResources(pkg) ?: return null
        val id = res.getIdentifier(name, "drawable", pkg).takeIf { it != 0 } ?: return null
        ResourcesCompat.getDrawable(res, id, null)
    }.getOrNull()

    @Synchronized
    private fun packResources(pkg: String): Resources? = resources.getOrPut(pkg) {
        runCatching { context.packageManager.getResourcesForApplication(pkg) }.getOrNull()
    }

    fun get(pkg: String): IconPack? {
        if (pkg.isBlank()) return null
        var pack: IconPack? = null
        repeat(MAX_PARSE_ATTEMPTS) {
            val before: Int
            synchronized(this) {
                if (cache.containsKey(pkg)) return cache[pkg]
                before = generation.get()
                loading[pkg] = (loading[pkg] ?: 0) + 1
            }
            pack = try {
                runCatching { loader(pkg) }
                    .onFailure { android.util.Log.w("IconPack", "load($pkg) failed: $it") }
                    .getOrNull()
            } finally {
                synchronized(this) {
                    val left = (loading[pkg] ?: 1) - 1
                    if (left <= 0) loading.remove(pkg) else loading[pkg] = left
                }
            }
            synchronized(this) {
                if (cache.containsKey(pkg)) return cache[pkg]
                if (generation.get() == before) {
                    cache[pkg] = pack
                    return pack
                }
            }
        }
        return pack
    }

    private companion object {
        const val MAX_PARSE_ATTEMPTS = 3
        val THEME_ACTIONS = listOf(
            "org.adw.launcher.THEMES",
            "com.novalauncher.THEME",
            "com.gau.go.launcherex.theme",
            "com.dlto.atom.launcher.THEME",
            "ch.deletescape.lawnchair.ICONPACK",
            "app.lawnchair.icons.THEMED_ICONPACK",
        )
        val THEME_CATEGORIES = listOf(
            "com.anddoes.launcher.THEME",
            "com.teslacoilsw.launcher.THEME_ICONPACK",
            "com.fede.launcher.THEME_ICONPACK",
        )
    }
}

private class AppFilterLoader(private val context: Context) {
    fun load(pkg: String): IconPack? {
        val res = context.packageManager.getResourcesForApplication(pkg)
        val xmlId = res.getIdentifier("appfilter", "xml", pkg)
        val parser: XmlPullParser
        val closeable: AutoCloseable
        if (xmlId != 0) {
            val xp = res.getXml(xmlId)
            parser = xp
            closeable = AutoCloseable { xp.close() }
        } else {
            val stream = context.createPackageContext(pkg, 0).assets.open("appfilter.xml")
            parser = Xml.newPullParser().apply { setInput(stream, null) }
            closeable = stream
        }
        val map = HashMap<String, String>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "item") {
                    val comp = componentKey(parser.getAttributeValue(null, "component"))
                    val drawable = parser.getAttributeValue(null, "drawable")
                    if (comp != null && !drawable.isNullOrEmpty() && comp !in map) map[comp] = drawable
                }
                event = parser.next()
            }
        } finally {
            runCatching { closeable.close() }
        }
        return IconPack(pkg, res, map)
    }

    private fun componentKey(raw: String?): String? {
        if (raw == null) return null
        val inner = raw.substringAfter('{', "").substringBefore('}', "")
        return inner.takeIf { it.isNotEmpty() && '/' in inner }
    }
}
