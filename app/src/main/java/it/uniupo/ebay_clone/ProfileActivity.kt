package it.uniupo.ebay_clone

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import it.uniupo.ebay_clone.databinding.ActivityProfileBinding
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.io.File

//TODO:gestire recicler view vuote

class ProfileActivity : AppCompatActivity(), MapsFragment.MapsResult,
    CreditCardSimulator.CreditCardSimulatorListener {

    private lateinit var binding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore
    private var currentUser: User = User()
    private var address: String = "nothing"

    //fragment articoli venduti
    private var miscresfragment = Misc_res_fragment()
    private var creditCardSimulator = CreditCardSimulator()

    //shimmer caricamento
    private lateinit var shimmerAq: ShimmerFrameLayout
    private lateinit var shimmerVd: ShimmerFrameLayout
    private lateinit var shimmerSearch: ShimmerFrameLayout

    //recycler view
    private var allItemsIDs_acquisti = arrayListOf<String>()
    private var itemsToLoad_acquisti = arrayListOf<String>()
    private var allItemsIDs_invendita = arrayListOf<String>()
    private var itemsToLoad_invendita = arrayListOf<String>()
    private var rv_acquisti_source = arrayListOf<ItemCardProfile>()
    private var rv_invendita_source = arrayListOf<ItemCardProfile>()
    private lateinit var adapter: RVAdapterProfile
    private var totalItemsLoaded_acquisti = 0
    private var totalItemsLoaded_invendita = 0


    //recycler saved search
    private var rv_saved_search = arrayListOf<SearchCard>()
    private lateinit var adapter_search: RVAdapterSavedSearch
    private var searchResultHM: HashMap<Item, String> = hashMapOf()
    private var resultAR: java.util.ArrayList<String> = arrayListOf()

    //Posizione
    private var mapsFragment = MapsFragment()
    private var lat: Double = 0.0
    private var long: Double = 0.0
    var pos: ArrayList<Double> = ArrayList(2)

    var startForResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            refreshAndLoadAllRecyclers()
        }

    override fun getMapsResults(titolo: String, latitude: Double, longitude: Double) {
        lat = latitude
        long = longitude
        pos[0] = lat
        pos[1] = long
        println("${pos[0]}, ${pos[1]}")
        binding.indirizzoUtente.text = titolo
        address = titolo
        if (currentUser.Posizione.isNotEmpty()) {
            currentUser.Posizione[0] = lat
            currentUser.Posizione[1] = long
        } else if (currentUser.Posizione.isEmpty()) {
            currentUser.Posizione.add(lat)
            currentUser.Posizione.add(long)
        }
        println("latitudine: $lat, longitudine: $long, titolo: $address")
        currentUser.updateUserPos(db, auth, pos)
        binding.ricaricaWallet.isEnabled = true
        binding.modificaIndirizzo.isEnabled = true
        binding.vediOggInVendita.isEnabled = true
    }

    override fun sendAmmount(ammount: Double) {
        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
            }
            if (currentUser.Status == 2 || currentUser.Status == 3 || currentUser.Admin == true) {
                finish()
            } else {
                currentUser.Budget = currentUser.Budget + ammount
                currentUser.updateUserWallet(db, auth, currentUser.Budget)
                binding.saldoUtente.text = currentUser.Budget.toString() + " €"
                binding.ricaricaWallet.isEnabled = true
                binding.modificaIndirizzo.isEnabled = true
                binding.vediOggInVendita.isEnabled = true
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        pos.add(lat)
        pos.add(long)


        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (mapsFragment.isAdded) {
                    supportFragmentManager.beginTransaction().remove(mapsFragment).commit()
                    binding.ricaricaWallet.isEnabled = true
                    binding.modificaIndirizzo.isEnabled = true
                    binding.vediOggInVendita.isEnabled = true
                } else if (miscresfragment.isAdded) {
                    supportFragmentManager.beginTransaction().remove(miscresfragment).commit()
                    binding.ricaricaWallet.isEnabled = true
                    binding.modificaIndirizzo.isEnabled = true
                    binding.vediOggInVendita.isEnabled = true
                } else if (creditCardSimulator.isAdded) {
                    supportFragmentManager.beginTransaction().remove(creditCardSimulator).commit()
                    binding.ricaricaWallet.isEnabled = true
                    binding.modificaIndirizzo.isEnabled = true
                    binding.vediOggInVendita.isEnabled = true
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(onBackPressedCallback)

        //retrival e setting userinfo
        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
                address = currentUser.getUserPosition(this@ProfileActivity)
            }
            if (currentUser.Status == 2 || currentUser.Status == 3 || currentUser.Admin == true) {
                finish()
            }
            setUserInfo()
            refreshAndLoadAllRecyclers()
        }


        binding.vediOggInVendita.setOnClickListener {
            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.fragment_holder, miscresfragment)
                .commit()
        }

        binding.ricaricaWallet.setOnClickListener {
            if (currentUser.Status == 1) {
                Toast.makeText(
                    this,
                    getString(R.string.toast_authorization_missing),
                    Toast.LENGTH_LONG
                )
                    .show()
            } else {
                it.isEnabled = false
                binding.modificaIndirizzo.isEnabled = false
                binding.vediOggInVendita.isEnabled = false
                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(R.id.fragment_holder, creditCardSimulator)
                    .commit()
            }
        }

        binding.modificaIndirizzo.setOnClickListener {
            if (currentUser.Status == 1) {
                Toast.makeText(
                    this,
                    getString(R.string.toast_authorization_missing),
                    Toast.LENGTH_LONG
                )
                    .show()
            } else {
                binding.ricaricaWallet.isEnabled = false
                binding.modificaIndirizzo.isEnabled = false
                binding.vediOggInVendita.isEnabled = false
                val transaction = supportFragmentManager.beginTransaction()
                transaction.add(R.id.fragment_holder, mapsFragment).commit()
            }
        }

    }

    override fun onResume() {
        super.onResume()
        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
                address = currentUser.getUserPosition(this@ProfileActivity)
            }
            if (currentUser.Status == 2 || currentUser.Status == 3 || currentUser.Admin == true) finish()
        }
    }

    //FUNZIONE PER AGGIORNARE LE INFO UTENTE
    private fun setUserInfo() {
        runOnUiThread {
            binding.emailUtente.text = currentUser.Email
            binding.usernameUtente.text = currentUser.Username.uppercase()
            binding.nomeUtente.text = currentUser.NomeCompleto.uppercase()
            binding.indirizzoUtente.text = address
            binding.saldoUtente.text = Item().ScaleDouble(currentUser.Budget) + " €"
            binding.userRating.rating = currentUser.Valutazione.toFloat()
        }
    }

    private fun resetRecycler() {
        runOnUiThread {
            binding.rvAcquisti.visibility = View.GONE
            binding.rvVendite.visibility = View.GONE
            shimmerAq = binding.shimmer
            shimmerVd = binding.shimmer1
            shimmerSearch = binding.shimmer3
            shimmerAq.visibility = View.VISIBLE
            shimmerVd.visibility = View.VISIBLE
            shimmerSearch.visibility = View.VISIBLE
            shimmerAq.startShimmer()
            shimmerVd.startShimmer()
            shimmerSearch.startShimmer()
        }
        allItemsIDs_acquisti.clear()
        allItemsIDs_invendita.clear()
        itemsToLoad_acquisti.clear()
        itemsToLoad_invendita.clear()
        rv_saved_search.clear()
        adapter_search = RVAdapterSavedSearch(arrayListOf())
        rv_acquisti_source.clear()
        rv_invendita_source.clear()
        totalItemsLoaded_acquisti = 0
        totalItemsLoaded_invendita = 0
        adapter = RVAdapterProfile(arrayListOf())
        println("Recycler resettate")

    }

    private fun refreshAndLoadAllRecyclers() {
        resetRecycler()
        MainScope().launch {
            withContext(Dispatchers.IO) {
                rv_saved_search = querySavedSearch()
                allItemsIDs_acquisti = queryItemsIds("Acquirente")
                allItemsIDs_invendita = queryItemsIds("Proprietario")
            }
            itemsToLoad_acquisti = getIdToLoad(totalItemsLoaded_acquisti, allItemsIDs_acquisti)
            totalItemsLoaded_acquisti = +itemsToLoad_acquisti.size

            itemsToLoad_invendita = getIdToLoad(totalItemsLoaded_invendita, allItemsIDs_invendita)
            totalItemsLoaded_invendita = +itemsToLoad_invendita.size

            var itemCardsToLoad = idToItemCardList(itemsToLoad_acquisti)
            rv_acquisti_source.addAll(itemCardsToLoad)

            startRecycler(
                binding.rvAcquisti,
                itemCardsToLoad,
                shimmerAq,
                totalItemsLoaded_acquisti,
                allItemsIDs_acquisti,
                "Acquisti"
            )

            itemCardsToLoad = idToItemCardList(itemsToLoad_invendita)
            rv_invendita_source.addAll(itemCardsToLoad)

            startRecycler(
                binding.rvVendite,
                itemCardsToLoad,
                shimmerVd,
                totalItemsLoaded_invendita,
                allItemsIDs_invendita,
                "Vendita"
            )

            startRecyclerSearch(
                binding.rvSavedSearch,
                rv_saved_search,
                shimmerSearch
            )
        }
    }

    private suspend fun querySavedSearch(): ArrayList<SearchCard> {
        var searchesCard = arrayListOf<SearchCard>()
        val query = db.collection("Research").whereEqualTo("userID", auth.currentUser?.uid)
        val snapshot = query.get().await()
        if (snapshot.isEmpty) {
            println("La query non ha fornito nessun risultato field: UserID, username : ${currentUser.Username}")
        }
        for (doc in snapshot) {
            var search = doc.toObject<Research>()
            var searchID = doc.id
            var card = searchToCard(search, searchID)
            searchesCard.add(card)
        }
        println("Total items retrived 'Ricerche': ${searchesCard.size}")
        return searchesCard

    }

    private fun searchToCard(search: Research, searchID: String): SearchCard {
        var pos: String

        if (search.searchAddress.isNullOrEmpty() || search.searchDistance == -1.0) {
            pos = ""
        } else {
            pos = "Max ${search.searchDistance}KM DA \n ${search.searchAddress}"
        }

        var searchCard = SearchCard(
            searchID,
            search.searchName,
            "Min: ${search.searchPriceMin}€ Max: ${search.searchPriceMax}€",
            search.searchShipping,
            pos
        )
        println("Trasformato in search card $searchCard")
        return searchCard
    }

    private fun startRecyclerSearch(
        reciclerView: RecyclerView,
        source: ArrayList<SearchCard>,
        shimmer: ShimmerFrameLayout,
    ) {
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
        reciclerView.visibility = View.VISIBLE
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        reciclerView.layoutManager = layoutManager
        adapter_search = RVAdapterSavedSearch(source)
        reciclerView.adapter = adapter_search
        adapter_search.setOnItemClickListener(object : RVAdapterSavedSearch.onItemClickListener {
            override fun onItemClick(position: Int) {
                MainScope().launch {
                    withContext(Dispatchers.Main) {
                        var searchID = source[position].id_search
                        println("$searchID")
                        var search = db.collection("Research").document(searchID).get().await()
                            .toObject<Research>()!!

                        searchResultHM = researchResult(search)
                        println("Risultati ricerca HashMap = $searchResultHM")
                        resultAR = getItemId(searchResultHM)
                        // a questo punto in resultAR abbiamo una lista di id da passare all'altra attività
                        val bundle = Bundle()
                        bundle.putStringArrayList("toLoad", resultAR)
                        bundle.putInt("code", 1)
                        miscresfragment.arguments = bundle
                        val transaction = supportFragmentManager.beginTransaction()
                        transaction.add(R.id.fragment_holder, miscresfragment).commit()
                    }
                }
            }
        })
        println("recycler view attivata Ricerche")
    }

    private fun getItemId(map: HashMap<Item, String>): java.util.ArrayList<String> {
        val res = arrayListOf<String>()
        for (items in map) {
            res.add(items.value)
        }
        for (id in res) {
            println(id)
        }
        return res
    }

    private suspend fun researchResult(research: Research): HashMap<Item, String> {
        val map: HashMap<Item, String> = research.queryNome(db, currentUser)

        if (map.isEmpty()) {
            println("Nessun Oggetto trovato")
            return map
        }

        var resMap: HashMap<Item, String> = research.queryShipping(map)

        if (resMap.isEmpty()) {
            println("Nessun Oggetto trovato")
            return hashMapOf()
        } else {
            map.clear()
            map.putAll(resMap)
        }

        if (research.searchPos?.isNotEmpty() == true && research.searchDistance != -1.0 && lat != 0.0 && long != 0.0) {
            resMap = research.queryPos(map)

            if (resMap.isEmpty()) {
                println("Nessun Oggetto trovato")
                return resMap
            } else {
                map.clear()
                map.putAll(resMap)
            }
        }

        if (research.searchPriceMin?.isNotEmpty() == true || research.searchPriceMax?.isNotEmpty() == true) {
            resMap = research.queryPrice(map)

            if (resMap.isEmpty()) {
                println("Nessun Oggetto trovato")
                return resMap
            } else {
                map.clear()
                map.putAll(resMap)
            }
        }

        return map
    }


    private fun startRecycler(
        reciclerView: RecyclerView,
        source: ArrayList<ItemCardProfile>,
        shimmer: ShimmerFrameLayout,
        totalItemsLoaded: Int,
        itemsIds: ArrayList<String>,
        tipo: String
    ) {
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
        reciclerView.visibility = View.VISIBLE
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        reciclerView.layoutManager = layoutManager
        adapter = RVAdapterProfile(source)
        reciclerView.adapter = adapter
        adapter.setOnItemClickListener(object : RVAdapterProfile.onItemClickListener {
            override fun onItemClick(position: Int) {
                var intent = Intent(this@ProfileActivity, ItemActivity::class.java)
                intent.putExtra("id", source.get(position).id_item)
                startForResult.launch(intent)
            }
        })

        val scrollListener = object : LazyLoadingScrollListener(layoutManager) {
            override fun onLoadMore(): Boolean {
                val itemsToLoad = getIdToLoad(totalItemsLoaded, itemsIds)
                if (itemsToLoad.isNotEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch {
                        var itemCardToLoad = idToItemCardList(itemsToLoad)
                        source.addAll(itemCardToLoad)
                        when {
                            tipo == "Acquisti" -> totalItemsLoaded_acquisti = +itemsToLoad.size
                            tipo == "Vendita" -> totalItemsLoaded_invendita = +itemsToLoad.size
                        }
                        itemsToLoad.clear()
                        itemCardToLoad.clear()
                        adapter.notifyDataSetChanged()
                    }
                    return true
                }
                return false
            }

        }
        reciclerView.addOnScrollListener(scrollListener)
        println("recycler view attivata")
    }

    private suspend fun idToItemCardList(ids: ArrayList<String>): ArrayList<ItemCardProfile> {
        val resultList = arrayListOf<ItemCardProfile>()
        val snapshot = db.collection("Item").get().await()
        for (document in snapshot.documents) {
            for (id in ids) {
                if (document.id.equals(id)) {
                    val itemFull: Item = document.toObject(Item::class.java)!!
                    val card = itemToCard(itemFull, document.id)
                    resultList.add(card!!)
                }
            }
        }
        println("Trasformati id in Item, size : ${resultList.size}")
        println(resultList)
        return resultList
    }

    private suspend fun itemToCard(item: Item, id: String): ItemCardProfile? {

        val image = item.Foto.getOrNull(0)
        if (image.isNullOrEmpty()) {
            Toast.makeText(this, getString(R.string.toast_photo_err), Toast.LENGTH_LONG).show()
            return null
        }
        //decode della foto
        val storageFile = FirebaseStorage.getInstance().getReference("images/$image")
        val localFile: File = File.createTempFile("tmp", "jpg")
        storageFile.getFile(localFile).await()
        val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)

        //carico l'oggetto nella item card
        val itemCardProfile =
            ItemCardProfile(id, bitmap, item.Nome, item.Prezzo.toString())
        return itemCardProfile
    }

    private suspend fun queryItemsIds(field: String): ArrayList<String> {
        var itemIds = arrayListOf<String>()
        val query = db.collection("Item").whereEqualTo(field, currentUser.Username)
        val snapshot = query.get().await()
        if (snapshot.isEmpty) {
            println("La query non ha fornito nessun risultato field: $field, username : ${currentUser.Username}")
        }
        for (doc in snapshot) {
            if (doc.get("Venduto") == false && field == "Proprietario") {
                itemIds.add(doc.id)
            } else if (doc.get("Venduto") == true && field == "Acquirente") {
                itemIds.add(doc.id)
            }
        }
        println("Total items retrived '$field': ${itemIds.size}")
        itemIds.sortBy { it }
        return itemIds
    }

    private suspend fun queryItemsIds_pref(): ArrayList<String> {

        if (currentUser.Preferiti.isEmpty()) {
            println("L'utente non ha aggiunto nessun preferito")
            return arrayListOf()
        }
        var itemIds = arrayListOf<String>()
        val snapshot = db.collection("Item").get().await()
        if (snapshot.isEmpty) {
            println("errore retrival items")
        }
        for (doc in snapshot) {
            for (id in currentUser.Preferiti) {
                if (doc.id.equals(id.key) && doc.get("Venduto") == false) {
                    itemIds.add(doc.id)
                }
            }
        }
        println("Total items retrived 'Preferiti': ${itemIds.size}")
        itemIds.sortBy { it }
        return itemIds
    }

    private fun getIdToLoad(totalItemsLoaded: Int, itemsIds: ArrayList<String>): ArrayList<String> {
        val idsToLoad = arrayListOf<String>()
        var count = totalItemsLoaded
        val limit = totalItemsLoaded + 6
        while (count < limit && count < itemsIds.size) {
            idsToLoad.add(itemsIds[count])
            count++
        }
        return idsToLoad
    }


}
