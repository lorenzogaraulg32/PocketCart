package it.uniupo.ebay_clone

import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RVAdapterProfile(private val itemArrayList: ArrayList<ItemCardProfile>) :
    RecyclerView.Adapter<RVAdapterProfile.ViewHolder>() {

    private lateinit var clickListener : onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener : RVAdapterProfile.onItemClickListener){
        clickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_view_item_profile, parent, false)
        return ViewHolder(view, clickListener)
    }

    override fun getItemCount(): Int {
        return itemArrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemArrayList[position]
        holder.img.setImageBitmap(item.image)
        holder.itemName.text = item.nome_item?.uppercase()
        holder.itemPrice.text = Item().ScaleDouble(item.prezzo_item!!.toDouble())+"€"
        holder.img.scaleType = ImageView.ScaleType.CENTER_CROP
    }

    class ViewHolder(itemView: View, listener : onItemClickListener) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.item_img_card)
        val itemName: TextView = itemView.findViewById(R.id.item_name_card)
        val itemPrice: TextView = itemView.findViewById(R.id.item_price_card)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}