package com.RowdyAvocado

import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class MangaDexSettings(val plugin: MangaDexPlugin) : BottomSheetDialogFragment() {

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
        val settingsLayoutId =
                plugin.resources!!.getIdentifier("settings", "layout", "com.RowdyAvocado")
        val settingsLayout = plugin.resources!!.getLayout(settingsLayoutId)
        val settings = inflater.inflate(settingsLayout, container, false)
        return settings
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // collecting required resources
        val outlineId = plugin.resources!!.getIdentifier("outline", "drawable", "com.RowdyAvocado")
        val saveIconId =
                plugin.resources!!.getIdentifier("save_icon", "drawable", "com.RowdyAvocado")

        // building save button and its click listener
        val saveBtn = view.findView<ImageView>("save")
        saveBtn.setImageDrawable(plugin.resources!!.getDrawable(saveIconId, null))
        saveBtn.background = plugin.resources!!.getDrawable(outlineId, null)
        saveBtn.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.reload(context)
                        Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show()
                        dismiss()
                    }
                }
        )

        val dataSaverSwitch = view.findView<Switch>("data_saver_switch")
        dataSaverSwitch.background = plugin.resources!!.getDrawable(outlineId, null)
        dataSaverSwitch.isChecked = plugin.dataSaver
        dataSaverSwitch.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View) {
                        plugin.dataSaver = dataSaverSwitch.isChecked
                    }
                }
        )
    }
}
