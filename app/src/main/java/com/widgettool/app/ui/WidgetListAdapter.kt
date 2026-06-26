package com.widgettool.app.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.widgettool.app.R
import com.widgettool.app.databinding.ItemWidgetBinding
import com.widgettool.app.model.WidgetItem
import com.widgettool.app.model.WidgetType
import java.io.File

class WidgetListAdapter(
    private val onItemClick: (WidgetItem) -> Unit,
    private val onItemLongClick: (WidgetItem) -> Unit
) : ListAdapter<WidgetItem, WidgetListAdapter.WidgetViewHolder>(DiffCallback()) {

    inner class WidgetViewHolder(private val binding: ItemWidgetBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: WidgetItem) {
            binding.tvWidgetName.text = item.name
            binding.tvWidgetType.text = getTypeString(item.type)

            when (item.type) {
                WidgetType.IMAGE -> {
                    binding.tvWidgetDesc.text = item.imageData?.let {
                        if (it.imagePath.isNotEmpty()) "已设置图片" else "未设置图片"
                    } ?: "未设置图片"
                    if (item.imageData?.imagePath?.isNotEmpty() == true) {
                        val file = File(item.imageData!!.imagePath)
                        if (file.exists()) {
                            Glide.with(binding.ivWidgetIcon.context)
                                .load(file)
                                .centerCrop()
                                .into(binding.ivWidgetIcon)
                        } else {
                            binding.ivWidgetIcon.setImageResource(R.drawable.ic_image)
                        }
                    } else {
                        binding.ivWidgetIcon.setImageResource(R.drawable.ic_image)
                    }
                }
                WidgetType.WOODEN_FISH -> {
                    binding.tvWidgetDesc.text = "功德: ${item.woodenFishData?.meritCount ?: 0}"
                    binding.ivWidgetIcon.setImageResource(R.drawable.ic_wooden_fish)
                }
                WidgetType.COUNTDOWN -> {
                    val seconds = item.countdownData?.remainingSeconds ?: 0
                    val mins = seconds / 60
                    val secs = seconds % 60
                    binding.tvWidgetDesc.text = String.format(
                        "%s: %02d:%02d",
                        item.countdownData?.label ?: "倒计时",
                        mins,
                        secs
                    )
                    binding.ivWidgetIcon.setImageResource(R.drawable.ic_countdown)
                }
            }

            binding.cardWidget.setOnClickListener {
                onItemClick(item)
            }
            binding.cardWidget.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }

        private fun getTypeString(type: WidgetType): String {
            return when (type) {
                WidgetType.IMAGE -> "图片"
                WidgetType.WOODEN_FISH -> "木鱼"
                WidgetType.COUNTDOWN -> "倒计时"
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<WidgetItem>() {
        override fun areItemsTheSame(oldItem: WidgetItem, newItem: WidgetItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: WidgetItem, newItem: WidgetItem): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetViewHolder {
        val binding = ItemWidgetBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return WidgetViewHolder(binding)
    }

    override fun onBindViewHolder(holder: WidgetViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

}
