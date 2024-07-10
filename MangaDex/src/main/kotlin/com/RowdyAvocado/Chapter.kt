package com.RowdyAvocado

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.target.Target
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MangaDexChapterFragment(
        val plugin: MangaDexPlugin,
        val chapterName: String,
        val pages: List<String>
) : BottomSheetDialogFragment() {

    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private fun getDrawable(name: String): Drawable? {
        val id =
                plugin.resources!!.getIdentifier(name, "drawable", BuildConfig.LIBRARY_PACKAGE_NAME)
        return ResourcesCompat.getDrawable(plugin.resources!!, id, null)
    }

    private fun getString(name: String): String? {
        val id = plugin.resources!!.getIdentifier(name, "string", BuildConfig.LIBRARY_PACKAGE_NAME)
        return plugin.resources!!.getString(id)
    }

    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        return this.findViewById(id)
    }

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {

        // collecting required resources
        val chapterLayoutId =
                plugin.resources!!.getIdentifier("chapter", "layout", "com.RowdyAvocado")
        val chapterLayout = plugin.resources!!.getLayout(chapterLayoutId)
        val chapterView = inflater.inflate(chapterLayout, container, false)

        val chapterTitleTextView = chapterView.findView<TextView>("title")
        chapterTitleTextView.text = chapterName

        // creating pages using recyclerView
        val customAdapter = CustomAdapter(plugin, pages)
        val recyclerView: RecyclerView = chapterView.findView<RecyclerView>("page_list")
        recyclerView.setLayoutManager(LinearLayoutManager(context))
        recyclerView.adapter = customAdapter

        return chapterView
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {}
}

class CustomAdapter(val plugin: MangaDexPlugin, private val imageUrls: List<String>) :
        RecyclerView.Adapter<CustomAdapter.ViewHolder>() {

    class ViewHolder(val plugin: MangaDexPlugin, val view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView

        init {
            imageView = view.findView<ImageView>("page")
        }

        private fun <T : View> View.findView(name: String): T {
            val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
            return this.findViewById(id)
        }
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val pageLayoutId = plugin.resources!!.getIdentifier("page", "layout", "com.RowdyAvocado")
        val pageLayout = plugin.resources!!.getLayout(pageLayoutId)
        val view = LayoutInflater.from(viewGroup.context).inflate(pageLayout, viewGroup, false)
        return ViewHolder(plugin, view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        Glide.with(plugin.activity as FragmentActivity)
                .load(imageUrls[position])
                .transition(DrawableTransitionOptions.withCrossFade())
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(Target.SIZE_ORIGINAL)
                .into(viewHolder.imageView)
    }

    override fun getItemCount() = imageUrls.size
}
