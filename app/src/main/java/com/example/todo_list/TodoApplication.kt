package com.example.todo_list

import android.app.Application
import com.example.todo_list.data.local.AppDatabase
import com.example.todo_list.data.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Custom Application class for initializing Room Database, Repositories, and Singletons.
 */
class TodoApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val repository: TaskRepository by lazy { TaskRepository.getInstance(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Seed initial data if first launch
        CoroutineScope(Dispatchers.IO).launch {
            repository.ensureInitialDataSeeded()
        }
    }

    companion object {
        lateinit var instance: TodoApplication
            private set
    }
}
