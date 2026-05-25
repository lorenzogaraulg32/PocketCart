package it.uniupo.ebay_clone

class Recensioni(
    var value: Int?,
    var comment: String?,
    var username :String?,
    var item: String?
){
    constructor() : this(null, null, null, null)
}