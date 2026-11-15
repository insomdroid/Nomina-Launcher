package com.insomdroid.nomina.model

/** Built-in monochrome rounded-dot pixel glyphs available to the mini-dock capsule. */
object CapsuleGlyphs {
    const val CAMERA = "camera"
    const val SEARCH = "search"
    const val BROWSER = "browser"

    data class Glyph(
        val id: String,
        val label: String,
        val rows: List<String>,
    )

    val all = listOf(
        Glyph(CAMERA, "Camera", listOf("0000000", "0011100", "0111110", "1101011", "1111111", "1100011", "0111110")),
        Glyph(SEARCH, "Search", listOf("0001100", "0010010", "0100001", "0100001", "0100011", "0010110", "0001100")),
        Glyph(BROWSER, "Browser", listOf("0111110", "1100011", "1011101", "1010101", "1011101", "1100011", "0111110")),
        Glyph("music", "Music", listOf("0001100", "0011100", "0001100", "0001100", "0001100", "0111100", "0111000")),
        Glyph("mail", "Mail", listOf("1111111", "1100011", "1010101", "1001001", "1010101", "1100011", "1111111")),
        Glyph("message", "Message", listOf("0111110", "1100011", "1100011", "1100011", "1111111", "0110000", "0011000")),
        Glyph("map", "Map", listOf("1111111", "1101011", "1010101", "1101011", "1010101", "1101011", "1111111")),
        Glyph("calendar", "Calendar", listOf("0111110", "1111111", "1101011", "1101011", "1111111", "1100011", "0111110")),
        Glyph("phone", "Phone", listOf("0011100", "0111110", "0111110", "0111110", "0011100", "0011000", "0011000")),
        Glyph("settings", "Settings", listOf("0001000", "0101010", "0011100", "1111111", "0011100", "0101010", "0001000")),
        Glyph("image", "Image", listOf("1111111", "1000001", "1010101", "1001001", "1011101", "1000001", "1111111")),
        Glyph("video", "Video", listOf("0000000", "1111111", "1100011", "1101111", "1101111", "1100011", "1111111")),
        Glyph("folder", "Folder", listOf("0000000", "0111000", "1111111", "1100011", "1100011", "1111111", "0000000")),
        Glyph("clock", "Clock", listOf("0011100", "0110110", "1100011", "1101011", "1101111", "0110000", "0011100")),
        Glyph("leaf", "Leaf", listOf("0001000", "0011100", "0111110", "1111111", "0011100", "0011000", "0010000")),
        Glyph("notes", "Notes", listOf("0111110", "0100010", "0101010", "0100010", "0101010", "0100010", "0111110")),
    )

    fun find(id: String): Glyph = all.firstOrNull { it.id == id } ?: all.first()
    fun label(id: String): String = find(id).label
}
