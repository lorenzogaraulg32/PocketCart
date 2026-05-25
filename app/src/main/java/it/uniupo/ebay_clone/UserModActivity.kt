package it.uniupo.ebay_clone

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserModActivity : AppCompatActivity() {

    lateinit var userMod: String
    lateinit var usernameText: TextView
    lateinit var back_btn: ImageButton
    lateinit var mod_btn: Button
    lateinit var elim_btn: Button
    lateinit var sospendi_switch: Switch
    lateinit var blocca_switch: Switch
    lateinit var chat_btn: Button
    var user = User()
    lateinit var currentUser: User
    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()
    var mod = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_mod)

        userMod = intent.getStringExtra("userId").toString()
        usernameText = findViewById(R.id.username_mod)
        back_btn = findViewById(R.id.back_btn)
        mod_btn = findViewById(R.id.mod_btn)
        sospendi_switch = findViewById(R.id.sosp_switch)
        blocca_switch = findViewById(R.id.bann_switch)
        elim_btn = findViewById(R.id.delete_btn)
        chat_btn = findViewById(R.id.chat_btn)
        auth = FirebaseAuth.getInstance()

        MainScope().launch {
            withContext(Dispatchers.IO){
                currentUser = User().getCurrentUser(auth,db)
                getUtente()
            }
        }

        elim_btn.setOnClickListener {
            MainScope().launch {
                withContext(Dispatchers.IO) {
                    delUser()
                }
            }

        }

        chat_btn.setOnClickListener {
            val intent = Intent(this@UserModActivity, ChatView::class.java)
            intent.putExtra("senderId", auth.currentUser!!.uid)
            intent.putExtra("sender", currentUser.Username)
            intent.putExtra("receiver", user.Username)
            startActivity(intent)
        }

        back_btn.setOnClickListener {
            finish()
        }

        mod_btn.setOnClickListener {
            modificaUtente()
        }


    }

    private fun getUtente() {
        db.collection("User").document(userMod).get().addOnSuccessListener { document ->
            user = document.toObject(User::class.java)!!
            usernameText.text = user.Username.uppercase()
            if (user.Status == 1) sospendi_switch.isChecked = true
            else if (user.Status == 2) {
                sospendi_switch.isChecked = true
                blocca_switch.isChecked = true
            }
        }
    }

    private fun modificaUtente() {
        if (sospendi_switch.isChecked) mod = 1
        if (blocca_switch.isChecked) mod = 2
        val map = HashMap<String, Any>()
        map.put("Status", mod)
        db.collection("User").document(userMod).update(map).addOnSuccessListener {
            println("utente modificato")
            finish()
        }
    }

    private suspend fun delUser() {
        user.Status = 3
        mod = 3
        var i = 0
        val map = HashMap<String, Any>()
        map.put("Status", mod)

        //elimino gli oggetti messi in vendita dall'utente eliminato
        val queryItems = db.collection("Item").get().await()
        for (docs in queryItems) {
            if (docs.get("Proprietario") == user.Username) {
                //elimino anche dai preferiti degli altri utenti tutti gli oggetti
                checkPrefDel(docs.id)
                val item = db.collection("Item").document(docs.id).get().await().toObject<Item>()!!
                //elimino le foto dallo storage
                for (photo in item.Foto) {
                    val storageRef =
                        FirebaseStorage.getInstance().getReference("images/" + item.Foto.get(i))
                    storageRef.delete().addOnSuccessListener {
                        println("foto eliminata!")
                    }
                    i++
                }
                i = 0
                db.collection("Item").document(docs.id).delete()

            }
        }

        //elimino le chat degli altri utenti
        val queryUsers = db.collection("User").get().await()
        for (users in queryUsers) {
            val u = users.toObject<User>()
            if (u.Chat.contains(user.Username)) {
                u.Chat.remove(user.Username)
                u.updateUserChat(db, users.id)
            }
        }

        //elimino i documenti chat in cui c'è l'utente da cancellare
        val queryChat = db.collection("Chat").get().await()
        for (chats in queryChat) {
            if (chats.id.contains(user.Username)) {
                db.collection("Chat").document(chats.id).delete()
            }
        }

        //aggiorno lo status dell'utente
        db.collection("User").document(userMod).update(map).addOnSuccessListener {
            Toast.makeText(this, "Utente Eliminato", Toast.LENGTH_LONG).show()
            finish()
        }
    }


    private suspend fun checkPrefDel(itemId: String) {
        val query = db.collection("User").get().await()
        for (docs in query) {
            val u = docs.toObject<User>()
            for (p in u.Preferiti) {
                if (p.key.equals(itemId)) {
                    u.Preferiti.remove(p.key)
                    u.updateUserPrefMod(db, docs.id)
                    println("Modificato il preferito di ${docs.id} = ${u.Username}")
                }
            }
        }
    }

}