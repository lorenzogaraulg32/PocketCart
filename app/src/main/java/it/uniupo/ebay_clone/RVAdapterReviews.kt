package it.uniupo.ebay_clone

import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class RVAdapterReviews(private val itemArrayList: ArrayList<RewCard>) :
    RecyclerView.Adapter<RVAdapterReviews.ViewHolder>() {

    private lateinit var clickListener : onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener : RVAdapterReviews.onItemClickListener){
        clickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_review, parent, false)
        return ViewHolder(view, clickListener)
    }



    override fun getItemCount(): Int {
        return itemArrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = itemArrayList[position]
        holder.rating.rating = item.rew.toString().toFloat()
        holder.comment.text = item.comment
        holder.user.text = item.username
        holder.item.text = item.item

    }

    class ViewHolder(itemView: View, listener : onItemClickListener) : RecyclerView.ViewHolder(itemView) {
        val rating: RatingBar = itemView.findViewById(R.id.reW_rating)
        val comment: TextView = itemView.findViewById(R.id.review_comment)
        val user: TextView = itemView.findViewById(R.id.review_username)
        val item: TextView = itemView.findViewById(R.id.review_item)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}