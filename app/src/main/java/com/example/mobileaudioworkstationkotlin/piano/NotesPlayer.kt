package com.example.mobileaudioworkstationkotlin.piano

import android.content.Context
import android.media.MediaPlayer


class NotesPlayer(private val context: Context?) {
    private val threadMap: HashMap<Int, PlayThread?> = HashMap()
    private var soundBank = "SimplePianoNotes"

    fun playNote(note: Int) {
        val thread = PlayThread(note)
        threadMap[note] = thread
        thread.start()
    }

    fun stopNote(note: Int) {
        val thread = threadMap[note]
        if (thread != null) {
            threadMap.remove(note)
        }
    }

    fun isNotePlaying(note: Int): Boolean {
        return threadMap[note] != null
    }

    fun setSoundBank(value: String) {
        soundBank = value
    }

    private inner class PlayThread(var note: Int) : Thread() {
        var player = MediaPlayer()
        override fun run() {
            try {
                val afd = context?.assets?.openFd("$soundBank/Piano$note.mp3")
                if (afd != null) {
                    player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.getLength())
                }
                player.prepare()
                player.start()
            } catch (e: Exception) {
            }
        }
    }
}

