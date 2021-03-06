package com.example.mvvmtodo.di

import android.app.Application
import androidx.room.Room
import com.example.mvvmtodo.data.TaskDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton


@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providesDatabase(
        app: Application,
        callback: TaskDatabase.Callback
    ) = Room.databaseBuilder(app, TaskDatabase::class.java, "task_Database")
        .fallbackToDestructiveMigration()
        .addCallback(callback)
        .build()

// TaskDao is singleton automatically that how room works under the hood that why we don't need to annotate it with @Singleton
    @Provides
    fun providesTaskDao(taskDatabase: TaskDatabase) = taskDatabase.getTaskDao()

    @ApplicationScope
    @Provides
    @Singleton
    fun provideApplicationScope() = CoroutineScope(SupervisorJob()) // this coroutine scope lives as long as the application does
    //                                                                 because we defined it as singleton in the ApplicationComponent class
}

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope
