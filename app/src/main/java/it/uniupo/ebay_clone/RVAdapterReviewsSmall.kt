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

class RVAdapterReviewsSmall(private val smallRewArrayList: ArrayList<RewCard>) :
    RecyclerView.Adapter<RVAdapterReviewsSmall.ViewHolder>() {

    private lateinit var clickListener : onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener : RVAdapterReviewsSmall.onItemClickListener){
        clickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_rewiew_small, parent, false)
        return ViewHolder(view, clickListener)
    }



    override fun getItemCount(): Int {
        return smallRewArrayList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = smallRewArrayList[position]
        holder.rating.rating = item.rew.toString().toFloat()
        holder.comment.text = item.comment
    }

    class ViewHolder(itemView: View, listener : onItemClickListener) : RecyclerView.ViewHolder(itemView) {
        val rating: RatingBar = itemView.findViewById(R.id.user_rating)
        val comment: TextView = itemView.findViewById(R.id.rew_comment)
        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }
}