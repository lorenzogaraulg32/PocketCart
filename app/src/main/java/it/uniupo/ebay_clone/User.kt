package it.uniupo.ebay_clone

import android.app.Activity
import android.location.Geocoder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class User(
    var NomeCompleto: String,
    var Email: String,
    var Username: String,
    var Admin: Boolean,
    var Posizione: ArrayList<Double>,
    var Budget: Double,
    //Status 0 attivo, 1 sospeso, 2 bannato
    var Status: Int,
    var Preferiti: HashMap<String, Boolean>,

    var Chat: ArrayList<String>,
    var Valutazione: Double,
    //storico recensioni ricevute
    var Valutazioni: ArrayList<Recensioni>,
    //a chi ho già lasciato una recensione
    var RecensioniLasciate: ArrayList<String>,
) {
    constructor() : this(
        "",
        "",
        "",
        false,
        ArrayList(),
        0.00,
        0,
        hashMapOf(),
        ArrayList(),
        0.0,
        ArrayList(),
        ArrayList(),
    )

    fun addUserToDb(db: FirebaseFirestore, user: FirebaseAuth) {
        MainScope().launch {
            withContext(Dispatchers.IO) {
                val utente = user.currentUser
                val df = utente?.let { db.collection("User").document(it.uid) }
                val userInfo = HashMap<String, Any>()
                userInfo["NomeCompleto"] = NomeCompleto
                userInfo["Email"] = Email
                userInfo["Username"] = Username
                userInfo["Admin"] = Admin
                userInfo["Posizione"] = Posizione
                userInfo["Budget"] = Budget
                userInfo["Status"] = Status
                userInfo["Preferiti"] = Preferiti
                userInfo["Chat"] = Chat
                userInfo["Valutazione"] = Valutazione
                userInfo["RecensioniLasciate"] = RecensioniLasciate
                userInfo["Valutazioni"] = Valutazioni
                df?.set(userInfo)
            }
            println("provo con le chat")
            for (u in db.collection("User").whereEqualTo("Admin", true).get().await()) {
                println("sono nel ciclo")
                val admin = u.toObject<User>()
                admin.Chat.add(Username)
                println("Chat = $admin.Chat")
                val adminInfo = HashMap<String, Any>()
                adminInfo["Chat"] = admin.Chat
                db.collection("User").document(u.id).update(adminInfo).addOnSuccessListener {
                    println("Chat aggiunta all'admin")
                }
            }
        }
    }

    fun updateUserPrefMod(db: FirebaseFirestore, id: String) {
        val userInfo = HashMap<String, Any>()
        userInfo["Preferiti"] = this.Preferiti
        db.collection("User").document(id).update(userInfo)
        println("Modificato il preferito di $id")
    }

    fun updateUserChat(db: FirebaseFirestore, id: String) {
        val userInfo = HashMap<String, Any>()
        userInfo["Chat"] = this.Chat
        db.collection("User").document(id).update(userInfo)
    }


    fun updateUserPos(db: FirebaseFirestore, auth: FirebaseAuth, posizione: ArrayList<Double>) {
        val userInfo = HashMap<String, Any>()
        userInfo["Posizione"] = posizione
        db.collection("User").document(auth.currentUser!!.uid).update(userInfo)
    }

    fun updateUserBudget(db: FirebaseFirestore, auth: FirebaseAuth, budget: Double) {
        val userInfo = HashMap<String, Any>()
        userInfo["Budget"] = budget
        db.collection("User").document(auth.currentUser!!.uid).update(userInfo)
    }

    fun updateUserWallet(db: FirebaseFirestore, auth: FirebaseAuth, importo: Double) {
        val userInfo = HashMap<String, Any>()
        userInfo["Budget"] = importo
        db.collection("User").document(auth.currentUser!!.uid).update(userInfo)
    }

    fun updateRewiews(db: FirebaseFirestore) {
        val userInfo = HashMap<String, Any>()
        userInfo["Valutazione"] = this.Valutazione
        userInfo["Valutazioni"] = this.Valutazioni
        println("valutazioni oggetto : ${this.Valutazioni}")
        db.collection("User").whereEqualTo("Username", this.Username).get().addOnSuccessListener {
            val userId = it.documents[0].id
            db.collection("User").document(userId).update(userInfo)
        }
    }

    fun updateRewiewsList(db: FirebaseFirestore, auth: FirebaseAuth) {
        val userInfo = HashMap<String, Any>()
        userInfo["RecensioniLasciate"] = this.RecensioniLasciate
        db.collection("User").document(auth.currentUser!!.uid).update(userInfo)
    }

    suspend fun getCurrentUser(auth: FirebaseAuth, db: FirebaseFirestore): User {
        val user = auth.currentUser
        val snapshotUser = db.collection("User").document(user!!.uid)
        if (snapshotUser.get().await().toObject(User::class.java) != null) {
            return snapshotUser.get().await().toObject(User::class.java)!!
        } else {
            return User()
        }

    }

    @Suppress("DEPRECATION")
    fun getUserPosition(activity: Activity): String {
        var address = ""
        return if (this.Posizione.isNotEmpty()) {
            val geoCoder = Geocoder(activity, Locale.getDefault())
            try {
                val addrList = geoCoder.getFromLocation(this.Posizione[0], this.Posizione[1], 1)!!
                val myAddress = addrList[0]
                address = myAddress.getAddressLine(0).toString()
            } catch (e: Exception) {
                println("Exception :$e")
            }
            address
        } else {
            "User has no position"
        }
    }

}
