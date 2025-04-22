package uk.co.austinatts.tnc

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StatisticActivity: AppCompatActivity() {


    private lateinit var stats: SharedPreferences
    private lateinit var fa_auth: FirebaseAuth
    private lateinit var ff_firestore: FirebaseFirestore
    private var vibrationsEnabled: Boolean = true


    private lateinit var tv_time: TextView
    private lateinit var tv_questions: TextView
    private lateinit var tv_correct: TextView
    private lateinit var tv_incorrect: TextView
    private lateinit var tv_accuracy: TextView
    private lateinit var tv_streak: TextView
    private lateinit var ibtn_back: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistic)

        stats = getSharedPreferences("stats", MODE_PRIVATE)
        fa_auth = FirebaseAuth.getInstance()
        ff_firestore = FirebaseFirestore.getInstance()

        tv_time = findViewById(R.id.tv_time)
        tv_questions = findViewById(R.id.tv_questions)
        tv_correct = findViewById(R.id.tv_correct)
        tv_incorrect = findViewById(R.id.tv_incorrect)
        tv_accuracy = findViewById(R.id.tv_accuracy)
        tv_streak = findViewById(R.id.tv_streak)
        ibtn_back = findViewById(R.id.ibtn_back)

        loadStats()
        loadSharedPreferences()
        loadPreferences()

        ibtn_back.setOnClickListener {
            vibrateIfEnabled()
            startActivity(Intent(this, MainActivity::class.java))
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

        tv_time.text = account_best_time.toString()
        tv_questions.text = account_total_questions.toString()
        tv_correct.text = account_questions_correct.toString()
        tv_incorrect.text = account_questions_incorrect.toString()
        tv_accuracy.text = account_accuracy.toString()
        tv_streak.text = account_highest_streak.toString()
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

    private fun loadPreferences() {
        val app_settings = getSharedPreferences("app_settings", MODE_PRIVATE)
        vibrationsEnabled = app_settings.getBoolean("vibrationsEnabled", true)
    }
}