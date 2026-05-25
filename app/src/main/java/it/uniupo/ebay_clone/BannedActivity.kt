package it.uniupo.ebay_clone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.MenuItem
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.get
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BannedActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var currentUser: User

    lateinit var bannedLogout: AppCompatButton

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_banned)

        bannedLogout=findViewById(R.id.logout_banned)

        auth = FirebaseAuth.getInstance()

        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
            }
        }

        bannedLogout.setOnClickListener {
            logout()
        }

    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        startActivity(Intent(this, FirstActivity::class.java))
        Toast.makeText(this, "Logout", Toast.LENGTH_SHORT).show()
        finish()
    }
}