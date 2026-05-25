package it.uniupo.ebay_clone

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Research(
    val userID: String,
    val searchName: String? = null,
    val searchPriceMin: String? = null,
    val searchPriceMax: String? = null,
    val searchShipping: Boolean = false,
    val searchPos: ArrayList<Double>? = null,
    val searchAddress : String? = null,
    val searchDistance: Double = -1.0,
) {

    constructor() : this("", null, null, null, false, null, null,-1.0)


    suspend fun saveSearch(research: Research, db: FirebaseFirestore) {
        val id = Date().time.toString()
        val searchInfo = java.util.HashMap<String, Any>()
        searchInfo["userID"] = research.userID
        searchInfo["searchName"] = research.searchName.orEmpty()
        searchInfo["searchPriceMin"] = research.searchPriceMin.orEmpty()
        searchInfo["searchPriceMax"] = research.searchPriceMax.orEmpty()
        searchInfo["searchShipping"] = research.searchShipping
        searchInfo["searchPos"] = research.searchPos.orEmpty()
        searchInfo["searchAddress"] = research.searchAddress.orEmpty()
        searchInfo["searchDistance"] = research.searchDistance
        println(searchInfo)
        db.collection("Research").document(id).set(searchInfo).await()
    }
    suspend fun queryNome(db: FirebaseFirestore, user:User): HashMap<Item, String> {
        val map: HashMap<Item, String> = HashMap()
        val query = db.collection("Item").whereEqualTo("Venduto", false)
        val snapshot = query.get().await()
        return if (this.searchName.isNullOrEmpty()) {
            for (document in snapshot.documents) {
                if (document.get("Proprietario") != user.Username) {
                    val itemFull: Item = document.toObject(Item::class.java)!!
                    map[itemFull] = document.id
                }

            }
            println("Torno mappa con tutti i nomi")
            map
        } else {
            for (document in snapshot.documents) {
                if (document.get("Nome").toString().lowercase()
                        .contains(this.searchName.lowercase())
                ) {
                    val itemFull: Item = document.toObject(Item::class.java)!!
                    map[itemFull] = document.id
                }
            }
            println("Torno mappa che contiene ${this.searchName}, size = ${map.size} ")
            map
        }
    }

    fun queryPos(map: HashMap<Item, String>): HashMap<Item, String> {
        val mapPos: HashMap<Item, String> = HashMap()
        for (items in map) {
                val dist = calculateDistance(this.searchPos!![0]  , this.searchPos[1], items.key.Posizione[0], items.key.Posizione[1])
                println("dist = $dist")
                println("Search dist = ${this.searchDistance}")
                if ( dist <= this.searchDistance) {
                    mapPos[items.key] = items.value
                }
            }
        return mapPos
    }


    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371

        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)

        val a = sin(latDistance / 2) * sin(latDistance / 2) + cos(Math.toRadians(lat1)) * cos(
            Math.toRadians(lat2)
        ) * sin(lonDistance / 2) * sin(lonDistance / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun queryShipping(map: HashMap<Item, String>): HashMap<Item, String> {
        val shipping = this.searchShipping
        val mapShipping: HashMap<Item, String> = HashMap()
        if (shipping) {
            for (items in map) {
                if (items.key.Spedizione) {
                    mapShipping[items.key] = items.value
                }
            }
            return mapShipping
        }

        for (items in map) {
            if (!items.key.Spedizione) {
                mapShipping[items.key] = items.value
            }
        }
        return mapShipping

    }

    fun queryPrice(map: HashMap<Item, String>): HashMap<Item, String> {
        val mapPrice: HashMap<Item, String> = HashMap()
        if (!this.searchPriceMin.isNullOrEmpty() && !searchPriceMax.isNullOrEmpty()) {
            for (items in map) {
                if (((this.searchPriceMin.toDoubleOrNull()
                        ?: Double.MIN_VALUE) <= items.key.Prezzo) && (items.key.Prezzo <= (searchPriceMax.toDoubleOrNull()
                        ?: Double.MAX_VALUE))
                ) {
                    mapPrice[items.key] = items.value
                }
            }
        } else if (searchPriceMax.isNullOrEmpty() && !this.searchPriceMin.isNullOrEmpty()) {
            for (items in map) {
                if ((this.searchPriceMin.toDoubleOrNull() ?: Double.MIN_VALUE) <= items.key.Prezzo) {
                    mapPrice[items.key] = items.value
                }
            }
        } else if (!searchPriceMax.isNullOrEmpty() && this.searchPriceMin.isNullOrEmpty()) {
            for (items in map) {
                if (items.key.Prezzo <= (this.searchPriceMax.toDoubleOrNull() ?: Double.MAX_VALUE)) {
                    mapPrice[items.key] = items.value
                }
            }
        }
        return mapPrice
    }
}

