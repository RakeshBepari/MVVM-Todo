package com.example.mvvmtodo.ui.deleteallcompletedtask

import android.util.Log
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mvvmtodo.data.TaskDao
import com.example.mvvmtodo.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "DeleteAllCompletedViewM"

class DeleteAllCompletedViewModel @ViewModelInject constructor(
    val taskDao : TaskDao,
    @ApplicationScope private val applicationScope: CoroutineScope
) :ViewModel()  {

    fun onConfirmClick() = applicationScope.launch {
        Log.d(TAG,"Delete All completed Task Called")
        taskDao.deleteAllCompletedTask()
    }
}