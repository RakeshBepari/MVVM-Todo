package com.example.mvvmtodo.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM task_table WHERE (completed != :hideCompleted or completed = 0) and name LIKE '%' || :searchQuery || '%' ORDER BY important desc, name")
    fun getTasksSortedByName(searchQuery: String, hideCompleted:Boolean): Flow<List<Task>>

    @Query("SELECT * FROM task_table WHERE (completed != :hideCompleted or completed = 0) and name LIKE '%' || :searchQuery || '%' ORDER BY important desc, created")
    fun getTasksSortedByDate(searchQuery: String, hideCompleted:Boolean): Flow<List<Task>>

    @Query("SELECT * FROM task_table WHERE name LIKE '%' || :searchQuery || '%' ORDER BY important desc")
    fun getTasks(searchQuery: String, sortOrder: SortOrder, hideCompleted:Boolean): Flow<List<Task>> =
        when(sortOrder){
            SortOrder.BY_NAME -> getTasksSortedByName(searchQuery, hideCompleted)
            SortOrder.BY_DATE -> getTasksSortedByDate(searchQuery, hideCompleted)
        }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task)

    @Update
    suspend fun update(task: Task)

    @Delete
    suspend fun delete(task: Task)

    @Query("DELETE FROM task_table where completed = 1")
    suspend fun deleteAllCompletedTask()
}