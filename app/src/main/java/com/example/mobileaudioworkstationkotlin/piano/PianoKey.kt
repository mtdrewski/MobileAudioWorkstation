package com.example.mobileaudioworkstationkotlin.piano

import android.graphics.RectF

class PianoKey(var rect: RectF, var soundID: Int, var keyColour: Colour) {
    enum class Colour {
        BLACK,
        WHITE
    }

    var isPressed = false
}

