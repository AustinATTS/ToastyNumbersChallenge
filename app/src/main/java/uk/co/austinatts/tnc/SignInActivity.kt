package uk.co.austinatts.tnc

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignInActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var vibrationsEnabled: Boolean = true
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var signInButton: Button
    private lateinit var registerButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        loadPreferences()

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        emailEditText = findViewById(R.id.et_email)
        passwordEditText = findViewById(R.id.et_password)
        signInButton = findViewById(R.id.btn_sign_in)
        registerButton = findViewById(R.id.btn_register)

        // Navigate to Register Page, passing email if entered
        registerButton.setOnClickListener {
            vibrateIfEnabled()
            val email = emailEditText.text.toString()
            val intent = Intent(this, RegisterActivity::class.java)
            intent.putExtra("email", email)
            startActivity(intent)
        }

        // Sign In user
        signInButton.setOnClickListener {
            vibrateIfEnabled()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                signInUser(email, password)
            } else {
                Toast.makeText(this, "Please fill in all fields.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun signInUser(email: String, password: String) {
        val currentUser = auth.currentUser

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    startActivity(Intent(this, AccountActivity::class.java))
                    if (currentUser != null && currentUser.isAnonymous) {
                        firestore.collection("users").document(currentUser.uid).delete()
                            .addOnCompleteListener { deleteFTask ->
                                if (deleteFTask.isSuccessful) {
                                    Log.d("Auth", "Anonymous FUser Deleted")
                                } else {
                                    Log.e("Auth", "Failed to delete anonymous Fuser: ${deleteFTask.exception?.message}")
                                }
                            }
                        currentUser.delete()
                            .addOnCompleteListener { deleteTask ->
                                if (deleteTask.isSuccessful) {
                                    Log.d("Auth", "Anonymous User Deleted")
                                } else {
                                    Log.e("Auth", "Failed to delete anonymous user: ${deleteTask.exception?.message}")
                                }
                            }
                    }

                    Toast.makeText(this, "Signed in successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to sign in: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
