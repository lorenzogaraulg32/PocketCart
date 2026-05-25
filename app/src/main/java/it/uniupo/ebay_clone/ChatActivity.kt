package it.uniupo.ebay_clone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
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

class ChatActivity : AppCompatActivity() {

    lateinit var currentUser: User
    lateinit var back_btn: ImageButton
    lateinit var chat_recycle: RecyclerView
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    private lateinit var senderId: String
    private var chatInfo = arrayListOf<ChatViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        senderId = intent.getStringExtra("sender").toString()
        back_btn = findViewById(R.id.back_btn)
        chat_recycle = findViewById(R.id.chat_recycle)
        chat_recycle.layoutManager = LinearLayoutManager(this)
        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        val snapshotUser = db.collection("User").document(user!!.uid)

        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = snapshotUser.get().await().toObject(User::class.java)!!
                retrieveChatInfo()
            }
            if (currentUser.Status != 0) {
                Toast.makeText(
                    this@ChatActivity,
                    getString(R.string.toast_authorization_missing),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }

            creaRecycleChat()
        }

        back_btn.setOnClickListener {
            finish()
        }
    }

    private suspend fun retrieveChatInfo() {
        val chats = db.collection("Chat").get().await()

        for (receiver in currentUser.Chat) {
            for (chatSnapshot in chats) {
                val chatId = chatSnapshot.id.lowercase()
                if (chatId.contains(receiver.lowercase()) && chatId.contains(currentUser.Username.lowercase())) {
                    val chatSize = (chatSnapshot.data.size) - 2
                        val currentChat = chatSnapshot.get("Messaggio $chatSize") as? HashMap<*, *>
                        val message = currentChat?.get("msg") as? String
                        val date = currentChat?.get("data") as? String
                        val existingChat = chatInfo.find { it.nomeReceiver == receiver && it.lastMsg == message && it.lastMsgDate == date }
                           if (existingChat == null) {
                               chatInfo.add(ChatViewModel(receiver, message, date))
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        val user = auth.currentUser
        val snapshotUser = db.collection("User").document(user!!.uid)
        chatInfo.clear()
        val adapter = ChatAdapter(chatInfo)
        chat_recycle.adapter = adapter
        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = snapshotUser.get().await().toObject(User::class.java)!!
                retrieveChatInfo()
            }
            if (currentUser.Status != 0) {
                Toast.makeText(
                    this@ChatActivity,
                    getString(R.string.toast_authorization_missing),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
            creaRecycleChat()
        }
    }

    private fun creaRecycleChat() {
        val adapter = ChatAdapter(chatInfo)
        chat_recycle.adapter = adapter
        adapter.setOnItemClickListener(object : ChatAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent(this@ChatActivity, ChatView::class.java)
                intent.putExtra("senderId", senderId)
                intent.putExtra("sender", currentUser.Username)
                intent.putExtra("receiver", chatInfo[position].nomeReceiver)
                startActivity(intent)
            }
        })
    }
}
