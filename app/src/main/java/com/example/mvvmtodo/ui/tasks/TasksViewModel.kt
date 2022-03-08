package com.example.mvvmtodo.ui.tasks

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.*
import com.example.mvvmtodo.data.PreferencesManager
import com.example.mvvmtodo.data.SortOrder
import com.example.mvvmtodo.data.Task
import com.example.mvvmtodo.data.TaskDao
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch


class TasksViewModel @ViewModelInject constructor(
    private val taskDao: TaskDao,
    private val preferencesManager: PreferencesManager,
    @Assisted private val state: SavedStateHandle
) : ViewModel() {

    val searchQuery = state.getLiveData("searchQuery","")

    val preferencesFlow = preferencesManager.preferencesFlow

    private  val taskFlow = combine(
        searchQuery.asFlow(),
        preferencesFlow
    ){searchQuery,filterPreferences->
        Pair(searchQuery,filterPreferences)
    }
        .flatMapLatest {(query,filterPreferences) ->
        taskDao.getTasks(query,filterPreferences.sortOrder,filterPreferences.hideCompleted)
    }

    private val taskEventChannel = Channel<TaskEvents> ()
    val taskEvents = taskEventChannel.receiveAsFlow()

    val tasks = taskFlow.asLiveData()

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
        taskEventChannel.send(TaskEvents.NavigateToAddTask)
    }

    fun onTaskSelected(task: Task) = viewModelScope.launch {
        taskEventChannel.send(TaskEvents.NavigateToEditTask(task))
    }

    fun onTaskCheckedChanged(task: Task, isChecked: Boolean) = viewModelScope.launch {
        taskDao.update(task.copy(completed = isChecked))
    }

    sealed class TaskEvents(){
        data class ShowUndoDeleteTaskMessage(val task: Task):TaskEvents()
        data class NavigateToEditTask(val task: Task):TaskEvents()
        object NavigateToAddTask:TaskEvents()
    }

}
