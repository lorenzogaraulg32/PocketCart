package it.uniupo.ebay_clone

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.browser.browseractions.BrowserActionsIntent.BrowserActionsItemId
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class Item_info_fragment : Fragment() {

    val dataBase = FirebaseFirestore.getInstance()
    private var sellerUser: User = User()
    private lateinit var auth: FirebaseAuth
    private var currentUser: User = User()
    private lateinit var item: Item
    private lateinit var id: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        id = requireArguments().getString("itemId")!!
        item = requireArguments().getParcelable("item")!!
        MainScope().launch() {
            withContext(Dispatchers.Main) {
                sellerUser = getSeller()
                currentUser = User().getCurrentUser(auth, dataBase)
            }
            try {
            requireActivity().runOnUiThread {
                if (item.Venduto || sellerUser.Username.lowercase() == currentUser.Username.lowercase()) {
                    view.findViewById<Button>(R.id.buy_item_now_btn).visibility = View.GONE
                }
                view.findViewById<TextView>(R.id.item_description).text = item.Descrizione
                view.findViewById<TextView>(R.id.conditions).text = setConditionText()
                view.findViewById<TextView>(R.id.address_shipping).text = getAddressInFormat()
                view.findViewById<ProgressBar>(R.id.loading).stopNestedScroll()
                view.findViewById<ProgressBar>(R.id.loading).visibility = View.GONE
                view.findViewById<ConstraintLayout>(R.id.layout1).visibility = View.VISIBLE
            }
            }catch (e:Exception){
                println("do nothing but don't crash")
            }
        }


        view.findViewById<Button>(R.id.buy_item_now_btn).setOnClickListener {
            if (!item.Venduto && sellerUser.Username.lowercase() != currentUser.Username.lowercase()) {
                val intent = Intent(this.requireContext(), BuyActivity::class.java)
                intent.putExtra("item", id)
                startActivity(intent)
                requireActivity().finish()
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

    private fun getAddressInFormat(): String {
        if(item.Spedizione) {
            val rawAddress = item.getItemPosition(this.requireActivity())
            val tokens = rawAddress.split(",")
            return tokens[0].trim() + ",\n" + tokens[1].trim() + ",\n" + tokens[2].trim()
        }else{
            return getString(R.string.ritiro_in_loco)
        }
    }

    private fun setConditionText(): String {
        return when (item.Stato) {
            0 -> {
                getString(R.string.cond0)
            }

            1 -> {
                getString(R.string.cond1)
            }

            2 -> {
                getString(R.string.cond2)
            }

            3 -> {
                getString(R.string.cond3)
            }

            else -> {
                ""
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_item_info_fragment, container, false)
    }


    companion object {
        fun newInstance(item: Item, itemId: String): Item_info_fragment {
            val fragment = Item_info_fragment()
            val args = Bundle()
            args.putParcelable("item", item)
            args.putString("itemId", itemId)
            fragment.arguments = args
            return fragment
        }
    }

}