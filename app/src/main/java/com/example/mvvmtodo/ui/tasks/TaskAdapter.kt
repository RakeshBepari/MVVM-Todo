package com.example.mvvmtodo.ui.tasks

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mvvmtodo.data.Task
import com.example.mvvmtodo.databinding.ItemTaskBinding

class TaskAdapter(private val listener: OnItemClickListener): ListAdapter<Task,TaskAdapter.TasksViewHolder>(DiffCallback()) {

    inner class TasksViewHolder(private val binding:ItemTaskBinding):RecyclerView.ViewHolder(binding.root){

        // we set the onClickListeners inside the init block so that it gets instantiated when the viewHolder is created
        init {
            binding.apply {
                root.setOnClickListener {       // Instead of setting up onClickListener in onBindViewHolder we set it here because
                                                // onBindViewHolder is called every time a new item scrolls on the screen so it leads
                                                // to ton of unnecessary work
                                                // the init block is only called only when the viewHolder was first created
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION){ // we check this condition because when we delete an item it gets
                                                               // animated of the list and theoretically its possible to still click
                                                               // the item while its animating so we check if the item is still there
                        val task = getItem(position)  // In order to access the getItem method we make TaskViewHolder an inner class
                        listener.onItemClick(task)
                    }
                }
                checkBoxCompleted.setOnClickListener {
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION){
                        val task = getItem(position)
                        listener.onCheckBoxClick(task, checkBoxCompleted.isChecked)
                    }
                }
            }
        }

        fun bind(task:Task){
            binding.apply {
                checkBoxCompleted.isChecked = task.completed
                textViewName.text = task.name
                textViewName.paint.isStrikeThruText = task.completed
                labelPriority.isVisible = task.important

            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TasksViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context),parent,false)
        return TasksViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TasksViewHolder, position: Int) {
        val currentItem = getItem(position)
        holder.bind(task = currentItem)
    }

    class DiffCallback : DiffUtil.ItemCallback<Task>() {
        override fun areItemsTheSame(oldItem: Task, newItem: Task) =
            oldItem.id  == newItem.id


        override fun areContentsTheSame(oldItem: Task, newItem: Task) =
           oldItem == newItem


    }

    interface OnItemClickListener{
        fun onItemClick(task:Task)
        fun onCheckBoxClick(task:Task,isChecked:Boolean)
    }
}