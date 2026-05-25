package it.uniupo.ebay_clone

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.system.measureTimeMillis

class HomeFragment : Fragment() {


    private var auth: FirebaseAuth? = null
    private lateinit var db: FirebaseFirestore
    private lateinit var currentUser: User

    //recycler view
    private var allItemsIDs = arrayListOf<String>()
    private var itemsToLoad = arrayListOf<String>()
    private var rvSource = arrayListOf<ItemCard>()
    private lateinit var adapter: RVAdapter
    private var totalItemsLoaded = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_items)
        recyclerView.visibility = View.GONE

        val shimmer = view.findViewById<ShimmerFrameLayout>(R.id.shimmer)
        shimmer.visibility = View.VISIBLE
        shimmer.startShimmer()

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()


        MainScope().launch {
            withContext(Dispatchers.Main) {
                currentUser = User().getCurrentUser(auth!!, db)
                if (currentUser.Status != 2 && currentUser.Status != 3) {
                    var elapsed = measureTimeMillis {
                        allItemsIDs = queryItemsIds()
                    }
                    println("Time to fetch all the ids: $elapsed")
                    itemsToLoad = getIdToLoad()
                    elapsed = measureTimeMillis {
                        val itemCardToLoad = idToItemCardList(itemsToLoad)
                        rvSource.addAll(itemCardToLoad)
                        itemsToLoad.clear()
                    }
                    println("Time to transale id into card: $elapsed")
                    startRecycler(shimmer,recyclerView)

                }

            }

        }

    }

    private suspend fun idToItemCardList(ids: java.util.ArrayList<String>): java.util.ArrayList<ItemCard> {
        val resultList: java.util.ArrayList<ItemCard> = arrayListOf()
        val snapshot = db.collection("Item").get().await()
        for (document in snapshot.documents) {
            for (id in ids) {
                if (document.id == id) {
                    val itemFull: Item = document.toObject(Item::class.java)!!
                    val card = itemToCard(itemFull, document.id)
                    resultList.add(card!!)
                }
            }
        }
        println("Trasformati id in Item, size : ${resultList.size}")
        println("Total Items Retrived : $totalItemsLoaded")
        println(resultList)
        return resultList
    }

    private suspend fun queryItemsIds(): ArrayList<String> {
        val itemIds = arrayListOf<String>()
        val snapshot = db.collection("Item").whereEqualTo("Venduto", false).get().await()
        for (doc in snapshot) {
            if (doc.get("Proprietario") != currentUser.Username) {
                itemIds.add(doc.id)
            }
        }
        println("Total items Retrived : ${itemIds.size}")
        itemIds.sortBy { it }
        return itemIds
    }

    private fun startRecycler(shimmer: ShimmerFrameLayout, recyclerView: RecyclerView) {
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
        val layoutManager = LinearLayoutManager(requireContext())
        recyclerView.layoutManager = layoutManager
        adapter = RVAdapter(rvSource)
        recyclerView.adapter = adapter
        adapter.setOnItemClickListener(object : RVAdapter.onItemClickListener {
            override fun onItemClick(position: Int) {
                val intent = Intent(requireContext(), ItemActivity::class.java)
                intent.putExtra("id", rvSource[position].id_item)
                startActivity(intent)
            }
        })

        val scrollListener = object : LazyLoadingScrollListener(layoutManager) {
            @SuppressLint("NotifyDataSetChanged")
            override fun onLoadMore(): Boolean {
                val itemsToLoad = getIdToLoad()
                if (itemsToLoad.isNotEmpty()) {
                    val elapsed = measureTimeMillis {
                        CoroutineScope(Dispatchers.Main).launch {
                            val itemCardToLoad = idToItemCardList(itemsToLoad)
                            rvSource.addAll(itemCardToLoad)
                            itemsToLoad.clear()
                            adapter.notifyDataSetChanged()
                        }
                    }
                    println("Time to transale id into card: $elapsed")
                    return true
                }
                return false
            }
        }
        recyclerView.addOnScrollListener(scrollListener)
        println("recycler view attivata")
    }


    private suspend fun itemToCard(item: Item, id: String): ItemCard? {
        val image = item.Foto.getOrNull(0)
        if (image.isNullOrEmpty()) {
            Toast.makeText(requireContext(), getString(R.string.toast_photo_err), Toast.LENGTH_LONG).show()
            return null
        }
        //decode della foto
        val storageFile = FirebaseStorage.getInstance().getReference("images/$image")
        val localFile: File = File.createTempFile("tmp", "jpg")
        storageFile.getFile(localFile).await()
        val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
        //carico l'oggetto nella item card
        val itemCard = ItemCard(id, bitmap, item.Nome, item.Prezzo.toString(), item.Spedizione)
        return itemCard
    }

    private fun getIdToLoad(): java.util.ArrayList<String> {
        val idsToLoad: java.util.ArrayList<String> = arrayListOf()
        val limit = totalItemsLoaded + 6
        while (totalItemsLoaded < limit && totalItemsLoaded < allItemsIDs.size) {
            idsToLoad.add(allItemsIDs[totalItemsLoaded])
            totalItemsLoaded++
        }
        return idsToLoad
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

}