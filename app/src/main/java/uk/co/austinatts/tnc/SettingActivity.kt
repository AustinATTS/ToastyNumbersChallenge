package uk.co.austinatts.tnc

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class SettingActivity : AppCompatActivity(){

    private lateinit var app_settings: SharedPreferences
    private lateinit var switch_auto_answer: Switch
    private lateinit var cb_sound: CheckBox
    private lateinit var cb_vibrations: CheckBox
    private lateinit var ibtn_back: ImageButton
    private lateinit var btn_account: Button
    private lateinit var btn_reset: Button
    private lateinit var intent: Intent
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setting)

        app_settings = getSharedPreferences("app_settings", MODE_PRIVATE)
        ibtn_back = findViewById(R.id.ibtn_back)
        switch_auto_answer = findViewById(R.id.switch_auto_answer)
        cb_sound = findViewById(R.id.cb_sound)
        cb_vibrations = findViewById(R.id.cb_vibration)
        btn_account = findViewById(R.id.btn_account)
        btn_reset = findViewById(R.id.btn_reset)

        Log.d("Auth", "1")
        loadSettings()
        Log.d("Auth", "2")

        ibtn_back.setOnClickListener {
            vibrateIfEnabled()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        switch_auto_answer.setOnCheckedChangeListener { _, isChecked ->
            vibrateIfEnabled()
            saveSettings("autoAnswerEnabled", isChecked)
        }

        cb_sound.setOnCheckedChangeListener { _, isChecked ->
            vibrateIfEnabled()
            saveSettings("soundEnabled", isChecked)
        }

        cb_vibrations.setOnCheckedChangeListener { _, isChecked ->
            vibrateIfEnabled()
            saveSettings("vibrationsEnabled", isChecked)
        }

        btn_account.setOnClickListener {
            vibrateIfEnabled()
            account()
        }

        btn_reset.setOnClickListener {
            vibrateIfEnabled()
            reset()
        }
    }

    private fun loadSettings() {
        Log.d("Auth", "3")
        val user = auth.currentUser
        if (user != null) {
            Log.d("Auth", "4")
            firestore.collection("users").document(user.uid)
                .collection("settings").document("preferences")
                .get()
                .addOnSuccessListener { document ->
                    Log.d("Auth", "Document does not exist 18")
                    if (document.exists()) {
                        Log.d("Auth", "Document exists: ${document.data}")
                        val autoAnswerEnabled = document.getBoolean("autoAnswerEnabled") ?: true
                        val soundEnabled = document.getBoolean("soundEnabled") ?: true
                        val vibrationsEnabled = document.getBoolean("vibrationsEnabled") ?: true

                        with(app_settings.edit()) {
                            putBoolean("autoAnswerEnabled", autoAnswerEnabled)
                            putBoolean("soundEnabled", soundEnabled)
                            putBoolean("vibrationsEnabled", vibrationsEnabled)
                            apply()
                        }

                        switch_auto_answer.isChecked = autoAnswerEnabled
                        cb_sound.isChecked = soundEnabled
                        cb_vibrations.isChecked = vibrationsEnabled
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
        val autoAnswerEnabled = app_settings.getBoolean("autoAnswerEnabled", true)
        val soundEnabled = app_settings.getBoolean("soundEnabled", true)
        val vibrationsEnabled = app_settings.getBoolean("vibrationsEnabled", true)

        switch_auto_answer.isChecked = autoAnswerEnabled
        cb_sound.isChecked = soundEnabled
        cb_vibrations.isChecked = vibrationsEnabled
    }

    private fun saveSettings(setting: String, status: Boolean) {
        with(app_settings.edit()) {
            putBoolean(setting, status)
            apply()
        }

        val user = auth.currentUser
        if (user != null) {
            val settingsMap = mapOf(
                setting to status
            )
            firestore.collection("users").document(user.uid)
                .collection("settings").document("preferences")
                .set(settingsMap, SetOptions.merge())
        }

        loadSettings()
    }

    private fun account() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            if (user.isAnonymous) {
                startActivity(Intent(this, SignInActivity::class.java))
            } else {
                startActivity(Intent(this, AccountActivity::class.java))
            }
        } else
            startActivity(Intent(this, SignInActivity::class.java))
    }


    private fun reset() {
        saveSettings("autoAnswerEnabled", true)
        saveSettings("soundEnabled", true)
        saveSettings("vibrationsEnabled", true)

        switch_auto_answer.isChecked = true
        cb_sound.isChecked = true
        cb_vibrations.isChecked = true
    }

    fun vibrateIfEnabled() {
        if (!cb_vibrations.isChecked) return

        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }
}