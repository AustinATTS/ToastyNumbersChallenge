package uk.co.austinatts.tnc

import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import com.google.firebase.auth.FirebaseAuth
import com.robinhood.ticker.TickerUtils
import com.robinhood.ticker.TickerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class EndlessActivity : AppCompatActivity(){

    private lateinit var ibtn_back: ImageButton
    private lateinit var tv_header_solved: TextView
    private lateinit var tkv_number_one: TickerView
    private lateinit var tkv_number_two: TickerView
    private lateinit var tkv_operator: TickerView
    private lateinit var et_answer: EditText
    private lateinit var v_error: View
    private lateinit var iv_error: ImageView
    private lateinit var tv_error: TextView
    private lateinit var fa_auth: FirebaseAuth

    private var questions_count: Int = 0
    private var answer_correct: Int = 0
    private var questions_correct: Int = 0
    private var questions_incorrect: Int = 0
    private var autoAnswerEnabled: Boolean = true
    private var soundEnabled: Boolean = true
    private var vibrationsEnabled: Boolean = true
    private var time: Int = 0
    private var timer_running: Boolean = false
    private var timer_job: Job? = null
    private var streak_count: Int = 0
    private var streak: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_endless)

        ibtn_back = findViewById(R.id.ibtn_back)
        tv_header_solved = findViewById(R.id.tv_header_solved)
        tkv_number_one = findViewById(R.id.tkv_number_one)
        tkv_number_two = findViewById(R.id.tkv_number_two)
        tkv_operator = findViewById(R.id.tkv_operator)
        et_answer = findViewById(R.id.et_answer)
        v_error = findViewById(R.id.v_error)
        iv_error = findViewById(R.id.iv_error)
        tv_error = findViewById(R.id.tv_error)

        fa_auth = FirebaseAuth.getInstance()

        tkv_number_one.setCharacterLists(TickerUtils.provideNumberList())
        tkv_number_two.setCharacterLists(TickerUtils.provideNumberList())
        tkv_operator.setCharacterLists(TickerUtils.provideAlphabeticalList())

        tkv_number_one.setAnimationDuration(1500)
        tkv_number_two.setAnimationDuration(1500)
        tkv_operator.setAnimationDuration(1500)

        tkv_number_one.typeface = ResourcesCompat.getFont(this, R.font.selawik_bold)
        tkv_number_two.typeface = ResourcesCompat.getFont(this, R.font.selawik_bold)
        tkv_operator.typeface = ResourcesCompat.getFont(this, R.font.selawik_bold)

        tv_header_solved.text = questions_correct.toString()

        startBackgroundTimer()
        loadPreferences()

        ibtn_back.setOnClickListener {
            vibrateIfEnabled()
            showResults()
        }

        et_answer.addTextChangedListener(object : TextWatcher {
            private var delayJob: Job? = null

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            }

            override fun afterTextChanged(s: Editable?) {
                val answer_user = s.toString()
                delayJob?.cancel()

                if (autoAnswerEnabled) {
                    if (answer_user.isNotEmpty() && answer_user.length >= answer_correct.toString().length) {
                        delayJob = CoroutineScope(Dispatchers.Main).launch {
                            delay(250)
                            checkAnswer(answer_user.toInt())
                        }
                    }
                }
            }
        })

        et_answer.setOnEditorActionListener { _, actionId, _ ->
            if (!autoAnswerEnabled && actionId == EditorInfo.IME_ACTION_DONE) {
                val answer_user = et_answer.text.toString()
                if (answer_user.isNotEmpty()) {
                    checkAnswer(answer_user.toInt())
                }
                true
            } else {
                false
            }
        }

        generateQuestions()
    }

    private fun startBackgroundTimer() {
        timer_running = true
        timer_job = CoroutineScope(Dispatchers.Main).launch {
            while (timer_running) {
                delay(1)
                time++
            }
        }
    }

    private fun loadPreferences() {
        val app_settings = getSharedPreferences("app_settings", MODE_PRIVATE)
        autoAnswerEnabled = app_settings.getBoolean("autoAnswerEnabled", true)
        soundEnabled = app_settings.getBoolean("soundEnabled", true)
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

    private fun showResults() {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("game_mode", "endless")
        intent.putExtra("streak", streak_count)
        intent.putExtra("questions_count", questions_count)
        intent.putExtra("questions_correct", questions_correct)
        intent.putExtra("questions_incorrect", questions_incorrect)
        intent.putExtra("time", time)
        startActivity(intent)
        finish()
    }

    private fun checkAnswer(answer_user: Int) {
        vibrateIfEnabled()
        if (answer_user == answer_correct) {
            playSound("correct")
            questions_correct += 1
            streak += 1
            if (streak > streak_count) {
                streak_count = streak
            }
            updateCount(questions_correct)
            generateQuestions()
        } else {
            playSound("incorrect")
            streak = 0
            questions_incorrect += 1
            showErrorFeedback()
            et_answer.text.clear()
            generateQuestions()
        }
    }

    private fun playSound(type: String) {
        if (!soundEnabled) return

        val soundResId = when (type) {
            "correct" -> R.raw.correct
            "incorrect" -> R.raw.incorrect
            else -> return
        }

        val mediaPlayer = MediaPlayer.create(this, soundResId)
        mediaPlayer?.start()
        mediaPlayer?.setOnCompletionListener { it.release() }
    }

    private fun updateCount(questions_correct: Int) {
        tv_header_solved.text = questions_correct.toString()
    }

    private fun generateQuestions() {
        questions_count += 1
        var number_one = (1..12).random()
        var number_two = (1..12).random()
        val operator = listOf("+", "−", "×", "÷").random()

        val result = when (operator) {
            "+" -> number_one + number_two
            "−" -> {
                var result: Int
                do {
                    number_one = (1..12).random()
                    number_two = (1..12).random()
                    result = number_one - number_two
                } while (result < 0 || number_one > 12)
                result
            }
            "×" -> number_one * number_two
            "÷" -> {
                var result: Int
                do {
                    number_one = (1..12).random()
                    number_two = (1..12).random()
                    result = number_one / number_two
                } while (number_one % number_two != 0 || number_one > 12)
                result
            }
            else -> 0
        }

        answer_correct = result

        tkv_number_one.setText(number_one.toString())
        tkv_number_two.setText(number_two.toString())
        tkv_operator.setText(operator)

        et_answer.text.clear()
    }

    private fun showErrorFeedback() {
        v_error.visibility = View.VISIBLE
        iv_error.visibility = View.VISIBLE

        tv_error.text = answer_correct.toString()
        tv_error.visibility = View.VISIBLE

        v_error.animate().alpha(1.0f).duration = 500
        iv_error.animate().alpha(1.0f).duration = 500
        tv_error.animate().alpha(1.0f).duration = 500

        Handler(Looper.getMainLooper()).postDelayed({
            v_error.animate().alpha(0.0f).setDuration(500).withEndAction {
                v_error.visibility = View.GONE
            }
            iv_error.animate().alpha(0.0f).setDuration(500).withEndAction {
                iv_error.visibility = View.GONE
            }
            tv_error.animate().alpha(0.0f).setDuration(500).withEndAction {
                tv_error.visibility - View.GONE
            }
        }, 1500)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopBackgroundTimer()
    }

    private fun stopBackgroundTimer() {
        timer_running = false
        timer_job?.cancel()
    }

}