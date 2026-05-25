package it.uniupo.ebay_clone

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

abstract class LazyLoadingScrollListener(private val layoutManager: LinearLayoutManager) :
    RecyclerView.OnScrollListener() {

    private val visibleThreshold = 2
    private var loading = false


    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
        super.onScrolled(recyclerView, dx, dy)

        val totalItemCount = layoutManager.itemCount
        println("totalItemCount : $totalItemCount")
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

        if(!loading && totalItemCount <= (lastVisibleItemPosition + visibleThreshold)){
                loading = true
                onLoadMore()
            }
    }

    abstract fun onLoadMore(): Boolean
}