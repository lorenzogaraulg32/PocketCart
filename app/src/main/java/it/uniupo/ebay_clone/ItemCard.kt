package it.uniupo.ebay_clone

import android.graphics.Bitmap


data class ItemCard(
    val id_item: String = "",
    val image: Bitmap? = null,
    val nome_item: String? = null,
    val prezzo_item: String? = null,
    val spedizione: Boolean = false,
)
