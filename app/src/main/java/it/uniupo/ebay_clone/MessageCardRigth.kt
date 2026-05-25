package it.uniupo.ebay_clone

import androidx.recyclerview.widget.RecyclerView
import it.uniupo.ebay_clone.databinding.CardMessageChatEndBinding

class MessageCardRigth(private val binding: CardMessageChatEndBinding) :
    RecyclerView.ViewHolder(binding.root) {
    fun bind(dataModel: MessageCard) {
        binding.nameMsg.text = dataModel.nome
        binding.dateMsg.text = dataModel.data
        binding.textViewMsg.text = dataModel.msg
    }
}