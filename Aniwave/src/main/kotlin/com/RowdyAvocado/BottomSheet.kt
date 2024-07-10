package com.RowdyAvocado

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class BottomFragment(private val plugin: AniwavePlugin) : BottomSheetDialogFragment() {
    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val id =
                plugin.resources!!.getIdentifier(
                        "bottom_sheet_layout",
                        "layout",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
        val layout = plugin.resources!!.getLayout(id)
        val view = inflater.inflate(layout, container, false)
        val outlineId =
                plugin.resources!!.getIdentifier(
                        "outline",
                        "drawable",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )

        // building save button and settings click listener
        val saveIconId =
                plugin.resources!!.getIdentifier(
                        "save_icon",
                        "drawable",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
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

        // building simkl sync switch and settings click listener
        val simklSyncSwitch = view.findView<Switch>("simkl_sync")
        simklSyncSwitch.isChecked = AniwavePlugin.aniwaveSimklSync
        simklSyncSwitch.background = plugin.resources!!.getDrawable(outlineId, null)
        simklSyncSwitch.setOnClickListener(
                object : OnClickListener {
                    override fun onClick(btn: View?) {
                        AniwavePlugin.aniwaveSimklSync = simklSyncSwitch.isChecked
                    }
                }
        )

        // building server options and settings click listener
        val serverGroup = view.findView<RadioGroup>("server_group")
        val radioBtnId =
                plugin.resources!!.getIdentifier(
                        "radio_button",
                        "layout",
                        BuildConfig.LIBRARY_PACKAGE_NAME
                )
        ServerList.values().forEach { server ->
            val radioBtnLayout = plugin.resources!!.getLayout(radioBtnId)
            val radioBtnView = inflater.inflate(radioBtnLayout, container, false)
            val radioBtn = radioBtnView.findView<RadioButton>("radio_button")
            radioBtn.text = server.link
            val newId = View.generateViewId()
            radioBtn.id = newId
            radioBtn.background = plugin.resources!!.getDrawable(outlineId, null)
            radioBtn.setOnClickListener(
                    object : OnClickListener {
                        override fun onClick(btn: View?) {
                            AniwavePlugin.currentAniwaveServer = radioBtn.text.toString()
                            serverGroup.check(newId)
                        }
                    }
            )
            serverGroup.addView(radioBtnView)
            if (AniwavePlugin.currentAniwaveServer.equals(server.link)) serverGroup.check(newId)
        }
        return view
    }

    private fun <T : View> View.findView(name: String): T {
        val id = plugin.resources!!.getIdentifier(name, "id", BuildConfig.LIBRARY_PACKAGE_NAME)
        return this.findViewById(id)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        (dialog as? BottomSheetDialog)?.behavior?.state = BottomSheetBehavior.STATE_EXPANDED
        return dialog
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }
}
