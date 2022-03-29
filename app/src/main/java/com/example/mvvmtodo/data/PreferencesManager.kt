package com.example.mvvmtodo.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.createDataStore
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.preferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton


enum class SortOrder {
    BY_NAME,
    BY_DATE
}

data class FilterPreferences(val sortOrder: SortOrder, val hideCompleted: Boolean)

private const val TAG = "PreferencesManager"

@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext context: Context) {

    private val dataStore = context.createDataStore("user_preferences")

    val preferencesFlow = dataStore.data
        .catch { exception ->
            if (exception is IOException)
                Log.e(TAG, "Error reading preferences: ", exception)
            else
                throw exception
        }
        .map { preferences ->
            val sortOrder = SortOrder.valueOf(  // value of converts strings into enums. In this case enums of type sortOrder
                preferences[PreferencesKeys.sortOrderPreference] ?: SortOrder.BY_DATE.name
            )

            val hideCompleted = preferences[PreferencesKeys.hideCompletedPreference] ?: false

            FilterPreferences(sortOrder, hideCompleted)

        }

    suspend fun updateSortOrder(sortOrder: SortOrder) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.sortOrderPreference] = sortOrder.name
        }
    }

    suspend fun updateHideCompleted(hideCompleted: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.hideCompletedPreference] = hideCompleted
        }
    }

    private object PreferencesKeys {
        val sortOrderPreference = preferencesKey<String>("sort_order")
        val hideCompletedPreference = preferencesKey<Boolean>("hide_completed")
    }
}