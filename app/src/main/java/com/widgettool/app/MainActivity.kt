package com.widgettool.app

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.widgettool.app.data.WidgetRepository
import com.widgettool.app.databinding.ActivityMainBinding
import com.widgettool.app.model.ClickAction
import com.widgettool.app.model.CountdownWidgetData
import com.widgettool.app.model.ImageWidgetData
import com.widgettool.app.model.WidgetItem
import com.widgettool.app.model.WidgetType
import com.widgettool.app.model.WoodenFishWidgetData
import com.widgettool.app.ui.WidgetListAdapter
import com.widgettool.app.widget.CountdownWidgetProvider
import com.widgettool.app.widget.ImageWidgetProvider
import com.widgettool.app.widget.WoodenFishWidgetProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var repository: WidgetRepository
    private lateinit var adapter: WidgetListAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = WidgetRepository.getInstance(this)

        initViews()
        loadWidgets()
    }

    override fun onResume() {
        super.onResume()
        loadWidgets()
    }

    private fun initViews() {
        binding.tvTitle.text = getString(R.string.app_name)

        adapter = WidgetListAdapter(
            onItemClick = { widget ->
                openWidgetEdit(widget)
            },
            onItemLongClick = { widget ->
                showDeleteDialog(widget)
            }
        )

        val isTablet = resources.getBoolean(R.bool.is_tablet)
        if (isTablet) {
            val spanCount = if (resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE) 3 else 2
            binding.rvWidgets.layoutManager = GridLayoutManager(this, spanCount)
        } else {
            binding.rvWidgets.layoutManager = LinearLayoutManager(this)
        }
        binding.rvWidgets.adapter = adapter

        binding.btnAdd.setOnClickListener {
            showAddWidgetDialog()
        }

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        binding.btnManage.setOnClickListener {
            toggleManageMode()
        }

        binding.tvSelectAll.setOnClickListener {
            if (adapter.isAllSelected()) {
                adapter.clearSelection()
                binding.tvSelectAll.text = getString(R.string.select_all)
            } else {
                adapter.selectAll()
                binding.tvSelectAll.text = getString(R.string.cancel)
            }
            updateDeleteButton()
        }

        binding.btnDeleteSelected.setOnClickListener {
            deleteSelectedWidgets()
        }
    }

    private fun toggleManageMode() {
        val isManage = !adapter.isManageMode()
        adapter.setManageMode(isManage)
        if (isManage) {
            binding.btnManage.text = getString(R.string.done)
            binding.btnAdd.visibility = android.view.View.GONE
            binding.btnSettings.visibility = android.view.View.GONE
            binding.bottomBar.visibility = android.view.View.VISIBLE
        } else {
            binding.btnManage.text = getString(R.string.manage)
            binding.btnAdd.visibility = android.view.View.VISIBLE
            binding.btnSettings.visibility = android.view.View.VISIBLE
            binding.bottomBar.visibility = android.view.View.GONE
        }
        binding.tvSelectAll.text = getString(R.string.select_all)
        updateDeleteButton()
    }

    private fun updateDeleteButton() {
        val count = adapter.getSelectedCount()
        binding.btnDeleteSelected.isEnabled = count > 0
        binding.btnDeleteSelected.alpha = if (count > 0) 1f else 0.5f
    }

    private fun deleteSelectedWidgets() {
        val ids = adapter.getSelectedIds()
        if (ids.isEmpty()) return
        AlertDialog.Builder(this)
            .setTitle("删除组件")
            .setMessage(String.format(getString(R.string.confirm_delete_selected), ids.size))
            .setPositiveButton("删除") { _, _ ->
                for (id in ids) {
                    repository.deleteWidget(id)
                }
                updateAllWidgets()
                loadWidgets()
                toggleManageMode()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun loadWidgets() {
        val widgets = repository.getWidgetList()
        adapter.submitList(widgets)
    }

    private fun showAddWidgetDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_widget, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<android.widget.LinearLayout>(R.id.optionImage).setOnClickListener {
            dialog.dismiss()
            addWidget(WidgetType.IMAGE)
        }
        dialogView.findViewById<android.widget.LinearLayout>(R.id.optionWoodenFish)
            .setOnClickListener {
                dialog.dismiss()
                addWidget(WidgetType.WOODEN_FISH)
            }
        dialogView.findViewById<android.widget.LinearLayout>(R.id.optionCountdown)
            .setOnClickListener {
                dialog.dismiss()
                addWidget(WidgetType.COUNTDOWN)
            }

        dialog.show()
    }

    private fun addWidget(type: WidgetType) {
        val id = repository.generateWidgetId()
        val name = when (type) {
            WidgetType.IMAGE -> "图片组件"
            WidgetType.WOODEN_FISH -> "木鱼组件"
            WidgetType.COUNTDOWN -> "倒计时组件"
        }

        val widget = WidgetItem(
            id = id,
            type = type,
            name = name,
            imageData = if (type == WidgetType.IMAGE) ImageWidgetData() else null,
            woodenFishData = if (type == WidgetType.WOODEN_FISH) WoodenFishWidgetData() else null,
            countdownData = if (type == WidgetType.COUNTDOWN) CountdownWidgetData() else null
        )

        repository.addWidget(widget)
        loadWidgets()
        openWidgetEdit(widget)
    }

    private fun openWidgetEdit(widget: WidgetItem) {
        when (widget.type) {
            WidgetType.IMAGE -> {
                val intent = Intent(this, ImageWidgetEditActivity::class.java)
                intent.putExtra("widget_id", widget.id)
                startActivity(intent)
            }
            WidgetType.WOODEN_FISH -> {
                val intent = Intent(this, WoodenFishWidgetEditActivity::class.java)
                intent.putExtra("widget_id", widget.id)
                startActivity(intent)
            }
            WidgetType.COUNTDOWN -> {
                val intent = Intent(this, CountdownWidgetEditActivity::class.java)
                intent.putExtra("widget_id", widget.id)
                startActivity(intent)
            }
        }
    }

    private fun showDeleteDialog(widget: WidgetItem) {
        AlertDialog.Builder(this)
            .setTitle("删除组件")
            .setMessage("确定要删除「${widget.name}」吗？")
            .setPositiveButton("删除") { _, _ ->
                repository.deleteWidget(widget.id)
                updateAllWidgets()
                loadWidgets()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun updateAllWidgets() {
        val imageIntent = Intent(this, ImageWidgetProvider::class.java)
        imageIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        sendBroadcast(imageIntent)

        val fishIntent = Intent(this, WoodenFishWidgetProvider::class.java)
        fishIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        sendBroadcast(fishIntent)

        val countdownIntent = Intent(this, CountdownWidgetProvider::class.java)
        countdownIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        sendBroadcast(countdownIntent)
    }

}
