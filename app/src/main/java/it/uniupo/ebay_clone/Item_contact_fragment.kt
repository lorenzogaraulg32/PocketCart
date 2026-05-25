package it.uniupo.ebay_clone

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class Item_contact_fragment : Fragment() {

    val dataBase = FirebaseFirestore.getInstance()
    private var sellerUser: User = User()
    private var currentUser: User = User()
    private lateinit var auth: FirebaseAuth
    private lateinit var item: Item


    private var rewievs: ArrayList<RewCard> = arrayListOf()
    private lateinit var adapter: RVAdapterReviewsSmall

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser

        val recycler = requireView().findViewById<RecyclerView>(R.id.rew_recycler)
        item = requireArguments().getParcelable("item")!!

        MainScope().launch() {
            withContext(Dispatchers.Main) {

                currentUser = User().getCurrentUser(auth, dataBase)
                sellerUser = getSeller()
                getRew(sellerUser)
                println(currentUser.Username + item.Proprietario)
                startRecycler(recycler)
            }
            try {
                requireActivity().runOnUiThread {
                    if (currentUser.Username == item.Proprietario) {
                        view.findViewById<Button>(R.id.chat_btn).visibility = View.GONE
                    }
                    view.findViewById<TextView>(R.id.seller_name).text = sellerUser.Username
                    view.findViewById<RatingBar>(R.id.user_rating).rating =
                        sellerUser.Valutazione.toFloat()
                    view.findViewById<ProgressBar>(R.id.loading).stopNestedScroll()
                    view.findViewById<ProgressBar>(R.id.loading).visibility = View.GONE
                    view.findViewById<ConstraintLayout>(R.id.layout1).visibility = View.VISIBLE
                    view.findViewById<ConstraintLayout>(R.id.layout2).visibility = View.VISIBLE
                }
            }catch (e:Exception){
                println("do nothing but dont crash")
            }
        }



        view.findViewById<Button>(R.id.chat_btn).setOnClickListener {
            if (currentUser.Username != item.Proprietario) {
                val intent = Intent(requireContext(), ChatView::class.java)
                intent.putExtra("senderId", user!!.uid)
                intent.putExtra("sender", currentUser.Username)
                intent.putExtra("receiver", item.Proprietario)
                startActivity(intent)
            }
        }
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                requireActivity().finish()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)
    }

    private suspend fun getSeller(): User {
        return dataBase.collection("User").whereEqualTo("Username", item.Proprietario).get()
            .await().documents[0].toObject(User::class.java)!!
    }

    private fun startRecycler(recycler: RecyclerView) {
        try {
            adapter = RVAdapterReviewsSmall(rewievs)
            val layoutManager =
                LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            recycler.layoutManager = layoutManager
            recycler.adapter = adapter

            adapter.setOnItemClickListener(object : RVAdapterReviewsSmall.onItemClickListener {
                override fun onItemClick(position: Int) {
                }
            })
        }catch(e:Exception){
            println("do nothing but dont crash xd")
        }
    }

    private suspend fun getRew(user: User) {
        for (r in user.Valutazioni) {
            val item =
                dataBase.collection("Item").document(r.item!!).get().await().toObject<Item>()!!
            rewievs.add(
                RewCard(
                    r.value!!,
                    r.comment!!.uppercase(),
                    r.username!!.uppercase(),
                    item.Nome.uppercase()
                )
            )
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_item_contact_fragment, container, false)
    }

    companion object {
        fun newInstance(item: Item): Item_contact_fragment {
            val fragment = Item_contact_fragment()
            val args = Bundle()
            args.putParcelable("item", item)
            fragment.arguments = args
            return fragment
        }
    }
}