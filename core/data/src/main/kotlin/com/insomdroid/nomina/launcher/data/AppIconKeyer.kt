package com.insomdroid.nomina.data

import coil3.key.Keyer
import coil3.request.Options
import com.insomdroid.nomina.model.IconRequest

class AppIconKeyer : Keyer<IconRequest> {
    // Every input that changes the resolved artwork or its mask belongs in the cache key.
    // The package epoch invalidates updated app icons; the custom-icon reference invalidates picks.
    override fun key(data: IconRequest, options: Options): String =
        "appicon:${data.app.key}:p=${data.iconPack}:e=${data.epoch}:m=${data.reshapeLegacyIcons}:s=${data.iconShape}" +
            ":c=${data.app.customIcon?.encode().orEmpty()}:r=${data.autoResize}"
}
