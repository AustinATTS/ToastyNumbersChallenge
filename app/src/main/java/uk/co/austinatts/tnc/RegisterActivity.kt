package uk.co.austinatts.tnc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button

    private var vibrationsEnabled: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        loadPreferences()

        auth = FirebaseAuth.getInstance()
        emailEditText = findViewById(R.id.et_email)
        passwordEditText = findViewById(R.id.et_password)
        confirmPasswordEditText = findViewById(R.id.et_confirm_password)
        registerButton = findViewById(R.id.btn_register)

        val emailFromSignIn = intent.getStringExtra("email")
        emailFromSignIn?.let {
            emailEditText.setText(it)
        }

        // Register new user
        registerButton.setOnClickListener {
            vibrateIfEnabled()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    linkAnonymousAccount(email, password)
                } else {
                    Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun linkAnonymousAccount(email: String, password: String) {
        val user = auth.currentUser

        if (user != null && user.isAnonymous) {
            val credential = EmailAuthProvider.getCredential(email, password)

            user.linkWithCredential(credential)
                .addOnCompleteListener{ task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account linked successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, AccountActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to link account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Account created!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, AccountActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to create account: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }
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
