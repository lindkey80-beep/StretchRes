package com.stretchres.app

data class ResolutionPreset(
    val name: String,
    val width: Int,
    val height: Int,
    val dpi: Int,
    val description: String
) {
    override fun toString() = name
}

object Presets {
    val list = listOf(
        ResolutionPreset("4:3 Stretched (1280x960)", 1280, 960, 300, "Classic 4:3 — wider enemies"),
        ResolutionPreset("4:3 Stretched (960x720)", 960, 720, 240, "Medium 4:3 — good FPS boost"),
        ResolutionPreset("4:3 Stretched (854x640)", 854, 640, 213, "Low 4:3 — max FPS boost"),
        ResolutionPreset("5:4 Stretched (1280x1024)", 1280, 1024, 300, "5:4 — slight stretch"),
        ResolutionPreset("16:10 Balanced (1280x800)", 1280, 800, 280, "16:10 — mild wide"),
        ResolutionPreset("720p Normal (1280x720)", 1280, 720, 240, "Standard 720p"),
        ResolutionPreset("540p Performance (960x540)", 960, 540, 200, "High FPS / Low res"),
        ResolutionPreset("480p Ultra Perf (854x480)", 854, 480, 160, "Max FPS mode"),
    )
}
