package com.RowdyAvocado

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.lagradost.cloudstream3.CommonActivity.showToast
import com.lagradost.cloudstream3.utils.SubtitleHelper
import kotlinx.coroutines.launch

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MangaDexChapterProvidersFragment(
        val plugin: MangaDexPlugin,
        val chapterGroup: MutableList<ChapterData>
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
        val chapterProvidersLayoutId =
                plugin.resources!!.getIdentifier("chapter_providers", "layout", "com.RowdyAvocado")
        val chapterProvidersLayout = plugin.resources!!.getLayout(chapterProvidersLayoutId)
        val chapterProvidersView = inflater.inflate(chapterProvidersLayout, container, false)
        val chapterDetailsLayoutId =
                plugin.resources!!.getIdentifier("provider_details", "layout", "com.RowdyAvocado")
        val backgroundId =
                plugin.resources!!.getIdentifier("background", "drawable", "com.RowdyAvocado")

        // Building list of chapter providers and its click listener
        val providersListLayout = chapterProvidersView.findView<LinearLayout>("chapter_list")
        chapterGroup.forEach { chapter ->

            // collecting required resources
            val providerDetailsLayout = plugin.resources!!.getLayout(chapterDetailsLayoutId)
            val providerDetailsLayoutView =
                    inflater.inflate(providerDetailsLayout, container, false)
            val providerDetailView =
                    providerDetailsLayoutView.findView<LinearLayout>("details_layout")
            val titleView = providerDetailsLayoutView.findView<TextView>("title")
            val pageCountView = providerDetailsLayoutView.findView<TextView>("page_count")
            val providerNameView = providerDetailsLayoutView.findView<TextView>("provider_name")
            val providerTypeView = providerDetailsLayoutView.findView<TextView>("provider_type")

            // configuring provider details and its click listener
            providerDetailView.background = plugin.resources!!.getDrawable(backgroundId, null)
            titleView.text =
                    SubtitleHelper.getFlagFromIso(chapter.attrs.translatedLanguage) +
                            " " +
                            if (chapter.attrs.title.isNullOrBlank())
                                    "Chapter " + (chapter.attrs.chapter ?: "0")
                            else chapter.attrs.title
            pageCountView.text = "Pages: " + (chapter.attrs.pages ?: "0")
            providerNameView.text =
                    "Provider: " +
                            chapter.rel.find { it.type.contains("scanlation_group") }?.attrs?.name
            providerTypeView.text =
                    if (chapter.attrs.externalUrl.isNullOrBlank()) "internal" else "external"

            providerDetailsLayoutView.setOnClickListener(
                    object : OnClickListener {
                        override fun onClick(btn: View) {
                            lifecycleScope.launch { fetchAndLoadChapter(chapter) }
                        }
                    }
            )
            providersListLayout.addView(providerDetailsLayoutView)
        }
        return chapterProvidersView
    }

    suspend fun fetchAndLoadChapter(chapter: ChapterData) {
        val chapterName =
                if (chapter.attrs.title.isNullOrBlank()) "Chapter " + chapter.attrs.chapter
                else chapter.attrs.title!!
        val pages = MangaDexExtractor(plugin).getPages(chapter)
        if (pages.isNotEmpty()) plugin.loadChapter(chapterName, pages)
        else showToast("Provider not yet supported")
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {}
}
