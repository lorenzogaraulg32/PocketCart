package it.uniupo.ebay_clone

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.google.firebase.auth.FirebaseAuth
import it.uniupo.ebay_clone.databinding.AboutUsBinding


class AboutUs : AppCompatActivity() {

    private lateinit var binding: AboutUsBinding
    private lateinit var auth: FirebaseAuth
    private var code = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
            insets.consumeSystemWindowInsets()
        }

        binding = AboutUsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        code = intent.getIntExtra("code", 0)
        println("code : $code")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowInsetsController = window.insetsController
            windowInsetsController?.hide(WindowInsets.Type.navigationBars())
            windowInsetsController?.systemBarsBehavior =
                WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null && code != 1) {
            startActivity(Intent(this, HomePage::class.java))
            finish()
        }

        binding.btnClose.setOnClickListener {
            if (code == 0) {
                startActivity(Intent(this, FirstActivity::class.java))
                finish()
            } else {
                finish()
            }
        }

        binding.referenzeUpo.text =
            Html.fromHtml("<a href=\"mailto:mobileapp@uniupo.it\"> mobileapp@uniupo.it </a>")
        binding.referenzeLorenzo.text =
            Html.fromHtml("<a href=\"mailto:lorenzo.garau.lg33@gmail.com\"> lorenzo.garau.lg33@gmail.com </a>")
        binding.referenzeFlavio.text =
            Html.fromHtml("<a href=\"mailto:flaviorognoni64@gmail.com\"> flaviorognoni64@gmail.com </a>")
        binding.referenzeUpo.movementMethod = LinkMovementMethod.getInstance()
        binding.referenzeLorenzo.movementMethod = LinkMovementMethod.getInstance()
        binding.referenzeFlavio.movementMethod = LinkMovementMethod.getInstance()



    }

    override fun onRestart() {
        super.onRestart()
        if (auth.currentUser != null && code != 1) {
            finish()
        }
    }
}