package com.widgettool.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.widgettool.app.data.WidgetRepository
import com.widgettool.app.databinding.ActivityImageWidgetEditBinding
import com.widgettool.app.model.AppInfo
import com.widgettool.app.model.ClickAction
import com.widgettool.app.model.ImageWidgetData
import com.widgettool.app.model.WidgetItem
import com.widgettool.app.widget.ImageWidgetProvider
import java.io.File
import java.io.FileOutputStream

class ImageWidgetEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityImageWidgetEditBinding
    private lateinit var repository: WidgetRepository
    private var widgetId: String = ""
    private var widget: WidgetItem? = null
    private var imageData: ImageWidgetData = ImageWidgetData()
    private var opacity: Int = 100
    private var selectedAppName: String = ""
    private var customSoundName: String = ""

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val savedPath = saveFileToInternal(uri, "image_")
                if (savedPath != null) {
                    imageData.imagePath = savedPath
                    loadPreviewImage(savedPath)
                }
            }
        }
    }

    private val pickSoundLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val savedPath = saveFileToInternal(uri, "sound_")
                if (savedPath != null) {
                    imageData.soundUri = savedPath
                    customSoundName = getFileName(uri) ?: "自定义音效"
                    binding.tvSelectedSound.text = customSoundName
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityImageWidgetEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WidgetRepository.getInstance(this)
        widgetId = intent.getStringExtra("widget_id") ?: ""

        loadWidgetData()
        initViews()
    }

    private fun loadWidgetData() {
        widget = repository.getWidgetById(widgetId)
        widget?.let { w ->
            opacity = w.opacity
            imageData = w.imageData?.copy() ?: ImageWidgetData()
        }
    }

    private fun initViews() {
        binding.tvTitle.text = widget?.name ?: getString(R.string.image_widget)

        binding.sbOpacity.progress = opacity
        binding.tvOpacityValue.text = "$opacity%"

        binding.cbSoundEnabled.isChecked = imageData.soundEnabled
        binding.layoutSoundOptions.visibility = if (imageData.soundEnabled) View.VISIBLE else View.GONE

        if (imageData.soundUri.isNotEmpty() && imageData.soundUri.startsWith("/")) {
            binding.rbCustomSound.isChecked = true
            binding.layoutCustomSound.visibility = View.VISIBLE
            val file = File(imageData.soundUri)
            customSoundName = file.name
            binding.tvSelectedSound.text = customSoundName
        } else {
            binding.rbDefaultSound.isChecked = true
            binding.layoutCustomSound.visibility = View.GONE
        }

        when (imageData.clickAction) {
            ClickAction.NONE -> binding.rbNoAction.isChecked = true
            ClickAction.OPEN_APP -> binding.rbOpenApp.isChecked = true
            ClickAction.OPEN_URL -> binding.rbOpenUrl.isChecked = true
        }

        if (imageData.openAppPackage.isNotEmpty()) {
            selectedAppName = getAppNameByPackage(imageData.openAppPackage)
            binding.tvSelectedApp.text = selectedAppName
        }

        binding.etOpenUrl.setText(imageData.openUrl)

        updateActionFields()

        if (imageData.imagePath.isNotEmpty()) {
            loadPreviewImage(imageData.imagePath)
        }

        binding.btnSelectImage.setOnClickListener {
            pickImage()
        }

        binding.sbOpacity.setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: android.widget.SeekBar?, progress: Int, fromUser: Boolean) {
                opacity = progress
                binding.tvOpacityValue.text = "$opacity%"
                binding.ivPreview.alpha = opacity / 100f
            }
            override fun onStartTrackingTouch(seekBar: android.widget.SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: android.widget.SeekBar?) {}
        })

        binding.rgClickAction.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbNoAction -> imageData.clickAction = ClickAction.NONE
                R.id.rbOpenApp -> imageData.clickAction = ClickAction.OPEN_APP
                R.id.rbOpenUrl -> imageData.clickAction = ClickAction.OPEN_URL
            }
            updateActionFields()
        }

        binding.cbSoundEnabled.setOnCheckedChangeListener { _, isChecked ->
            imageData.soundEnabled = isChecked
            binding.layoutSoundOptions.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        binding.rgSoundType.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbDefaultSound -> {
                    binding.layoutCustomSound.visibility = View.GONE
                    imageData.soundUri = ""
                }
                R.id.rbCustomSound -> {
                    binding.layoutCustomSound.visibility = View.VISIBLE
                }
            }
        }

        binding.btnSelectSound.setOnClickListener {
            pickSound()
        }

        binding.btnSelectApp.setOnClickListener {
            showAppPicker()
        }

        binding.btnSave.setOnClickListener {
            saveWidget()
            finish()
        }
    }

    private fun updateActionFields() {
        when (imageData.clickAction) {
            ClickAction.NONE -> {
                binding.layoutOpenApp.visibility = View.GONE
                binding.etOpenUrl.visibility = View.GONE
            }
            ClickAction.OPEN_APP -> {
                binding.layoutOpenApp.visibility = View.VISIBLE
                binding.etOpenUrl.visibility = View.GONE
            }
            ClickAction.OPEN_URL -> {
                binding.layoutOpenApp.visibility = View.GONE
                binding.etOpenUrl.visibility = View.VISIBLE
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun pickSound() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "audio/*"
        pickSoundLauncher.launch(Intent.createChooser(intent, "选择音效"))
    }

    private fun saveFileToInternal(uri: Uri, prefix: String): String? {
        return try {
            val fileName = getFileName(uri) ?: "${prefix}${System.currentTimeMillis()}"
            val file = File(filesDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME))
                }
            }
        }
        if (result == null) {
            result = uri.path?.let { path ->
                val cut = path.lastIndexOf('/')
                if (cut != -1) path.substring(cut + 1) else null
            }
        }
        return result
    }

    private fun loadPreviewImage(path: String) {
        val file = File(path)
        if (file.exists()) {
            Glide.with(this)
                .load(file)
                .centerCrop()
                .into(binding.ivPreview)
            binding.ivPreview.alpha = opacity / 100f
        }
    }

    private fun getInstalledApps(): List<AppInfo> {
        val pm = packageManager
        val apps = pm.getInstalledApplications(0)
        val appList = mutableListOf<AppInfo>()
        for (app in apps) {
            if ((app.flags and ApplicationInfo.FLAG_SYSTEM) == 0 || pm.getLaunchIntentForPackage(app.packageName) != null) {
                val appName = app.loadLabel(pm).toString()
                val icon = app.loadIcon(pm)
                appList.add(AppInfo(app.packageName, appName, icon))
            }
        }
        return appList.sortedBy { it.appName.lowercase() }
    }

    private fun getAppNameByPackage(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (e: Exception) {
            packageName
        }
    }

    private fun showAppPicker() {
        val apps = getInstalledApps()
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_picker, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.rvApps)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.select_app)
            .setView(dialogView)
            .create()

        val adapter = AppPickerAdapter(apps) { appInfo ->
            imageData.openAppPackage = appInfo.packageName
            selectedAppName = appInfo.appName
            binding.tvSelectedApp.text = selectedAppName
            dialog.dismiss()
        }
        recyclerView.adapter = adapter

        dialog.show()
    }

    private fun saveWidget() {
        widget?.let { w ->
            imageData.openUrl = binding.etOpenUrl.text.toString()

            w.opacity = opacity
            w.imageData = imageData
            repository.updateWidget(w)

            val intent = Intent(this, ImageWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            sendBroadcast(intent)
        }
    }

    inner class AppPickerAdapter(
        private val apps: List<AppInfo>,
        private val onAppClick: (AppInfo) -> Unit
    ) : RecyclerView.Adapter<AppPickerAdapter.AppViewHolder>() {

        inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val ivIcon: ImageView = itemView.findViewById(R.id.ivAppIcon)
            private val tvName: TextView = itemView.findViewById(R.id.tvAppName)
            private val tvPackage: TextView = itemView.findViewById(R.id.tvAppPackage)

            fun bind(app: AppInfo) {
                tvName.text = app.appName
                tvPackage.text = app.packageName
                app.icon?.let { ivIcon.setImageDrawable(it) }
                itemView.setOnClickListener { onAppClick(app) }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_app_picker, parent, false)
            return AppViewHolder(view)
        }

        override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
            holder.bind(apps[position])
        }

        override fun getItemCount(): Int = apps.size
    }
}

