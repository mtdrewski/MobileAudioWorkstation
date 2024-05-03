package com.example.mobileaudioworkstationkotlin

import android.Manifest
import android.content.pm.PackageManager
import android.icu.text.SimpleDateFormat
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mobileaudioworkstationkotlin.recorder.AudioPlayer
import com.example.mobileaudioworkstationkotlin.recorder.AudioRecorder
import java.io.File
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val recorder by lazy {
        AudioRecorder(applicationContext)
    }
    private val player by lazy {
        AudioPlayer(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setButtons()
    }

    private val startRecordButton: Button by lazy {
        findViewById(R.id.startRecordButton)
    }
    private val stopRecordButton: Button by lazy {
        findViewById(R.id.stopRecordButton)
    }
    private val playRecordButton: Button by lazy {
        findViewById(R.id.playRecordButton)
    }

    private fun setButtons(){
        if(!isRecordAudioPermissionGranted()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                0
            )
        }
        setButtonsEnabled(false)

        val audioCapturesDirectory = File(getExternalFilesDir(null), "/AudioCaptures")
        var bufferedFile: File? = null
        startRecordButton.setOnClickListener {
            if (!audioCapturesDirectory.exists()) {
                audioCapturesDirectory.mkdirs()
            }
            bufferedFile = File(audioCapturesDirectory.absolutePath + "/buffer.aac")
            recorder.start(bufferedFile!!)
            setButtonsEnabled(true)
        }

        val input = EditText(this)
        input.setHint("Please name your recording")
        val saveRecordingDialog = AlertDialog.Builder(this)
            .setTitle("Do you want to save the recording?")
            .setView(input)
            .setPositiveButton("Yes") { _,_ ->
                val recordingName = if(input.text.isEmpty()){
                    val timestamp = SimpleDateFormat("dd-MM-yyyy-hh-mm-ss", Locale.UK).format(Date())
                    "Capture-$timestamp.aac"
                }  else {
                    input.text.toString()+".aac"
                }

                val renamedFile = File(audioCapturesDirectory.absolutePath+"/"+recordingName)
                bufferedFile?.renameTo(renamedFile)

                Toast.makeText(this, "The recording was saved as $recordingName",Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No"){_,_ ->
                Toast.makeText(this, "The recording was discarded",Toast.LENGTH_SHORT).show()
            }.create()

        stopRecordButton.setOnClickListener {
            recorder.stop()
            saveRecordingDialog.show()
            setButtonsEnabled(false)
        }

        playRecordButton.setOnClickListener {
            val options = audioCapturesDirectory.listFiles()?.map {
                    fileName -> fileName.path.substring(fileName.path.lastIndexOf('/')+1) }
                ?.toTypedArray()

            var audioFile: File? = null
            val singleChoiceDialog = AlertDialog.Builder(this)
                .setTitle("Choose the recording you want to play")
                .setSingleChoiceItems(options,0){_, i ->
                    audioFile = File(audioCapturesDirectory.absolutePath+"/"+options?.get(i))
                }.setNegativeButton("Return"){_,_->
                }.setPositiveButton("Play") { _,_ ->
                    audioFile?.let { it1 ->
                        player.playFile(
                            it1
                        )
                    }
                }.create()
            singleChoiceDialog.show()
        }
    }

    private fun setButtonsEnabled(isCapturingAudio: Boolean){
        startRecordButton.isEnabled = !isCapturingAudio
        stopRecordButton.isEnabled = isCapturingAudio
    }

    private fun isRecordAudioPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }

}