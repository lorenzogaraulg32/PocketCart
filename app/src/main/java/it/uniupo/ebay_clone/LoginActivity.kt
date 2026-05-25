package it.uniupo.ebay_clone

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.google.firebase.auth.FirebaseAuth
import it.uniupo.ebay_clone.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            insets.consumeSystemWindowInsets()
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = window.insetsController
            windowInsetsController?.hide(WindowInsets.Type.navigationBars())
            windowInsetsController?.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        binding.btnLogin.setOnClickListener {
            checkUser()
        }
        binding.noAccount.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        binding.logLayout.setOnClickListener {
            it.hideKeyboard()
            binding.email.clearFocus()
            binding.password.clearFocus()
        }
    }

    //funzione per rimuovere la tastiera e i focus
    private fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)

    }

    //funzione per evitare che un utente già loggato possa accedere al login nuovamente
    override fun onRestart() {
        super.onRestart()
        if (auth.currentUser != null) {
            finish()
        }
    }

    //funzione che gestisce il login tramite firebase, rimanda alla homepage se login corretto
    private fun signIn(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this, HomePage::class.java))
                finish()
            } else {
                binding.passErr.visibility = View.VISIBLE
                binding.passErr.text = getString(R.string.failed_login)
            }
        }
    }

    //funzione per verificare gli input del login
    private fun checkUser() {
        val email = binding.emailField.text.toString()
        val pass = binding.passwordField.text.toString()
        val checkEmail: Boolean
        val checkPass: Boolean
        if (email.isNotEmpty()) {
            if (Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                binding.emailErr.visibility = View.INVISIBLE
                checkEmail = true
            } else {
                binding.emailErr.text = getString(R.string.log_err_email_format)
                binding.emailErr.visibility = View.VISIBLE
                checkEmail = false
            }
        } else {
            binding.emailErr.visibility = View.VISIBLE
            checkEmail = false
        }
        if (pass.isNotEmpty()) {
            binding.passErr.visibility = View.INVISIBLE
            checkPass = true
        } else {
            binding.passErr.visibility = View.VISIBLE
            checkPass = false
        }
        if (checkEmail && checkPass) {
            signIn(email, pass)
        }
    }
}