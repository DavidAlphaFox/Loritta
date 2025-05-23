package net.perfectdreams.loritta.website.backend.utils

import kotlinx.html.*
import net.perfectdreams.etherealgambi.data.DefaultImageVariantPreset
import net.perfectdreams.etherealgambi.data.ScaleDownToWidthImageVariantPreset
import net.perfectdreams.etherealgambi.data.api.responses.ImageVariantsResponse
import net.perfectdreams.loritta.website.backend.LorittaWebsiteBackend

fun FlowOrInteractiveOrPhrasingContent.imgSrcSetFromEtherealGambi(m: LorittaWebsiteBackend, preloadedImageInfo: EtherealGambiImages.PreloadedImageInfo, extension: String, sizes: String, block: IMG.() -> Unit = {}) {
    val variantInfo = preloadedImageInfo.imageInfo

    val defaultVariant = variantInfo.variants.first { it.preset is DefaultImageVariantPreset }
    val scaleDownVariants = variantInfo.variants.filter { it.preset is ScaleDownToWidthImageVariantPreset }

    val imageUrls = (
            scaleDownVariants.map {
                "${m.etherealGambiClient.baseUrl}/${it.urlWithoutExtension}.$extension ${(it.preset as ScaleDownToWidthImageVariantPreset).width}w"
            } + "${m.etherealGambiClient.baseUrl}/${defaultVariant.urlWithoutExtension}.$extension ${variantInfo.imageInfo.width}w"
            ).joinToString(", ")

    imgSrcSet(
        "${m.etherealGambiClient.baseUrl}/${defaultVariant.urlWithoutExtension.removePrefix("/")}.$extension",
        sizes,
        imageUrls
    ) {
        block()
        style += "aspect-ratio: ${variantInfo.imageInfo.width}/${variantInfo.imageInfo.height}"
    }
}


fun FlowOrInteractiveOrPhrasingContent.imgSrcSetFromEtherealGambi(m: LorittaWebsiteBackend, variantInfo: ImageVariantsResponse, extension: String, sizes: String, block: IMG.() -> Unit = {}) {
    val defaultVariant = variantInfo.variants.first { it.preset is DefaultImageVariantPreset }
    val scaleDownVariants = variantInfo.variants.filter { it.preset is ScaleDownToWidthImageVariantPreset }

    val imageUrls = (
            scaleDownVariants.map {
                "${m.etherealGambiClient.baseUrl}/${it.urlWithoutExtension}.$extension ${(it.preset as ScaleDownToWidthImageVariantPreset).width}w"
            } + "${m.etherealGambiClient.baseUrl}/${defaultVariant.urlWithoutExtension}.$extension ${variantInfo.imageInfo.width}w"
            ).joinToString(", ")

    imgSrcSet(
        "${m.etherealGambiClient.baseUrl}/${defaultVariant.urlWithoutExtension}.$extension",
        sizes,
        imageUrls
    ) {
        block()
        style += "aspect-ratio: ${variantInfo.imageInfo.width}/${variantInfo.imageInfo.height}"
    }
}

// Generates Image Sets
fun DIV.imgSrcSet(path: String, fileName: String, sizes: String, max: Int, min: Int, stepInt: Int, block : IMG.() -> Unit = {}) {
    val srcsets = mutableListOf<String>()
    val split = fileName.split(".")
    val ext = split.last()

    for (i in (max - stepInt) downTo min step stepInt) {
        // "${websiteUrl}$versionPrefix/assets/img/home/lori_gabi.png 1178w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_1078w.png 1078w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_978w.png 978w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_878w.png 878w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_778w.png 778w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_678w.png 678w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_578w.png 578w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_478w.png 478w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_378w.png 378w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_278w.png 278w, ${websiteUrl}$versionPrefix/assets/img/home/lori_gabi_178w.png 178w"
        srcsets.add("$path${split.first()}_${i}w.$ext ${i}w")
    }

    srcsets.add("$path$fileName ${max}w")

    imgSrcSet(
        "$path$fileName",
        sizes,
        srcsets.joinToString(", "),
        block
    )
}

fun FlowOrInteractiveOrPhrasingContent.imgSrcSet(filePath: String, sizes: String, srcset: String, block : IMG.() -> Unit = {})  {
    img(src = filePath) {
        style = "width: auto;"
        attributes["sizes"] = sizes
        attributes["srcset"] = srcset

        this.apply(block)
    }
}

fun DIV.adWrapper(svgIconManager: SVGIconManager, callback: DIV.() -> Unit) {
    // Wraps the div in a nice wrapper
    div {
        style = "text-align: center;"
        fieldSet {
            style = "display: inline;\n" +
                    "border: 2px solid rgba(0,0,0,.05);\n" +
                    "border-radius: 7px;\n" +
                    "color: rgba(0,0,0,.3);"

            legend {
                style = "margin-left: auto;"
                svgIconManager.ad.apply(this)
            }

            div {
                callback.invoke(this)
            }
        }
    }
}

fun DIV.mediaWithContentWrapper(
    mediaOnTheRightSide: Boolean,
    mediaFigure: DIV.() -> (Unit),
    mediaBody: DIV.() -> (Unit),
) {
    div(classes = "media") {
        if (mediaOnTheRightSide) {
            div(classes = "media-body") {
                mediaBody.invoke(this)
            }

            div(classes = "media-figure") {
                mediaFigure.invoke(this)
            }
        } else {
            div(classes = "media-figure") {
                mediaFigure.invoke(this)
            }

            div(classes = "media-body") {
                mediaBody.invoke(this)
            }
        }
    }
}

fun DIV.innerContent(block: DIV.() -> (Unit)) = div {
    id = "inner-content"

    div(classes = "background-overlay") {}

    block.invoke(this)
}

fun DIV.generateAd(adSlot: String, adsenseAdClass: String? = null) {
    div(classes = "centralized-ad") {
        ins(classes = "adsbygoogle") {
            if (adsenseAdClass != null)
                classes += adsenseAdClass

            style = "display: block;"

            attributes["data-ad-client"] = "ca-pub-9989170954243288"
            attributes["data-ad-slot"] = adSlot
            attributes["data-ad-format"] = "auto"
            attributes["data-full-width-responsive"] = "true"
        }
    }

    script(type = ScriptType.textJavaScript) {
        unsafe {
            raw("(adsbygoogle = window.adsbygoogle || []).push({});")
        }
    }
}

fun Tag.enableHarmonyProgressBarOnLoad() {
    attributes["harmony-progress-bar"] = "true"
}