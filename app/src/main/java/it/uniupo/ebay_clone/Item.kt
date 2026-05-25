package it.uniupo.ebay_clone

import android.app.Activity
import android.location.Geocoder
import android.os.Parcelable
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.*

@Parcelize
class Item(
    var Nome: String,
    var Categoria: String,
    var SottoCategoria: String,
    //0 = latitudine, 1 = longitudine
    var Posizione: ArrayList<Double>,
    var Descrizione: String,
    var Prezzo: Double,
    var Stato: Int,
    var Foto: ArrayList<String>,
    var Proprietario: String,
    var Spedizione: Boolean,
    var Acquirente: String,
    var Venduto: Boolean,
    var DataVendita: Long
) : Parcelable {
    constructor() : this(
        "",
        "",
        "",
        ArrayList(),
        "",
        0.toDouble(),
        0,
        ArrayList(),
        "",
        false,
        "",
        false,
        0
    )

    fun addItemtoDb(db: FirebaseFirestore) {

        val id = Date().time.toString()

        val itemInfo = HashMap<String, Any>()
        itemInfo["Nome"] = Nome
        itemInfo["Categoria"] = Categoria
        itemInfo["SottoCategoria"] = SottoCategoria
        itemInfo["Posizione"] = Posizione
        itemInfo["Descrizione"] = Descrizione
        itemInfo["Prezzo"] = Prezzo
        itemInfo["Stato"] = Stato
        itemInfo["Foto"] = Foto
        itemInfo["Proprietario"] = Proprietario
        itemInfo["Spedizione"] = Spedizione
        itemInfo["Acquirente"] = Acquirente
        itemInfo["Venduto"] = Venduto
        itemInfo["DataVendita"] = DataVendita


        db.collection("Item").document(id)
            .set(itemInfo)
            .addOnSuccessListener {
                println("Oggeetto aggiunto correttamente")
            }
    }

    fun ScaleDouble(num: Double): String {
        var roundendNum = BigDecimal(num).setScale(2, BigDecimal.ROUND_HALF_EVEN).toString()
        return roundendNum
    }

    suspend fun getCurrentItem(db: FirebaseFirestore, itemId: String): Item {
        val snapshot = db.collection("Item").document(itemId)
        val document = snapshot.get().await()
        val oggetto = document.toObject<Item>()
        println("oggetto : $oggetto")
        return oggetto!!
    }


    fun getItemPosition(activity: Activity): String {
        var address = ""
        if (this.Posizione.isNotEmpty()) {
            val geoCoder = Geocoder(activity, Locale.getDefault())
            try {
                val addrList = geoCoder.getFromLocation(this.Posizione[0], this.Posizione[1], 1)!!
                val myAddress = addrList[0]
                println("retrived position item = $myAddress")
                address = myAddress.getAddressLine(0).toString()
            } catch (e: Exception) {
                println("Exception:$e")
            }
            return address
        } else {
            return "null"
        }
    }

    fun updateItemVenduto(db: FirebaseFirestore, itemId: String) {
        val itemInfo = HashMap<String, Any>()
        itemInfo["Acquirente"] = Acquirente
        itemInfo["Venduto"] = Venduto
        itemInfo["DataVendita"] = DataVendita
        db.collection("Item").document(itemId).update(itemInfo)
    }


}