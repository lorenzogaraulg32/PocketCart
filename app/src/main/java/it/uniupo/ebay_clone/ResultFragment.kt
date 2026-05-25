package it.uniupo.ebay_clone

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.facebook.shimmer.ShimmerFrameLayout
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

class ResultFragment : Fragment() {

    private lateinit var shimmer: ShimmerFrameLayout
    private lateinit var recycler: RecyclerView
    private lateinit var noSearchText: TextView
    private lateinit var db: FirebaseFirestore

    private var code = 0

    private var allItemsIDs = arrayListOf<String>()
    private var itemsToLoad = arrayListOf<String>()
    private var rvSource = arrayListOf<ItemCard>()
    private lateinit var adapter: RVAdapter
    private var totalItemsLoaded = 0
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        shimmer = view.findViewById(R.id.shimmer)
        recycler = view.findViewById(R.id.rv_items)
        noSearchText = view.findViewById(R.id.blank_search)

        code = arguments?.getString("code").toString().toInt()
        println("Codice = $code")
        if (code == 1) {
            allItemsIDs = arguments?.getStringArrayList("searchResult")!!
        }

        if (allItemsIDs.isEmpty()) {
            shimmer.visibility = View.GONE
            recycler.visibility = View.GONE
            noSearchText.visibility = View.VISIBLE
        } else {
            MainScope().launch {
                withContext(Dispatchers.Main) {
                    noSearchText.visibility = View.GONE
                    shimmer.visibility = View.VISIBLE
                    recycler.visibility = View.GONE
                    shimmer.startShimmer()
                    itemsToLoad = getIdToLoad()
                    val itemCardToLoad = idToItemCardList(itemsToLoad)
                    rvSource.addAll(itemCardToLoad)
                    itemsToLoad.clear()
                    startRecycler(shimmer,recycler)
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


    private fun startRecycler(shimmer: ShimmerFrameLayout, recycler: RecyclerView) {
        shimmer.stopShimmer()
        shimmer.visibility = View.GONE
        recycler.visibility = View.VISIBLE
        val layoutManager = LinearLayoutManager(requireContext())
        recycler.layoutManager = layoutManager
        adapter = RVAdapter(rvSource)
        recycler.adapter = adapter
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
        recycler.addOnScrollListener(scrollListener)
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
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_result, container, false)
    }



}