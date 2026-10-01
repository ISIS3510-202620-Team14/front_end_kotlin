package com.enad.enadmovil.domain.model

/** Fragmento de texto que devuelve el OCR, con su posición en la foto. */
data class LineaOcr(
    val texto: String,
    val left: Int,
    val top: Int,
    val bottom: Int
)