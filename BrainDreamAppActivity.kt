package com.yourname.braindreamapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.speech.tts.TextToSpeech
import org.brainflow.BoardShim
import org.brainflow.devices.eeg.Emotiv
import org.brainflow.DataFilter
import java.util.*

class BrainDreamAppActivity : AppCompatActivity() {

    lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts.language = Locale("ar")
            }
        }

        val boardId = Emotiv.getBoardId()
        BoardShim.enable_board_logger()
        val boardShim = BoardShim(boardId, "{}")
        boardShim.prepare_session()
        boardShim.start_stream()

        Thread {
            Thread.sleep(10000) // تسجيل نشاط دماغي قصير
            val data = boardShim.get_board_data()
            val delta = DataFilter.get_band_power(data, boardId, "AF3", 0.5, 4.0)
            val theta = DataFilter.get_band_power(data, boardId, "AF3", 4.0, 8.0)
            val alpha = DataFilter.get_band_power(data, boardId, "AF3", 8.0, 13.0)
            val beta  = DataFilter.get_band_power(data, boardId, "AF3", 13.0, 30.0)
            val gamma = DataFilter.get_band_power(data, boardId, "AF3", 30.0, 100.0)

            val powers = mapOf("دلتا" to delta, "ثيتا" to theta, "ألفا" to alpha, "بيتا" to beta, "جاما" to gamma)
            val maxState = powers.maxByOrNull { it.value }?.key ?: "غير محدد"

            runOnUiThread {
                tts.speak("الحالة العقلية الحالية: $maxState", TextToSpeech.QUEUE_FLUSH, null, "")
            }

            Thread.sleep(5000) // تسجيل النوم (مسجل أحلام)
            val sleepData = boardShim.get_board_data()
            val sleepDelta = DataFilter.get_band_power(sleepData, boardId, "AF3", 0.5, 4.0)
            val sleepTheta = DataFilter.get_band_power(sleepData, boardId, "AF3", 4.0, 8.0)

            val sleepStage = when {
                sleepDelta > sleepTheta -> "نوم عميق"
                sleepTheta > sleepDelta -> "نوم أحلام (REM)"
                else -> "نوم خفيف"
            }

            runOnUiThread {
                tts.speak("مرحلة نومك: $sleepStage", TextToSpeech.QUEUE_ADD, null, "")
            }

            boardShim.stop_stream()
            boardShim.release_session()
        }.start()
    }
}
