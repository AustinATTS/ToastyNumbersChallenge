package uk.co.austinatts.tnc

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.findViewTreeViewModelStoreOwner
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlin.math.round

class ResultActivity: AppCompatActivity() {

    private lateinit var tv_timer: TextView
    private lateinit var tv_round: TextView
    private lateinit var tv_round_details: TextView
    private lateinit var tv_questions_details: TextView
    private lateinit var tv_incorrect_details: TextView
    private lateinit var tv_perfect: TextView
    private lateinit var tv_accuracy_details: TextView
    private lateinit var btn_statistics: Button
    private lateinit var btn_play: Button
    private lateinit var btn_home: Button
    private lateinit var btn_share: Button
    private lateinit var stats: SharedPreferences

    private lateinit var fa_auth: FirebaseAuth
    private lateinit var ff_firestore: FirebaseFirestore
    private var vibrationsEnabled: Boolean = true


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        loadPreferences()

        tv_timer = findViewById(R.id.tv_timer)
        tv_round = findViewById(R.id.tv_round)
        tv_round_details = findViewById(R.id.tv_round_details)
        tv_questions_details = findViewById(R.id.tv_questions_details)
        tv_incorrect_details = findViewById(R.id.tv_incorrect_details)
        tv_perfect = findViewById(R.id.tv_perfect)
        tv_accuracy_details = findViewById(R.id.tv_accuracy_details)
        btn_statistics = findViewById(R.id.btn_statistics)
        btn_play = findViewById(R.id.btn_play)
        btn_home = findViewById(R.id.btn_home)
        btn_share = findViewById(R.id.btn_share)
        stats = getSharedPreferences("stats", MODE_PRIVATE)

        fa_auth = FirebaseAuth.getInstance()
        ff_firestore = FirebaseFirestore.getInstance()

        val game_mode = intent.getStringExtra("game_mode")

        if (game_mode == "endless") {
            showEndlessResults()
        } else {
            showPlayResults()
        }

        btn_statistics.setOnClickListener {
            vibrateIfEnabled()
            startActivity(Intent(this, StatisticActivity::class.java))
            finish()
        }

        btn_play.setOnClickListener {
            vibrateIfEnabled()
            play_again()
        }

        btn_home.setOnClickListener {
            vibrateIfEnabled()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        btn_share.setOnClickListener {
            vibrateIfEnabled()
            share()
        }

        loadStats()

    }

    private fun loadPreferences() {
        val app_settings = getSharedPreferences("app_settings", MODE_PRIVATE)
        vibrationsEnabled = app_settings.getBoolean("vibrationsEnabled", true)
    }

    private fun showEndlessResults() {
        val streak = intent.getIntExtra("streak", 0)
        val questions_count = intent.getIntExtra("questions_count", 0)
        val questions_correct = intent.getIntExtra("questions_correct", 0)
        val questions_incorrect = intent.getIntExtra("questions_incorrect", 0)
        val time = intent.getIntExtra("time", 0)

        val questions_answered = questions_count - 1

        val accuracy: Float = (questions_correct.toFloat() / questions_answered.toFloat()) * 100

        val minutes = (time / 60000) % 60
        val seconds = (time / 1000) % 60
        val millis = time % 1000

        val timer = String.format("%02d:%02d:%03d", minutes, seconds, millis)

        tv_round.text = "Highest Streak:"
        tv_round_details.text = streak.toString()
        tv_questions_details.text = questions_answered.toString()
        tv_incorrect_details.text = questions_incorrect.toString()
        tv_accuracy_details.text = accuracy.toInt().toString()
        tv_timer.text = timer

        if (questions_incorrect != 0) {
            tv_perfect.visibility = View.GONE
        }

        analyseResults(questions_answered, questions_correct, questions_incorrect, time, 0, streak)
    }

    private fun showPlayResults() {
        val questions_count = intent.getIntExtra("questions_count", 0)
        val questions_correct = intent.getIntExtra("questions_correct", 0)
        val questions_incorrect = intent.getIntExtra("questions_incorrect", 0)
        val round_number = intent.getIntExtra("round_number", 0)
        val time = intent.getIntExtra("time", 0)

        val accuracy: Float =
            100 - ((questions_incorrect.toFloat() / questions_count.toFloat()) * 100)

        val minutes = (time / 60000) % 60
        val seconds = (time / 1000) % 60
        val millis = time % 1000

        val timer = String.format("%02d:%02d:%03d", minutes, seconds, millis)

        tv_round_details.text = round_number.toString()
        tv_questions_details.text = questions_count.toString()
        tv_incorrect_details.text = questions_incorrect.toString()
        tv_accuracy_details.text = accuracy.toInt().toString()
        tv_timer.text = timer

        if (questions_incorrect != 0) {
            tv_perfect.visibility = View.GONE
        }

        analyseResults(questions_count, (questions_count - questions_incorrect), questions_incorrect, time, round_number, 0)
    }

    private fun share() {
        val questions_count = intent.getIntExtra("questions_count", 0)
        val questions_incorrect = intent.getIntExtra("questions_incorrect", 0)
        val round_number = intent.getIntExtra("round_number", 0)
        val accuracy: Float =
            100 - ((questions_incorrect.toFloat() / questions_count.toFloat()) * 100)
        var round_played = "0"

        if(round_number == 0) {
            round_played = "Endless"
        } else {
            round_played = "$round_number"
        }

        val shareMessage = """
        🔥 I just completed the Toasty Numbers Challenge!
        🧠 Accuracy: $accuracy
        🕹️ Rounds Played: $round_played
        🚀 Can you beat my score?

        Download the game and try it yourself!
        https://play.google.com/store/apps/details?id=com.yourpackage.name
    """.trimIndent()

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareMessage)
        }

        startActivity(Intent.createChooser(shareIntent, "Share your results via"))
    }

    private fun analyseResults(questions_count: Int, questions_correct: Int, questions_incorrect: Int, time: Int, round_number: Int, streak: Int) {
        val account_total_questions = stats.getInt("account_total_questions", 0)
        val account_questions_correct = stats.getInt("account_questions_correct", 0)
        val account_questions_incorrect = stats.getInt("account_questions_incorrect", 0)
        val account_best_time = stats.getInt("account_best_time", 0)
        val account_highest_streak = stats.getInt("account_highest_streak", 0)
        val account_accuracy = stats.getInt("account_accuracy", 0)

        val new_total_questions = account_total_questions + questions_count
        val new_questions_correct = account_questions_correct + questions_correct
        val new_questions_incorrect = account_questions_incorrect + questions_incorrect

        saveStats("account_total_questions", new_total_questions)
        saveStats("account_questions_correct", new_questions_correct)
        saveStats("account_questions_incorrect", new_questions_incorrect)

        if (round_number == 3) {
            if (time > account_best_time) {
                saveStats("account_best_time", time)
            }
        }

        if (streak > account_highest_streak) {
            saveStats("account_highest_streak", streak)
        }

        val accuracy: Float =
            100 - ((new_questions_incorrect.toFloat() / new_total_questions.toFloat()) * 100)

        saveStats("account_accuracy", accuracy.toInt())
    }

    private fun play_again() {
        val game_mode = intent.getStringExtra("game_mode")

        if (game_mode == "endless") {
            startActivity(Intent(this, EndlessActivity::class.java))
            finish()
        } else {
            startActivity(Intent(this, PlayActivity::class.java))
            finish()
        }
    }

    private fun loadStats() {
        val user = fa_auth.currentUser
        if (user != null) {
            ff_firestore.collection("users").document(user.uid)
                .collection("statistics").document("scores")
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val account_total_questions =
                            document.getLong("account_total_questions")?.toInt() ?: 0
                        val account_questions_correct =
                            document.getLong("account_questions_correct")?.toInt() ?: 0
                        val account_questions_incorrect =
                            document.getLong("account_questions_incorrect")?.toInt() ?: 0
                        val account_best_time = document.getLong("account_best_time")?.toInt() ?: 0
                        val account_highest_streak =
                            document.getLong("account_highest_streak")?.toInt() ?: 0
                        val account_accuracy = document.getLong("account_accuracy")?.toInt() ?: 0

                        with (stats.edit()) {
                            putInt("account_total_questions", account_total_questions)
                            putInt("account_questions_correct", account_questions_correct)
                            putInt("account_questions_incorrect", account_questions_incorrect)
                            putInt("account_best_time", account_best_time)
                            putInt("account_highest_streak", account_highest_streak)
                            putInt("account_accuracy", account_accuracy)
                            apply()
                        }
                        Log.d("Auth", "5")
                    } else {
                        Log.d("Auth", "Document does not exist 21")
                        loadSharedPreferences()
                    }
                }
                .addOnFailureListener {
                    Log.d("Auth", "Document does not exist")
                    loadSharedPreferences()
                }
        } else {
            loadSharedPreferences()
        }
    }

    private fun loadSharedPreferences() {
        val account_total_questions = stats.getInt("account_total_questions", 0)
        val account_questions_correct = stats.getInt("account_questions_correct", 0)
        val account_questions_incorrect = stats.getInt("account_questions_incorrect", 0)
        val account_best_time = stats.getInt("account_best_time", 0)
        val account_highest_streak = stats.getInt("account_highest_streak", 0)
        val account_accuracy = stats.getInt("account_accuracy", 0)
    }

    private fun saveStats(stat: String, value: Int) {
        with(stats.edit()) {
            putInt(stat, value)
            apply()
        }

        val user = fa_auth.currentUser
        if (user != null) {
            val statsMap = mapOf(
                stat to value
            )
            ff_firestore.collection("users").document(user.uid)
                .collection("statistics").document("scores")
                .set(statsMap, SetOptions.merge())
        }

        loadStats()
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

