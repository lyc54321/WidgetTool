package com.widgettool.app

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.RadioButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.widgettool.app.data.WidgetRepository
import com.widgettool.app.databinding.ActivityImageWidgetEditBinding
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

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                val savedPath = saveImageToInternal(uri)
                if (savedPath != null) {
                    imageData.imagePath = savedPath
                    loadPreviewImage(savedPath)
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

        when (imageData.clickAction) {
            ClickAction.NONE -> binding.rbNoAction.isChecked = true
            ClickAction.OPEN_APP -> binding.rbOpenApp.isChecked = true
            ClickAction.OPEN_URL -> binding.rbOpenUrl.isChecked = true
        }

        binding.etOpenAppPackage.setText(imageData.openAppPackage)
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
        }

        binding.btnSave.setOnClickListener {
            saveWidget()
            finish()
        }
    }

    private fun updateActionFields() {
        when (imageData.clickAction) {
            ClickAction.NONE -> {
                binding.etOpenAppPackage.visibility = android.view.View.GONE
                binding.etOpenUrl.visibility = android.view.View.GONE
            }
            ClickAction.OPEN_APP -> {
                binding.etOpenAppPackage.visibility = android.view.View.VISIBLE
                binding.etOpenUrl.visibility = android.view.View.GONE
            }
            ClickAction.OPEN_URL -> {
                binding.etOpenAppPackage.visibility = android.view.View.GONE
                binding.etOpenUrl.visibility = android.view.View.VISIBLE
            }
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun saveImageToInternal(uri: Uri): String? {
        return try {
            val fileName = getFileName(uri) ?: "image_${System.currentTimeMillis()}.jpg"
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

    private fun saveWidget() {
        widget?.let { w ->
            imageData.openAppPackage = binding.etOpenAppPackage.text.toString()
            imageData.openUrl = binding.etOpenUrl.text.toString()

            w.opacity = opacity
            w.imageData = imageData
            repository.updateWidget(w)

            val intent = Intent(this, ImageWidgetProvider::class.java)
            intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            sendBroadcast(intent)
        }
    }

}
