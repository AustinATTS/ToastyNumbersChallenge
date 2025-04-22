package uk.co.austinatts.tnc

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import uk.co.austinatts.tnc.databinding.ActivityMainBinding
import com.robinhood.ticker.TickerView
import com.robinhood.ticker.TickerUtils
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var tkv_number_one: TickerView
    private lateinit var tkv_number_two: TickerView
    private lateinit var tkv_operator: TickerView
    private lateinit var tkv_result: TickerView
    private lateinit var fa_auth: FirebaseAuth
    private lateinit var ibtn_setting: ImageButton
    private val firestore = FirebaseFirestore.getInstance()
    private var vibrationsEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        tkv_number_one = findViewById(R.id.tkv_number_one)
        tkv_number_two = findViewById(R.id.tkv_number_two)
        tkv_operator = findViewById(R.id.tkv_operator)
        tkv_result = findViewById(R.id.tkv_result)

        tkv_number_one.typeface = ResourcesCompat.getFont(this, R.font.selawik_bold)
        tkv_number_two.typeface = ResourcesCompat.getFont(this, R.font.selawik_bold)
        tkv_operator.typeface = ResourcesCompat.getFont(this, R.font.selawik_bold)
        tkv_result.typeface = ResourcesCompat.getFont(this, R.font.selawik_bold)

        tkv_number_one.setCharacterLists(TickerUtils.provideNumberList())
        tkv_number_two.setCharacterLists(TickerUtils.provideNumberList())
        tkv_operator.setCharacterLists(TickerUtils.provideAlphabeticalList())
        tkv_result.setCharacterLists(TickerUtils.provideNumberList())

        fa_auth = FirebaseAuth.getInstance()
        val current_user = fa_auth.currentUser

        if (current_user == null) {
            createAnonymousUser()
            initialiseSettings()
        }

        ibtn_setting = findViewById(R.id.ibtn_setting)
        ibtn_setting.setOnClickListener{
            vibrateIfEnabled()
            startActivity(Intent(this, SettingActivity::class.java))
        }

        binding.btnEndless.setOnClickListener {
            vibrateIfEnabled()
            navigateToActivity(EndlessActivity::class.java)
        }

        binding.btnPlay.setOnClickListener {
            vibrateIfEnabled()
            navigateToActivity(PlayActivity::class.java)
        }

        binding.btnCustom.setOnClickListener {
            vibrateIfEnabled()
            navigateToActivity(CustomSelectorActivity::class.java)
        }

        binding.btnStatistic.setOnClickListener {
            vibrateIfEnabled()
            navigateToActivity(StatisticActivity::class.java)
        }

        updateEquation()
    }

    private fun createAnonymousUser() {
        fa_auth.signInAnonymously()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    fa_auth.currentUser
                } else {
                    Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun initialiseSettings() {
        saveSettings("autoAnswerEnabled", true)
        saveSettings("soundEnabled", true)
        saveSettings("vibrationsEnabled", true)
        Log.d("Settings", "Settings Done")
    }

    private fun saveSettings(setting: String, status: Boolean) {
        val app_settings = getSharedPreferences("app_settings", MODE_PRIVATE)
        with(app_settings.edit()) {
            putBoolean(setting, status)
            apply()
        }

        val user = fa_auth.currentUser
        if (user != null) {
            val settingsMap = mapOf(
                setting to status
            )
            firestore.collection("users").document(user.uid)
                .collection("settings").document("preferences")
                .set(settingsMap, SetOptions.merge())
        }
    }

    fun vibrateIfEnabled() {
        loadPreferences()
        if (!vibrationsEnabled) return

        val vibrator = getSystemService(VIBRATOR_SERVICE) as Vibrator

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

    private fun navigateToActivity(activityClass: Class<*>) {
        val intent = Intent(this, activityClass)
        startActivity(intent)
    }

    private fun updateEquation() {
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

        tkv_number_one.animationDuration = 1500
        tkv_number_two.animationDuration = 1500
        tkv_operator.animationDuration = 1500
        tkv_result.animationDuration = 1500

        tkv_number_one.text = number_one.toString()
        tkv_number_two.text = number_two.toString()
        tkv_operator.text = operator
        tkv_result.text = result.toString()

        tkv_number_one.postDelayed({
            updateEquation()
        }, 5000)
    }
}