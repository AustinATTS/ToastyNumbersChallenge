package uk.co.austinatts.tnc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class CustomSelectorActivity: AppCompatActivity() {

    private lateinit var ibtn_back: ImageButton
    private lateinit var et_round: EditText
    private lateinit var et_questions: EditText
    private lateinit var et_time: EditText
    private lateinit var btn_play: Button

    private var vibrationsEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom_selector)

        ibtn_back = findViewById(R.id.ibtn_back)
        et_round = findViewById(R.id.et_round)
        et_questions = findViewById(R.id.et_questions)
        et_time = findViewById(R.id.et_time)
        btn_play = findViewById(R.id.btn_play)

        loadPreferences()

        ibtn_back.setOnClickListener {
            vibrateIfEnabled()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btn_play.setOnClickListener {
            vibrateIfEnabled()
            val roundText = et_round.text.toString().trim()
            val questionsText = et_questions.text.toString().trim()
            val timeText = et_time.text.toString().trim()

            if (roundText.isEmpty() || questionsText.isEmpty() || timeText.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val rounds = roundText.toInt()
            val questions = questionsText.toInt()
            val time = timeText.toInt()

            val intent = Intent(this, CustomActivity::class.java).apply {
                putExtra("ROUNDS", rounds)
                putExtra("QUESTIONS", questions)
                putExtra("TIME", time)
            }
            startActivity(intent)
        }
    }

    private fun loadPreferences() {
        val app_settings = getSharedPreferences("app_settings", MODE_PRIVATE)
        vibrationsEnabled = app_settings.getBoolean("vibrationsEnabled", true)
    }

    fun vibrateIfEnabled() {
        if (!vibrationsEnabled) return

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }
}