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
import com.example.mvvmtodo.uitl.exhaustive
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

        /*
         we use viewLifecycleOwner because when we go to a different fragment this fragment is put to background
         the fragment instance stays alive but the view hierarchy is destroyed and when we don't have a view
         means we don't have a recycler view to show the data and if we don't have a recycler view we don't need
         updates, and if we get updates while the view hierarchy is no there we would get crashes because the references
         would not be valid
         */
        viewModel.tasks.observe(viewLifecycleOwner) {
            taskAdapter.submitList(it)
        }

        // we use launchWhenStarted cause to make the scope of the coroutine smaller it will be cancelled when onStop is called
        // instead of when onDestroy is called and restarted when onStart is called
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            viewModel.taskEvents.collect { event->
                when(event){
                    is TasksViewModel.TaskEvents.ShowUndoDeleteTaskMessage ->{
                        Snackbar.make(requireView(),"Task Deleted", Snackbar.LENGTH_LONG)
                            .setAction("Undo"){
                                viewModel.onUndoDeleteClicked(event.task) // we can write event.task directly cause we are in the
                                                                          // when check of TasksViewModel.TaskEvents.ShowUndoDeleteTaskMessage
                                                                         // which automatically smart cast to ShowUndoDeleteTaskMessage event
                            }.show()
                    }
                    is TasksViewModel.TaskEvents.NavigateToAddTaskScreen -> {
                        val action = TasksFragmentDirections.actionTasksFragmentToAddEditTaskFragment(null,"New Task")
                        findNavController().navigate(action)
                    }
                    is TasksViewModel.TaskEvents.NavigateToEditTaskScreen -> {
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
                }.exhaustive

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

        // viewLifecycleOwner lives as long as the view lives. If the view is destroyed the coroutine will be cancelled and
        // we won't read any more values from the flow.
        viewLifecycleOwner.lifecycleScope.launch {
            menu.findItem(R.id.action_hide_completed_tasks).isChecked =
                viewModel.preferencesFlow.first().hideCompleted // .first() is used as we only want to read from the flow only once
                                                                // after we started our app. This will read the value from the flow
                                                                // once and then cancel it. But cancelling the flow doesn't mean that
                                                                // it stops working in other places, it will just be cancelled inside
                                                                // this coroutine
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
    /* when a fragment's view is destroyed the searchView by default sends an empty
     string as the input which makes the searchQuery value in the vm empty so
     we make searchView a property of this class so we can deactivate the listener of  searchView */
    }

}