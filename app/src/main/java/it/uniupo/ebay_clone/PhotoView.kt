package it.uniupo.ebay_clone

import android.content.Context
import android.graphics.Bitmap
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.Shader.TileMode
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout


class PhotoView : Fragment() {

    private lateinit var result: PhotoResult
    interface PhotoResult {
        fun closeFrag()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            result = context as PhotoResult
        } catch (e: Exception) {
            println("Photo Exception: $e")
        }
    }
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeBtn = view.findViewById<ImageButton>(R.id.close_btn)


        view.findViewById<ImageView>(R.id.frag_img).setImageBitmap(this.requireArguments().getParcelable("photo"))

        closeBtn.setOnClickListener {
            val fragmentManager = requireActivity().supportFragmentManager
            fragmentManager.beginTransaction().remove(this).commit()
            result.closeFrag()
        }

        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val fragmentManager = requireActivity().supportFragmentManager
                fragmentManager.beginTransaction().remove(this@PhotoView).commit()
                result.closeFrag()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(onBackPressedCallback)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        return inflater.inflate(R.layout.fragment_photo_view, container, false)
    }

    fun closeFrag() {

    }

    companion object{
        fun newInstance(bitmap: Bitmap) : PhotoView{
            val fragment = PhotoView()
            val args = Bundle()
            args.putParcelable("photo", bitmap)
            fragment.arguments = args
            return fragment
        }
    }

}