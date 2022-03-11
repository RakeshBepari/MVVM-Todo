package com.example.mvvmtodo.ui.tasks

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mvvmtodo.R
import com.example.mvvmtodo.data.SortOrder
import com.example.mvvmtodo.data.Task
import com.example.mvvmtodo.databinding.FragmentTasksBinding
import com.example.mvvmtodo.uitl.OnQueryTextChanged
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@AndroidEntryPoint
class TasksFragment : Fragment(R.layout.fragment_tasks),TaskAdapter.OnItemClickListener {

    private val viewModel: TasksViewModel by viewModels()

    private lateinit var searchView : SearchView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val binding = FragmentTasksBinding.bind(view)

        val taskAdapter = TaskAdapter(this)

        binding.apply {
            recyclerViewTasks.apply {
                adapter = taskAdapter
                layoutManager = LinearLayoutManager(requireContext())
                setHasFixedSize(true)
            }
            fabAddTask.setOnClickListener(){
                viewModel.onAddTaskClicked()
            }

            viewModel.tasks.observe(viewLifecycleOwner) {
                taskAdapter.submitList(it)
            }

            ItemTouchHelper(object :
                ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
                override fun onMove(
                    recyclerView: RecyclerView,
                    viewHolder: RecyclerView.ViewHolder,
                    target: RecyclerView.ViewHolder
                ): Boolean {
                    return false
                }

                override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    val task = taskAdapter.currentList[viewHolder.adapterPosition]
                    viewModel.onTaskSwiped(task)
                }

            }).attachToRecyclerView(recyclerViewTasks)
        }

        setFragmentResultListener("add_edit_request"){ _,bundle->
            val result = bundle.getInt("add_edit_request")
            viewModel.onAddEditResult(result)

        }

        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.taskEvents.collect { event->
                when(event){
                    is TasksViewModel.TaskEvents.ShowUndoDeleteTaskMessage ->{
                        Snackbar.make(requireView(),"Task Deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo"){
                                viewModel.onUndoDeleteClicked(event.task)
                            }.show()
                    }
                    is TasksViewModel.TaskEvents.NavigateToAddTask -> {
                        val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(null,"New Task")
                        findNavController().navigate(action)
                    }
                    is TasksViewModel.TaskEvents.NavigateToEditTask -> {
                        val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(event.task,"Edit Task")
                        findNavController().navigate(action)
                    }
                    is TasksViewModel.TaskEvents.ShowTaskSavedConfirmationMessage -> {
                        Snackbar.make(requireView(),event.msg, Snackbar.LENGTH_LONG).show()
                    }
                    TasksViewModel.TaskEvents.NavigateToDeleteAllCompletedScreen -> {
                        val action = TasksFragmentDirections.actionGlobalDeleteAllCompletedDialogFragment()
                        findNavController().navigate(action)
                    }
                }

            }
        }

        setHasOptionsMenu(true)
    }



    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_fragment_tasks, menu)

        val searchItem = menu.findItem(R.id.action_search)
         searchView = searchItem.actionView as SearchView

        val pendingQuery = viewModel.searchQuery.value

        if(pendingQuery != null && pendingQuery.isNotEmpty()){
            searchItem.expandActionView()
            searchView.setQuery(pendingQuery,false)
        }

        searchView.setQuery(viewModel.searchQuery.value,false)

        searchView.OnQueryTextChanged {
            viewModel.searchQuery.value = it
        }

        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                viewModel.preferencesFlow.first().hideCompleted
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_sort_by_name -> {
                viewModel.onSortOrderSelected(SortOrder.BY_NAME)
                true
            }
            R.id.action_sort_by_date_created -> {
                viewModel.onSortOrderSelected(SortOrder.BY_DATE)
                true
            }
            R.id.action_hide_completed_tasks -> {
                viewModel.onHideCompletedClicked(!item.isChecked)
                item.isChecked = !item.isChecked
                true
            }
            R.id.action_delete_all_completed_tasks -> {
                viewModel.deleteAllCompletedClick()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onItemClick(task: Task) {
        viewModel.onTaskSelected(task)
    }

    override fun onCheckBoxClick(task: Task, isChecked: Boolean) {
        viewModel.onTaskCheckedChanged(task,isChecked)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchView.setOnQueryTextListener(null)
    }

}