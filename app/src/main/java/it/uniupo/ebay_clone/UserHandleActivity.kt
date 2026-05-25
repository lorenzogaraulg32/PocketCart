package it.uniupo.ebay_clone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserHandleActivity : AppCompatActivity() {

    lateinit var currentUser: User
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    private lateinit var usItems: RecyclerView
    private lateinit var adapter: UserAdapter
    private lateinit var ricerca: EditText
    private lateinit var serc_btn: Button
    private lateinit var back_btn: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_handle)

        usItems = findViewById(R.id.usRecycle)
        usItems.layoutManager = LinearLayoutManager(this)
        ricerca = findViewById(R.id.user_search_field)
        serc_btn = findViewById(R.id.search_user_btn)
        back_btn = findViewById(R.id.back_btn)
        auth = FirebaseAuth.getInstance()

        val user = auth.currentUser
        val snapshotUser = db.collection("User").document(user!!.uid)

        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = snapshotUser.get().await().toObject(User::class.java)!!
            }
            creaRecycleUsers()
        }

        back_btn.setOnClickListener {
            finish()
        }

        serc_btn.setOnClickListener {
            ricerca()
        }
    }

    private fun creaRecycleUsers() {
        val data = ArrayList<UserViewModel>()
        db.collection("User").get().addOnSuccessListener { documents ->
            for (document in documents) {
                if (!document.get("Admin").toString().toBoolean() && document.get("Status")
                        .toString().toInt() != 3
                ) {
                    data.add(UserViewModel(document.get("Username").toString(), document.id))
                }
            }
            adapter = UserAdapter(data)
            usItems.adapter = adapter
            adapter.setOnItemClickListener(object : UserAdapter.onItemClickListener {
                override fun onItemClick(position: Int) {
                    val intent = Intent(this@UserHandleActivity, UserModActivity::class.java)
                    intent.putExtra("userId", data[position].id)
                    startActivity(intent)
                }
            })
        }
    }


    override fun onResume() {
        super.onResume()
        adapter = UserAdapter(arrayListOf())
        usItems.adapter = adapter
        creaRecycleUsers()
    }

    private fun ricerca() {
        val data = ArrayList<UserViewModel>()
        db.collection("User").get().addOnSuccessListener { documents ->
            for (document in documents) {
                if (!document.get("Admin").toString().toBoolean() && document.get("Status")
                        .toString().toInt() != 3
                ) {
                    if (document.get("Username").toString().lowercase().contains(ricerca.text.toString().lowercase())) {
                        data.add(UserViewModel(document.get("Username").toString(), document.id))
                    }
                }
            }
            adapter = UserAdapter(data)
            usItems.adapter = adapter
            adapter.setOnItemClickListener(object : UserAdapter.onItemClickListener {
                override fun onItemClick(position: Int) {
                    val intent = Intent(this@UserHandleActivity, UserModActivity::class.java)
                    intent.putExtra("userId", data[position].id)
                    startActivity(intent)
                }
            })
        }
    }
}