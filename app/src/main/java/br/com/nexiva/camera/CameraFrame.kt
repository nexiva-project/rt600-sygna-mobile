package br.com.nexiva.camera

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy

/**
 * Converte um quadro cru da câmera num bitmap em pé, já cortado no que a tela mostra.
 * Não detecta nada. Só deixa a imagem pronta para o modelo.
 */
fun ImageProxy.toUprightBitmap(): Bitmap {
    val plane = planes[0]
    val buffer = plane.buffer
    val rowPadding = plane.rowStride - plane.pixelStride * width
    val paddedWidth = width + rowPadding / plane.pixelStride
    val raw = Bitmap.createBitmap(paddedWidth, height, Bitmap.Config.ARGB_8888)
    buffer.rewind()
    raw.copyPixelsFromBuffer(buffer)

    val crop = cropRect
    val left = crop.left.coerceIn(0, width - 1)
    val top = crop.top.coerceIn(0, height - 1)
    val cropWidth = crop.width().coerceIn(1, width - left)
    val cropHeight = crop.height().coerceIn(1, height - top)
    val cropped = Bitmap.createBitmap(raw, left, top, cropWidth, cropHeight)
    if (cropped != raw) raw.recycle()

    val rotation = imageInfo.rotationDegrees
    if (rotation == 0) return cropped
    val upright = Bitmap.createBitmap(
        cropped,
        0,
        0,
        cropped.width,
        cropped.height,
        Matrix().apply { postRotate(rotation.toFloat()) },
        true,
    )
    if (upright != cropped) cropped.recycle()
    return upright
}
