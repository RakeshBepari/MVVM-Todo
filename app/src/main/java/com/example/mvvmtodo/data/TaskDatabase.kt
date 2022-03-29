package com.example.mvvmtodo.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.mvvmtodo.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(entities = [Task::class],version = 1)
abstract class TaskDatabase : RoomDatabase() {

    abstract fun getTaskDao(): TaskDao

    //Dagger will not instantiated the taskDatabase when the callback is created
    class Callback @Inject constructor(
        private val taskDatabase: Provider<TaskDatabase> ,// With Provider we can get dependencies lazily and the circular dependency is borken
        @ApplicationScope private val applicationScope: CoroutineScope
        ) : RoomDatabase.Callback() {

        // onCreate is not executed when we construct the the TaskDatabase in AppModule (TaskDatabase needs callback as its dependency to be constructed)
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            val dao = taskDatabase.get().getTaskDao() // TaskDatabase is instantiated when we call get on it

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