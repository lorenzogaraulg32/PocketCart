package it.uniupo.ebay_clone

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File


class Misc_res_fragment : Fragment() {

    //utente
    private lateinit var auth: FirebaseAuth
    private var db = Firebase.firestore
    private var currentUser: User = User()
    private var username_rew = ""

    //rv adapter
    private lateinit var id_search: ArrayList<String>
    private lateinit var rv_source: ArrayList<ItemCard>
    private lateinit var adapter: RVAdapter

    //ADAPTER RECENSIONI
    private lateinit var recensioni: ArrayList<Recensioni>
    private lateinit var rv_rew: ArrayList<RewCard>
    private lateinit var adapter_rew: RVAdapterReviews

    private var code = 0


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.misc_res_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        rv_source = arrayListOf()
        rv_rew = arrayListOf()

        val args = this.arguments
        if (args?.size() == 2) {
            code = args.getInt("code")
            try {
                id_search = args.getStringArrayList("toLoad")!!
                println("ricevuti items to load")
            } catch (e: Exception) {
                println(e)
            }
            try {
                username_rew = args.getString("username")!!
                println("ricevuto username")
            } catch (e: Exception) {
                println(e)
            }
        } else if (args?.size() == 1) {
            code = args.getInt("code")
        }
        val recycler = view.findViewById<RecyclerView>(R.id.rv_items)
        recycler?.visibility = View.GONE

        println("code = $code")

        MainScope().launch {
            withContext(Dispatchers.IO) {
                currentUser = User().getCurrentUser(auth, db)
            }
            if (code == 2) {
                //recycler delle mie recensioni
                view.findViewById<TextView>(R.id.review_switch).visibility = View.VISIBLE
                view.findViewById<TextView>(R.id.misc_title).text = getString(R.string.your_rew).toString()
                adapter_rew = RVAdapterReviews(rv_rew)
                getRew(currentUser, recycler)
            } else if (code == 3) {
                //recycler recensioni di qualcun altro
                view.findViewById<TextView>(R.id.misc_title).text = getString(R.string.reww)
                adapter_rew = RVAdapterReviews(rv_rew)
                println("Username : $username_rew")
                getRewUser(username_rew, recycler)
            } else {
                //recycler oggetti
                view.findViewById<TextView>(R.id.review_switch).visibility = View.GONE
                view.findViewById<TextView>(R.id.misc_title).text = getString(R.string.item_sold_title)
                adapter = RVAdapter(rv_source)
                getItems(
                    currentUser.Username,
                    recycler,
                    rv_source,
                )
            }


            view.findViewById<TextView>(R.id.review_switch).setOnClickListener {
                if (view.findViewById<TextView>(R.id.review_switch).text == getString(R.string.leaved)) {
                    view.findViewById<TextView>(R.id.misc_title).text = getString(R.string.reww)
                    view.findViewById<TextView>(R.id.review_switch).text = getString(R.string.recived)
                    startShimmer()
                    resetRecycler(recycler)
                    CoroutineScope(Dispatchers.Main).launch {
                        getRewSelf(currentUser.Username, recycler)
                    }
                } else if (view.findViewById<TextView>(R.id.review_switch).text == getString(R.string.recived)) {
                    view.findViewById<TextView>(R.id.misc_title).text = getString(R.string.your_rew)
                    view.findViewById<TextView>(R.id.review_switch).text = getString(R.string.leaved)
                    startShimmer()
                    resetRecycler(recycler)
                    CoroutineScope(Dispatchers.Main).launch {
                        getRew(currentUser, recycler)
                    }
                }
            }
        }
        //back btn
        view.findViewById<ImageButton>(R.id.back_btn).setOnClickListener {
            parentFragmentManager.beginTransaction().remove(this).commit()
        }
    }

    private fun stopShimmer() {
        try {
            val shimmer = view!!.findViewById<ShimmerFrameLayout>(R.id.shimmer)!!
            shimmer.visibility = View.INVISIBLE
            shimmer.stopShimmer()
        } catch (e: Exception) {
            println("$e")
        }
    }
    private fun startShimmer() {
        try {
            val shimmer = view!!.findViewById<ShimmerFrameLayout>(R.id.shimmer)!!
            shimmer.visibility = View.VISIBLE
            shimmer.startShimmer()
        } catch (e: Exception) {
            println("$e")
        }
    }

    private suspend fun getRew(currentUser: User, recycler: RecyclerView?) {
        var recensioni = ArrayList<RewCard>()
        for (r in currentUser.Valutazioni) {
            var item = db.collection("Item").document(r.item!!).get().await().toObject<Item>()!!
            recensioni.add(
                RewCard(
                    r.value!!,
                    r.comment!!.uppercase(),
                    r.username!!.uppercase(),
                    item.Nome.uppercase()
                )
            )
        }
        startRecyclerRew(recycler!!, recensioni)
    }

    private suspend fun getRewSelf(username: String, recycler: RecyclerView?) {
        var recensioni = ArrayList<RewCard>()
        //per tutti gli utenti
        for (u in db.collection("User").get().await().documents) {
            var user = u.toObject<User>()!!
            //cerco tra le loro recensioni.
            for (r in user.Valutazioni) {
                var item = db.collection("Item").document(r.item!!).get().await().toObject<Item>()!!
                if (username == r.username) {
                    recensioni.add(
                        RewCard(
                            r.value!!,
                            r.comment!!.uppercase(),
                            item.Proprietario.uppercase(),
                            item.Nome.uppercase()
                        )
                    )
                }
            }
        }

        startRecyclerRew(recycler!!, recensioni)
    }

    private suspend fun getRewUser(username: String, recycler: RecyclerView?) {
        var recensioni = ArrayList<RewCard>()
        //per tutti gli utenti
        for (u in db.collection("User").get().await().documents) {
            var user = u.toObject<User>()!!
            //cerco tra le loro recensioni.
            for (r in user.Valutazioni) {
                var item = db.collection("Item").document(r.item!!).get().await().toObject<Item>()!!
                if (username == item.Proprietario) {
                    recensioni.add(
                        RewCard(
                            r.value!!,
                            r.comment!!.uppercase(),
                            r.username!!.uppercase(),
                            item.Nome.uppercase()
                        )
                    )
                }
            }
        }

        startRecyclerRew(recycler!!, recensioni)
    }

    private fun resetRecycler(recycler: RecyclerView) {
        rv_rew.clear()
        adapter_rew = RVAdapterReviews(rv_rew)
        recycler.adapter = adapter_rew
        adapter_rew.setOnItemClickListener(object : RVAdapterReviews.onItemClickListener {
            override fun onItemClick(position: Int) {

            }
        })
        println("Recycler resettata")
    }

    private fun startRecyclerRew(recycler: RecyclerView, recensioni: ArrayList<RewCard>) {
        recycler.visibility = View.VISIBLE
        stopShimmer()
        recycler.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter_rew = RVAdapterReviews(recensioni)
        recycler.adapter = adapter_rew
        adapter_rew.setOnItemClickListener(object : RVAdapterReviews.onItemClickListener {
            override fun onItemClick(position: Int) {

            }
        })


    }


    private fun startRecycler(
        reciclerView: RecyclerView,
        source: ArrayList<ItemCard>,

        ) {
        stopShimmer()
        reciclerView.visibility = View.VISIBLE
        reciclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = RVAdapter(source)
        reciclerView.adapter = adapter
        adapter.setOnItemClickListener(object : RVAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent(context, ItemActivity::class.java)
                intent.putExtra("id", source[position].id_item)
                startActivity(intent)
            }
        })
    }


    private suspend fun idToItemCardList(ids: ArrayList<String>): ArrayList<ItemCard> {
        val resultList = arrayListOf<ItemCard>()
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

    private suspend fun itemToCard(item: Item, id: String): ItemCard? {

        val image = item.Foto.getOrNull(0)
        if (image.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Errore retrival foto ", Toast.LENGTH_LONG).show()
            return null
        }
        //decode della foto
        val storageFile = FirebaseStorage.getInstance().getReference("images/$image")
        val localFile: File = File.createTempFile("tmp", "jpg")
        storageFile.getFile(localFile).await()
        val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)

        //carico l'oggetto nella item card
        val itemCard =
            ItemCard(
                id,
                bitmap,
                item.Nome,
                item.Prezzo.toString(),
                item.Spedizione
            )
        return itemCard
    }


    //recupero gli oggetti già venduti dall'utente e li mostro
    private fun getItems(
        username: String,
        reciclerView: RecyclerView?,
        source: ArrayList<ItemCard>,
    ) {
        db.collection("Item").whereEqualTo("Proprietario", username).whereEqualTo("Venduto", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
                    println("Errore caricamente recycler")
                    stopShimmer()
                }
                if (snapshot != null && !snapshot.isEmpty) {
                    val deferredList = mutableListOf<Deferred<Unit>>()

                    for (document in snapshot.documents) {
                        if (document.get("Venduto") == true) {
                            val itemFull: Item = document.toObject(Item::class.java)!!
                            val image1Name = itemFull.Foto.getOrNull(0)
                            val storageFile =
                                FirebaseStorage.getInstance().getReference("images/$image1Name")
                            val localFile: File = File.createTempFile("tmp", "jpg")

                            val deferred = CoroutineScope(Dispatchers.IO).async {
                                val bitmap: Bitmap? = try {
                                    storageFile.getFile(localFile).await()
                                    BitmapFactory.decodeFile(localFile.absolutePath)
                                } catch (e: Exception) {
                                    null
                                }
                                withContext(Dispatchers.Main) {
                                    val itemCard = ItemCard(
                                        document.id,
                                        bitmap,
                                        itemFull.Nome,
                                        itemFull.Prezzo.toString(),
                                        itemFull.Spedizione,
                                    )
                                    source.add(itemCard)
                                    adapter.notifyDataSetChanged()
                                }
                            }
                            deferredList.add(deferred)
                        }
                    }
                    CoroutineScope(Dispatchers.IO).launch {
                        deferredList.awaitAll()
                        withContext(Dispatchers.Main) {
                            source.sortBy { rv_source -> rv_source.id_item }
                            startRecycler(reciclerView!!, source)
                        }
                    }
                } else {
                    stopShimmer()
                    println("retrival fallito")
                }
            }
    }


}