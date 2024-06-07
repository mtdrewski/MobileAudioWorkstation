package com.example.mobileaudioworkstationkotlin.piano

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Handler
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.example.mobileaudioworkstationkotlin.bluetooth.BluetoothController


class PianoView(context: Context?, attrs: AttributeSet?) :
    View(context, attrs) {
    private val pianoKeys: ArrayList<PianoKey> = ArrayList()
    private var PianoKeyWidth = 0
    private var height = 0
    private var soundPlayer: NotesPlayer

    init {
        soundPlayer = NotesPlayer(context)
    }

    private lateinit var connectingThread: BluetoothController.ConnectingThread
    fun setConnectingThread(thread: BluetoothController.ConnectingThread){
        connectingThread = thread
    }

    fun setSoundBank(soundBank: String){
        soundPlayer.setSoundBank(soundBank)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        PianoKeyWidth = w / notesNumber
        height = h
        var soundID = 0
        for (i in 0 until notesNumber) {
            val whiteLeft = i * PianoKeyWidth
            val right = whiteLeft + PianoKeyWidth
            var rect = RectF(whiteLeft.toFloat(), 0f, right.toFloat(), h.toFloat())
            pianoKeys.add(PianoKey(rect, soundID, PianoKey.Colour.WHITE))
            soundID++
            if (i % 7 != 2 && i % 7 != 6) {
                val blackLeft = whiteLeft + PianoKeyWidth * 0.7f
                val blackRight = blackLeft + PianoKeyWidth * 0.6f
                rect = RectF(blackLeft, 0f, blackRight, 0.67f * height)
                pianoKeys.add(PianoKey(rect, soundID, PianoKey.Colour.BLACK))
                soundID++
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        for (k in pianoKeys) {
            if (k.keyColour === PianoKey.Colour.WHITE) {
                (if (k.isPressed) gray else white)?.let { canvas.drawRect(k.rect, it) }
            }
        }
        for (i in 1 until notesNumber) {
            canvas.drawLine(
                (i * PianoKeyWidth).toFloat(), 0f, (i * PianoKeyWidth).toFloat(), height.toFloat(),
                black!!
            )
        }
        for (k in pianoKeys) {
            if (k.keyColour === PianoKey.Colour.BLACK) {
                (if (k.isPressed) gray else black)?.let { canvas.drawRect(k.rect, it) }
            }
        }
    }

    private fun PianoKeyInCoords(x: Float, y: Float): PianoKey? {
        for (k in pianoKeys) {
            if (k.rect.contains(x, y)) {
                return k
            }
        }
        return null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val action = event.action
        if (action != MotionEvent.ACTION_DOWN && action != MotionEvent.ACTION_MOVE) {
            return true
        }
        val changedKeys: HashSet<PianoKey?> = HashSet<PianoKey?>()
        for (touchIndex in 0 until event.pointerCount) {
            val x = event.getX(touchIndex)
            val y = event.getY(touchIndex)
            val k: PianoKey? = PianoKeyInCoords(x, y)
            changedKeys.add(k)
        }
        for (key in changedKeys) {
            if (key != null && !key.isPressed) {
                key.isPressed = true
                if (!soundPlayer.isNotePlaying(key.soundID)) {
                    soundPlayer.playNote(key.soundID)
                    Handler().postDelayed({
                        key.isPressed = false
                        soundPlayer.stopNote(key.soundID)
                        invalidate()
                    }, 200)

                    if(connectingThread.isAlive) {
                        connectingThread.write(key.soundID)
                    }
                }
                invalidate()
            }
        }
        return true
    }

    companion object {
        const val notesNumber = 14
        private var black: Paint? = null
        private var gray: Paint? = null
        private var white: Paint? = null

        init {
            black = Paint()
            black!!.setColor(Color.BLACK)
            white = Paint()
            white!!.setColor(Color.WHITE)
            gray = Paint()
            gray!!.setColor(Color.GRAY)
        }
    }
}