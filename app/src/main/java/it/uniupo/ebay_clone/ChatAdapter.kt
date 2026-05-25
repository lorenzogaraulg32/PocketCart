package it.uniupo.ebay_clone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter (private val mList: List<ChatViewModel>): RecyclerView.Adapter<ChatAdapter.ViewHolder>() {

    private lateinit var clickListener: onItemClickListener

    interface onItemClickListener{
        fun onItemClick(position: Int)
    }

    fun setOnItemClickListener(listener : onItemClickListener){
        clickListener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view= LayoutInflater.from(parent.context).inflate(R.layout.card_user_chat,parent,false)
        return ViewHolder(view,clickListener)
    }

    override fun getItemCount(): Int {
        return mList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chatViewModel=mList[position]
        holder.nomeReceiver.text=chatViewModel.nomeReceiver
        holder.lastMsg.text = chatViewModel.lastMsg
        holder.lastMsgDate.text = chatViewModel.lastMsgDate
    }

    class ViewHolder(ItemView: View,listener: onItemClickListener): RecyclerView.ViewHolder(ItemView){
        val nomeReceiver: TextView= itemView.findViewById(R.id.username)
        val lastMsg: TextView= itemView.findViewById(R.id.last_text)
        val lastMsgDate: TextView= itemView.findViewById(R.id.last_text_date)


        init {
            itemView.setOnClickListener {
                listener.onItemClick(adapterPosition)
            }
        }
    }

}
