package com.example.mvvmtodo.ui.tasks

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.example.mvvmtodo.ui.ADD_TASK_RESULT_OK
import com.example.mvvmtodo.ui.EDIT_TASK_RESULT_OK
import com.example.mvvmtodo.data.PreferencesManager
import com.example.mvvmtodo.data.SortOrder
import com.example.mvvmtodo.data.Task
import com.example.mvvmtodo.data.TaskDao
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


@ExperimentalCoroutinesApi
class TasksViewModel @ViewModelInject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    @Assisted private val state: SavedStateHandle
) : ViewModel() {

    val searchQuery = state.getLiveData("searchQuery","")
    /* We cannot store flow inside a SavedStateHandle cause flow is a kotlin feature and SavedStateHandle belongs to framework
     but we can store LiveData and turn live data into a flow and vice versa
     when using LiveData with SavedStateHandle we don't have to handle the set method instead whenever the value of the
     searchQuery changes it will automatically be persisted inside SavedStateHandle */

    val preferencesFlow = preferencesManager.preferencesFlow

    private  val taskFlow = combine( // combine is used to combine multiple flows into a single flow
                                    // whenever the value in any of the flow changes the combine operator gives us all of the values
                                    // inside the lambda
        searchQuery.asFlow(),
        preferencesFlow
    ){searchQuery,filterPreferences->
        Pair(searchQuery,filterPreferences)
    }
        .flatMapLatest {(query,filterPreferences) ->  // switch from one flow to another flow as and when the value of
                                                     // old flow changes and keep observing the old flow switched from query
                                                    // and filterPreferences flow to Flow<List<Task>>
        taskDao.getTasks(query,filterPreferences.sortOrder,filterPreferences.hideCompleted)
    }


    /*
     we use a channel instead of a live data cause live data keep their latest values and when we rotate the device and
     the old fragment gets destroyed and a new one gets created it connects to the old same viewModel but it will also get
     the latest value of live data which means if showSnackbar live data is set true on device rotation the snackbar will be
     shown again and again when we rotate the device which we don't want. What we want is when an event is consumed its over
     Both sides producer of events and consumere of events have to launch this channel in coroutine both sides can suspend when
     they are waiting, this means if the vm puts an event in the channel and fragment is in bg so and we don't want to show
     snackbar while the fragment is in background then this channel can just suspend and vice versa is also true
    */
    private val taskEventChannel = Channel<TaskEvents> () // taskEventChannel is private cause the fragment should not put
                                                          // any event inside it, fragment should only consume it
    val taskEvents = taskEventChannel.receiveAsFlow()


    val tasks = taskFlow.asLiveData() // we can use flow all the way from db to ui or similarly flow all the way but we use
                                      // flow from db to vm so we can switch threads it has various operators we can use
                                     // we have whole stream of values but livedata has only a single latest value
                                     // From vm to ui we use Livedata as it makes handling the lifecycle of the fragment easier
                                     // because it is lifecycle aware which means when the fragment goes into to background
                                     // and becomes inactive livedata will automatically detect this and stop dispatching events
                                     // so we don't get memory leaks and crashes

    fun onSortOrderSelected(sortOrder: SortOrder){
        viewModelScope.launch {
            preferencesManager.updateSortOrder(sortOrder)
        }
    }
    fun onHideCompletedClicked(hideCompleted: Boolean){
        viewModelScope.launch {
            preferencesManager.updateHideCompleted(hideCompleted)
        }
    }

    fun onTaskSwiped(task: Task) = viewModelScope.launch {
        taskDao.delete(task)
        taskEventChannel.send(TaskEvents.ShowUndoDeleteTaskMessage(task))
    }

    fun onUndoDeleteClicked(task: Task) = viewModelScope.launch {
        taskDao.insert(task)
    }

    fun onAddTaskClicked() = viewModelScope.launch {
        taskEventChannel.send(TaskEvents.NavigateToAddTaskScreen)
    }

    fun onTaskSelected(task: Task) = viewModelScope.launch {
        taskEventChannel.send(TaskEvents.NavigateToEditTaskScreen(task))
    }

    fun onTaskCheckedChanged(task: Task, isChecked: Boolean) = viewModelScope.launch {
        taskDao.update(task.copy(completed = isChecked))
    }

    fun onAddEditResult(result: Int) {
        when (result) {
            ADD_TASK_RESULT_OK -> showConfirmationMessage("Task Added")
            EDIT_TASK_RESULT_OK -> showConfirmationMessage("Task updated")
        }
    }

    private fun showConfirmationMessage(msg: String)= viewModelScope.launch {
        taskEventChannel.send(TaskEvents.ShowTaskSavedConfirmationMessage(msg))
    }

    fun deleteAllCompletedClick() =viewModelScope.launch {
        taskEventChannel.send(TaskEvents.NavigateToDeleteAllCompletedScreen)
    }

    sealed class TaskEvents{  // we use a sealed class instead of a data class because we cause we get a warning of exhaustiveness
                              // in a when check so that all the events are covered
        data class ShowUndoDeleteTaskMessage(val task: Task):TaskEvents()
        data class NavigateToEditTaskScreen(val task: Task):TaskEvents()
        object NavigateToAddTaskScreen:TaskEvents()
        data class ShowTaskSavedConfirmationMessage(val msg:String):TaskEvents()
        object NavigateToDeleteAllCompletedScreen : TaskEvents()
    }

}
