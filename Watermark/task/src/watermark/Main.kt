package watermark

import java.awt.Color
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import java.lang.NumberFormatException

class ImgFileException( s: String ): Exception(s)

// resulting image
var resImg = BufferedImage(1,1, BufferedImage.TYPE_INT_RGB)

fun main() {

    val im: BufferedImage // Image
    val wm: BufferedImage // Watermark
    try {
        println("Input the image filename:")
        im = readFile("image")
        println("Input the watermark image filename:")
        wm = readFile("watermark")
    } catch (e: ImgFileException) { return }

    // check if watermark is bigger than image
    if (im.width < wm.width || im.height < wm.height) {
        println("The watermark's dimensions are larger.")
        return
    }

    var useAlfa = false // should we use watermark's alfa channel
    var useBGColor = false // should we use a background color as translucent
    var tClr = Color(0,0, 0) // transparency color in watermark
    if (wm.transparency == Transparency.TRANSLUCENT) {
        println("Do you want to use the watermark's Alpha channel?")
        useAlfa = readln().equals("yes", true)
    } else {
        println("Do you want to set a transparency color?")
        if (readln().equals("yes", true)) {
            println("Input a transparency color ([Red] [Green] [Blue]):")
            val clrs = readln().split(" ")
            // only 3 colors accepted
            if (clrs.size != 3) {
                println("The transparency color input is invalid.")
                return
            }
            val tRed: Int
            val tGreen: Int
            val tBlue: Int
            // check if int numbers were input
            try {
                tRed = clrs[0].toInt()
                tGreen = clrs[1].toInt()
                tBlue = clrs[2].toInt()
            } catch (e: NumberFormatException) {
                println("The transparency color input is invalid.")
                return
            }

            // check if numbers are in range
            if( !(tRed in 0..255 && tGreen in 0..255 && tBlue in 0..255)) {
                println("The transparency color input is invalid.")
                return
            }
            tClr = Color(tRed, tGreen, tBlue)
            useBGColor = true
        }
    }

    // weight of watermark
    val weight: Int
    println("Input the watermark transparency percentage (Integer 0-100):")
    try {
        weight = readln().toInt()
    } catch (e: NumberFormatException) {
        println("The transparency percentage isn't an integer number.")
        return
    }

    if (!(weight in 0..100)) {
        println("The transparency percentage is out of range.")
        return
    }

    // should we put a single watermark (else we put grid)
    var isWMSingle = true
    var xWM = 0
    var yWM = 0
    println("Choose the position method (single, grid):")
    when (readln().lowercase()) {
        "single" -> {
            println("Input the watermark position ([x 0-${im.width-wm.width}] [y 0-${im.height-wm.height}]):")
            val inp = readln().split(" ")
            if (inp.size != 2) {
                println("The position input is invalid.")
                return
            }
            try {
                xWM = inp[0].toInt()
                yWM = inp[1].toInt()
            } catch (e: NumberFormatException) {
                println("The position input is invalid.")
                return
            }
            if ( !( xWM in 0..(im.width-wm.width) && yWM in 0..(im.height-wm.height) ) ){
                println("The position input is out of range.")
                return
            }
        }
        "grid" -> isWMSingle = false
        else -> {
            println("The position method input is invalid.")
            return
        }
    }

    println("Input the output image filename (jpg or png extension):")
    val outFileName = readln()
    if ( !( outFileName.endsWith(".jpg", true) || outFileName.endsWith(".png", true ) ) ) {
        println("The output file extension isn't \"jpg\" or \"png\".")
        return
    }


    resImg = im // ???
    if(isWMSingle) {
        if(useAlfa) putTransLucidWM(im, wm, weight, xWM, yWM)
        else if (useBGColor) putTransColorWM(im, wm, tClr, weight, xWM, yWM)
        else putOpaqueWM(im, wm, weight, xWM, yWM)
    } else {
        for (x in 0 until im.width step wm.width) {
            for (y in 0 until im.height step wm.height) {
                if(useAlfa) putTransLucidWM(resImg, wm, weight, x, y)
                else if (useBGColor) putTransColorWM(resImg, wm, tClr, weight, x, y)
                else putOpaqueWM(resImg, wm, weight, x, y)
            }
        }
    }

    val outFile = File(outFileName)
    ImageIO.write(resImg, outFileName.substring(outFileName.length - 3), outFile)
    println("The watermarked image $outFileName has been created.")
}

fun readFile(str: String): BufferedImage {

    val fileName = readln()
    val file = File(fileName)
    if (!file.exists()) {
        println("The file $fileName doesn't exist.")
        throw ImgFileException("")
    }

    val im: BufferedImage = ImageIO.read(file)
    if (im.colorModel.numComponents < 3) {
        println("The number of $str color components isn't 3.")
        throw ImgFileException("")
    }
    if (im.colorModel.pixelSize != 24 && im.colorModel.pixelSize != 32) {
        println("The $str isn't 24 or 32-bit.")
        throw ImgFileException("")
    }
    return im
}

fun putOpaqueWM(im: BufferedImage, wm: BufferedImage, weight: Int, xWM: Int, yWM: Int) {

    for (x in xWM until Math.min(im.width, xWM+wm.width)) {
        for (y in yWM until Math.min(im.height, yWM+wm.height)) {
            val i = Color(im.getRGB(x, y))
            val w = Color(wm.getRGB(x-xWM, y-yWM))
            val color = Color(
                (weight * w.red + (100 - weight) * i.red) / 100,
                (weight * w.green + (100 - weight) * i.green) / 100,
                (weight * w.blue + (100 - weight) * i.blue) / 100
            )
            resImg.setRGB(x, y, color.rgb)
        }
    }
}

fun putTransLucidWM(im: BufferedImage, wm: BufferedImage, weight: Int, xWM: Int, yWM: Int) {

    for (x in xWM until Math.min(im.width, xWM+wm.width)) {
        for (y in yWM until Math.min(im.height, yWM+wm.height)) {
            val i = Color(im.getRGB(x, y))
            val w = Color(wm.getRGB(x-xWM, y-yWM), true)
            val color: Color

            color = if (w.alpha == 0) {
                Color(i.red, i.green, i.blue)
            } else /*if (w.alpha == 255)*/ {
                Color(
                    (weight * w.red + (100 - weight) * i.red) / 100,
                    (weight * w.green + (100 - weight) * i.green) / 100,
                    (weight * w.blue + (100 - weight) * i.blue) / 100
                )
            }
            resImg.setRGB(x, y, color.rgb)
        }
    }
}

fun putTransColorWM(im: BufferedImage, wm: BufferedImage, tClr: Color, weight: Int, xWM: Int, yWM: Int) {

    for (x in xWM until Math.min(im.width, xWM+wm.width)) {
        for (y in yWM until Math.min(im.height, yWM+wm.height)) {
            val i = Color(im.getRGB(x, y))
            val w = Color(wm.getRGB(x-xWM, y-yWM))
            val color: Color

            color = if (w.red == tClr.red && w.green == tClr.green && w.blue == tClr.blue) {
                Color(i.red, i.green, i.blue)
            } else /*if (w.alpha == 255)*/ {
                Color(
                    (weight * w.red + (100 - weight) * i.red) / 100,
                    (weight * w.green + (100 - weight) * i.green) / 100,
                    (weight * w.blue + (100 - weight) * i.blue) / 100
                )
            }
            resImg.setRGB(x, y, color.rgb)
        }
    }
}