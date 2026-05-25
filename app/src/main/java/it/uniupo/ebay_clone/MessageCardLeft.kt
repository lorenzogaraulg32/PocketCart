package it.uniupo.ebay_clone

import androidx.recyclerview.widget.RecyclerView
import it.uniupo.ebay_clone.databinding.CardMessageChatStartBinding

class MessageCardLeft(private val binding: CardMessageChatStartBinding) :
    RecyclerView.ViewHolder(binding.root) {
        fun bind(dataModel : MessageCard){
            binding.nameMsg.text = dataModel.nome
            binding.dateMsg.text = dataModel.data
            binding.textViewMsg.text = dataModel.msg
        }
    }