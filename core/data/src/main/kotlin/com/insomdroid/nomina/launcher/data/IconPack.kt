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

/** Parsed ADW/Nova appfilter mappings and iconback artwork declarations. */
class IconPack(
    private val packageName: String,
    private val res: Resources,
    private val componentMap: Map<String, String>,
    private val iconBackDrawables: List<String> = emptyList(),
) {
    fun getIcon(component: ComponentName): Drawable? {
        val name = drawableName(component) ?: return null
        val id = res.getIdentifier(name, "drawable", packageName).takeIf { it != 0 } ?: return null
        return runCatching { ResourcesCompat.getDrawable(res, id, null) }.getOrNull()
    }

    fun drawableName(component: ComponentName): String? =
        componentMap["${component.packageName}/${component.className}"]

    fun mappedDrawables(): List<String> = componentMap.values.distinct()

    /** Dominant colour sampled from the centre of the first usable iconback asset. */
    val iconBackColor: Int? by lazy(LazyThreadSafetyMode.PUBLICATION) { readIconBackColor() }

    @SuppressLint("DiscouragedApi")
    private fun readIconBackColor(): Int? {
        val candidates = iconBackDrawables.ifEmpty { listOf("iconback") }
        for (name in candidates) {
            val drawable = loadIconBackDrawable(name) ?: continue
            val color = sampleDominantBackplateColor(drawable)
            if (color != null) return color
        }
        return null
    }

    @SuppressLint("DiscouragedApi")
    private fun loadIconBackDrawable(name: String): Drawable? {
        val id = res.getIdentifier(name, "drawable", packageName).takeIf { it != 0 }
            ?: res.getIdentifier(name, "mipmap", packageName).takeIf { it != 0 }
            ?: return null
        return runCatching { ResourcesCompat.getDrawable(res, id, null) }.getOrNull()
    }

    private fun sampleDominantBackplateColor(drawable: Drawable): Int? = runCatching {
        val size = 64
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        try {
            drawable.setBounds(0, 0, size, size)
            drawable.draw(Canvas(bitmap))

            // Ignore the outer edge: it often contains transparent padding, shadows or antialiasing.
            val buckets = HashMap<Int, ColorBucket>()
            for (y in size / 4 until size * 3 / 4) {
                for (x in size / 4 until size * 3 / 4) {
                    val pixel = bitmap.getPixel(x, y)
                    val alpha = Color.alpha(pixel)
                    if (alpha < 200) continue
                    val red = Color.red(pixel)
                    val green = Color.green(pixel)
                    val blue = Color.blue(pixel)
                    val key = ((red shr 4) shl 8) or ((green shr 4) shl 4) or (blue shr 4)
                    val bucket = buckets.getOrPut(key) { ColorBucket() }
                    bucket.red += red.toLong()
                    bucket.green += green.toLong()
                    bucket.blue += blue.toLong()
                    bucket.count++
                }
            }
            val dominant = buckets.values.maxByOrNull { it.count } ?: return null
            if (dominant.count == 0L) null else Color.rgb(
                (dominant.red / dominant.count).toInt(),
                (dominant.green / dominant.count).toInt(),
                (dominant.blue / dominant.count).toInt(),
            )
        } finally {
            bitmap.recycle()
        }
    }.getOrNull()

    private class ColorBucket {
        var red = 0L
        var green = 0L
        var blue = 0L
        var count = 0L
    }
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

internal fun parseIconBackDrawableList(parser: XmlPullParser): List<String> {
    val names = LinkedHashSet<String>()
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        if (event == XmlPullParser.START_TAG && parser.name == "iconback") {
            for (index in 0 until parser.attributeCount) {
                val attributeName = parser.getAttributeName(index)
                if (attributeName.startsWith("img")) {
                    parser.getAttributeValue(index)?.takeIf { it.isNotBlank() }?.let(names::add)
                }
            }
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
            "com.teslacoilsw.launcher.THEME",
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
        val iconBacks = LinkedHashSet<String>()
        try {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG) {
                    when (parser.name) {
                        "item" -> {
                            val comp = componentKey(parser.getAttributeValue(null, "component"))
                            val drawable = parser.getAttributeValue(null, "drawable")
                            if (comp != null && !drawable.isNullOrEmpty() && comp !in map) map[comp] = drawable
                        }
                        "iconback" -> {
                            for (index in 0 until parser.attributeCount) {
                                if (parser.getAttributeName(index).startsWith("img")) {
                                    parser.getAttributeValue(index)?.takeIf { it.isNotBlank() }?.let(iconBacks::add)
                                }
                            }
                        }
                    }
                }
                event = parser.next()
            }
        } finally {
            runCatching { closeable.close() }
        }
        return IconPack(pkg, res, map, iconBacks.toList())
    }

    private fun componentKey(raw: String?): String? {
        if (raw == null) return null
        val inner = raw.substringAfter('{', "").substringBefore('}', "")
        return inner.takeIf { it.isNotEmpty() && '/' in inner }
    }
}
