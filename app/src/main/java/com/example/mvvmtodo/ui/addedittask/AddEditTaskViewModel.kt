package com.example.mvvmtodo.ui.addedittask

import androidx.hilt.Assisted
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmtodo.ADD_TASK_RESULT_OK
import com.example.mvvmtodo.EDIT_TASK_RESULT_OK
import com.example.mvvmtodo.data.Task
import com.example.mvvmtodo.data.TaskDao
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class AddEditTaskViewModel @ViewModelInject constructor(
    private val taskDao: TaskDao,
    @Assisted private val state: SavedStateHandle
): ViewModel() {
    val task = state.get<Task>("task")

    private val addEditTaskEventChannel = Channel<AddEditTaskEvent>()
    val addEditTaskEvent = addEditTaskEventChannel.receiveAsFlow()

    var taskName = state.get<String>("taskName") ?: task?.name ?: ""
        set(value) {
            field = value
            state.set("taskName",value)
        }

    var taskImportance = state.get<Boolean>("taskImportance") ?: task?.important ?: false
        set(value) {
            field = value
            state.set("taskImportance",value)
        }

    fun onSaveClick() {
        if (taskName.isBlank()){
            showInvalidInputMessage("Fields cannot be empty")
            return
        }
        if (task != null){
            val updateTask = task.copy(name = taskName, important = taskImportance)
            updateTask(updateTask)
        }
        else{
            val newTask = Task(name = taskName, important = taskImportance)
            createTask(newTask)
        }
    }

    private fun createTask(newTask: Task) = viewModelScope.launch {
        taskDao.insert(newTask)
        addEditTaskEventChannel.send(AddEditTaskEvent.NavigateBackWithResult(ADD_TASK_RESULT_OK))
    }

    private fun updateTask(updateTask: Task) = viewModelScope.launch {
            taskDao.update(updateTask)
            addEditTaskEventChannel.send(AddEditTaskEvent.NavigateBackWithResult(EDIT_TASK_RESULT_OK))
        }

    private fun showInvalidInputMessage(text: String) = viewModelScope.launch {
        addEditTaskEventChannel.send(AddEditTaskEvent.ShowInvalidInputMessage(text))
    }


    sealed class AddEditTaskEvent(){
        data class ShowInvalidInputMessage(val msg:String):AddEditTaskEvent()
        data class NavigateBackWithResult(val result:Int):AddEditTaskEvent()
    }
}

