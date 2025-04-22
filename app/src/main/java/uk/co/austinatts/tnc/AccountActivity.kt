package uk.co.austinatts.tnc

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth

class AccountActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var tv_email: TextView
    private lateinit var btn_sign_out: Button
    private lateinit var ibtn_back: ImageButton

    private var vibrationsEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account)

        auth = FirebaseAuth.getInstance()

        tv_email = findViewById(R.id.tv_email)
        btn_sign_out = findViewById(R.id.btn_sign_out)
        ibtn_back = findViewById(R.id.ibtn_back)

        loadPreferences()

        val user = auth.currentUser
        user?.let {
            tv_email.text = "Logged in as: ${it.email}"
        }

        // Sign out user
        btn_sign_out.setOnClickListener {
            vibrateIfEnabled()
            auth.signOut()
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        ibtn_back.setOnClickListener {
            vibrateIfEnabled()
            startActivity(Intent(this, SettingActivity::class.java))
            finish()
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
