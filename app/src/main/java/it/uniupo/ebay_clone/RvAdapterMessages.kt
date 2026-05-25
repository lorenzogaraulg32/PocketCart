package it.uniupo.ebay_clone


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import it.uniupo.ebay_clone.databinding.CardMessageChatEndBinding
import it.uniupo.ebay_clone.databinding.CardMessageChatStartBinding


class RvAdapterMessages(private val list: ArrayList<MessageCard>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val FIRST_VIEW = 0
        const val SECOND_VIEW = 1

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            FIRST_VIEW -> MessageCardRigth(
                CardMessageChatEndBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )

            SECOND_VIEW -> MessageCardLeft(
                CardMessageChatStartBinding.inflate(
                    LayoutInflater.from(
                        parent.context
                    ), parent, false
                )
            )

            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        return when (list[position].type) {
            FIRST_VIEW -> (holder as MessageCardRigth).bind(list[position])
            SECOND_VIEW -> (holder as MessageCardLeft).bind(list[position])
            else -> throw IllegalArgumentException("Invalid item type")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return list[position].type
    }
}