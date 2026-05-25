package it.uniupo.ebay_clone

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import it.uniupo.ebay_clone.databinding.ActivityChatViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class ChatView : AppCompatActivity() {

    private lateinit var binding: ActivityChatViewBinding

    private val dataBase = FirebaseFirestore.getInstance()

    private lateinit var auth: FirebaseAuth
    private lateinit var currentUser: User

    lateinit var receiver_view: TextView

    lateinit var send_text: EditText
    var chatDim: Int = 0

    private lateinit var recyclerView: RecyclerView

    var nomeChat = "Chat-"
    private val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
    private var start = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        MainScope().launch {
            withContext(Dispatchers.Main) {
                currentUser = User().getCurrentUser(auth, dataBase)
            }
            if (currentUser.Status != 0) {
                Toast.makeText(
                    this@ChatView,
                    getString(R.string.toast_authorization_missing),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }

        val sender: String = intent.getStringExtra("sender").toString()
        val receiver: String = intent.getStringExtra("receiver").toString()
        val senderId: String = intent.getStringExtra("senderId").toString()
        println(" prima stampa = " + sender + "-" + receiver)
        receiver_view = findViewById(R.id.chat_title)
        receiver_view.text = receiver
        recyclerView = findViewById(R.id.rv_chat)
        recyclerView.layoutManager = LinearLayoutManager(this)

        chatRefresh(sender, receiver, senderId)

        val back_btn = findViewById<ImageButton>(R.id.back_btn)
        back_btn.setOnClickListener {
            finish()
        }

        send_text = findViewById(R.id.send_message)

        val send_btn = findViewById<ImageButton>(R.id.send_message_btn)
        send_btn.setOnClickListener {
            checkStatusUtente(sender, receiver, senderId)
            it.hideKeyboard()
        }

        chatUpdateRealTime(sender, receiver, senderId)

    }

    private fun View.hideKeyboard() {
        val inputManager =
            context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        inputManager.hideSoftInputFromWindow(windowToken, 0)

    }

    private fun chatRefresh(sender: String, receiver: String, senderId: String) {
        val data = ArrayList<MessageCard>()
        dataBase.collection("Chat").document("Chat-" + sender + "-" + receiver).get()
            .addOnSuccessListener { document ->
                if (document.data != null) {
                    println(document.id)
                    chatDim = (document.data?.size)!!.dec()
                    println("chatDIm = $chatDim")
                    var count = 0
                    while (count < chatDim) {
                        val msgTmp = document.get("Messaggio " + count) as HashMap<*, *>
                        if (msgTmp["nome"]!!.equals(receiver)) {
                            data.add(
                                MessageCard(
                                    msgTmp.get("msg").toString(),
                                    msgTmp.get("nome").toString(),
                                    msgTmp.get("data").toString(),
                                    1
                                )
                            )
                            count++
                        } else {
                            data.add(
                                MessageCard(
                                    msgTmp.get("msg").toString(),
                                    msgTmp.get("nome").toString(),
                                    msgTmp.get("data").toString(),
                                    0
                                )
                            )
                            count++
                        }
                    }
                    val adapter = RvAdapterMessages(data)
                    recyclerView.adapter = adapter
                    recyclerView.scrollToPosition(data.size - 1)
                    nomeChat = "Chat-$sender-$receiver"
                    if (!start) {
                        startListening(sender, receiver, senderId)
                        start = true
                    }
                } else {
                    dataBase.collection("Chat").document("Chat-$receiver-$sender").get()
                        .addOnSuccessListener { document ->
                            if (document.data != null) {
                                chatDim = (document.data?.size)!!.dec()
                                var count = 0
                                while (count < chatDim) {
                                    val msgTmp =
                                        document.get("Messaggio $count") as HashMap<*, *>
                                    if (msgTmp["nome"]!!.equals(receiver)) {
                                        data.add(
                                            MessageCard(
                                                msgTmp.get("msg").toString(),
                                                msgTmp.get("nome").toString(),
                                                msgTmp.get("data").toString(),
                                                1
                                            )
                                        )
                                        count++
                                    } else {
                                        data.add(
                                            MessageCard(
                                                msgTmp.get("msg").toString(),
                                                msgTmp.get("nome").toString(),
                                                msgTmp.get("data").toString(),
                                                0
                                            )
                                        )
                                        count++
                                    }

                                }

                                val adapter = RvAdapterMessages(data)
                                recyclerView.adapter = adapter
                                recyclerView.scrollToPosition(data.size - 1)
                                nomeChat = "Chat-" + receiver + "-" + sender
                                if (!start) {
                                    startListening(sender, receiver, senderId)
                                    start = true
                                }
                            } else {
                                var startChat = HashMap<String, Boolean>()
                                startChat.put("ChatStart", true)
                                dataBase.collection("Chat")
                                    .document("Chat-" + sender + "-" + receiver).set(startChat)
                                    .addOnSuccessListener {
                                        nomeChat = "Chat-" + sender + "-" + receiver
                                        dataBase.collection("User").document(senderId).get()
                                            .addOnSuccessListener { document ->
                                                var chats =
                                                    document.get("Chat") as ArrayList<String>
                                                if (document.get("Admin") == false && !currentUser.Admin) {
                                                    chats.add(receiver)
                                                }
                                                dataBase.collection("User").document(senderId)
                                                    .update("Chat", chats).addOnSuccessListener {}
                                            }
                                        dataBase.collection("User")
                                            .whereEqualTo("Username", receiver).get()
                                            .addOnSuccessListener {
                                                var doc = it.documents[0]
                                                var receiverId = doc.id
                                                var chats = doc.get("Chat") as ArrayList<String>
                                                chats.add(sender)
                                                dataBase.collection("User").document(receiverId)
                                                    .update("Chat", chats).addOnSuccessListener {
                                                        if (!start) {
                                                            startListening(
                                                                sender,
                                                                receiver,
                                                                senderId
                                                            )
                                                            start = true
                                                        }
                                                    }
                                            }
                                    }
                            }
                        }
                }
            }.addOnFailureListener {
                println("Chat inesistente")
            }
    }

    private fun chatUpdateRealTime(sender: String, receiver: String, senderId: String) {
        dataBase.collection("Chat").document(nomeChat).addSnapshotListener { value, error ->
            when {
                error != null -> Toast.makeText(this, "Errore ricarica", Toast.LENGTH_SHORT).show()
                value != null && value.exists() -> {
                    chatRefresh(sender, receiver, senderId)
                }
            }
        }
    }

    private fun startListening(sender: String, receiver: String, senderId: String) {
        chatUpdateRealTime(sender, receiver, senderId)
    }

    override fun onResume() {
        super.onResume()
        MainScope().launch {
            withContext(Dispatchers.Main) {
                currentUser = User().getCurrentUser(auth, dataBase)
            }
            if (currentUser.Status != 0) {
                Toast.makeText(
                    this@ChatView,
                    getString(R.string.toast_authorization_missing),
                    Toast.LENGTH_LONG
                ).show()
                finish()
            }
        }
    }

    private fun checkStatusUtente(sender: String, receiver: String, senderId: String) {
        MainScope().launch {
            withContext(Dispatchers.Main) {
                currentUser = User().getCurrentUser(auth, dataBase)
            }
            if (currentUser.Status != 0) {
                finish()
            } else {
                if (send_text.text.isNotEmpty()) {
                    val map = HashMap<String, Any>()
                    val msgMap = HashMap<String, String>()
                    msgMap.put("nome", sender)
                    msgMap.put("msg", send_text.text.toString())
                    msgMap.put("data", sdf.format(Date()))
                    map.put("Messaggio " + chatDim, msgMap)
                    val addmsg = dataBase.collection("Chat").document(nomeChat)
                    addmsg.update(map)
                        .addOnSuccessListener {
                            chatRefresh(sender, receiver, senderId)
                            send_text.text.clear()
                        }
                        .addOnFailureListener {
                            println("non inviato")
                        }
                } else {
                    println("Empty msg, do nothing")
                }
                }
            }
        }

    }
