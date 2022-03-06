package com.example.mvvmtodo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mvvmtodo.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Task::class],version = 1, exportSchema = false)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun getTaskDao(): TaskDao

    class Callback @Inject constructor(
        private val taskDatabase: Provider<TaskDatabase>,
        @ApplicationScope private val applicationScope: CoroutineScope
        ) :
        RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            val dao = taskDatabase.get().getTaskDao()

            applicationScope.launch {
                dao.insert(Task("Add Todos by clicking on the add button", important = true))
                dao.insert(Task("Edit Todos by clicking on the todos in the list"))
                dao.insert(Task("If a todo is done click on the checkbox in front of todo"))
                dao.insert(Task("You can search or filter them by the menu options above"))
                dao.insert(Task("You can delete a todo by swiping the todo from left to right", completed = true))
                dao.insert(Task("Mark a task important in the edit screen", completed = true))
                dao.insert(Task("Important task will appear on top"))

            }
        }
    }
}