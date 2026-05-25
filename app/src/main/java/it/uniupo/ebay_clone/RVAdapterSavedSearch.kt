package it.uniupo.ebay_clone

import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RVAdapterSavedSearch(private val itemArrayList: ArrayList<SearchCard>) :
    RecyclerView.Adapter<RVAdapterSavedSearch.ViewHolder>() {

    private lateinit var clickListener : onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener : RVAdapterSavedSearch.onItemClickListener){
        clickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_saved_search, parent, false)
        return ViewHolder(view, clickListener)
    }



    override fun getItemCount(): Int {
        return itemArrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemArrayList[position]
        holder.searchName.text = item.searchName?.uppercase()
        holder.searchPrice.text = item.searchPrice?.uppercase()
        holder.searchPos.text = item.searchPos?.uppercase()
        if(!item.searchShipping){
            holder.searchShipping.visibility = View.GONE
        }else{
            holder.searchShipping.visibility = View.VISIBLE
        }
    }

    class ViewHolder(itemView: View, listener : onItemClickListener) : RecyclerView.ViewHolder(itemView) {
        val searchName: TextView = itemView.findViewById(R.id.nome_search)
        val searchPrice: TextView = itemView.findViewById(R.id.price_search)
        val searchShipping: ImageView = itemView.findViewById(R.id.shipping)
        val searchPos: TextView = itemView.findViewById(R.id.dist_search)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}