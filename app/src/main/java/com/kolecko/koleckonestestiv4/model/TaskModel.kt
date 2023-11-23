package com.kolecko.koleckonestestiv4

import android.content.Context
import com.kolecko.koleckonestestiv4.model.Task
import com.kolecko.koleckonestestiv4.model.TaskDatabase
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Rozhraní reprezentující model úkolů
interface TaskModel {
    suspend fun getAllTasks(): List<Task>
    suspend fun removeTask(task: Task)
    suspend fun insertTask(task: Task)
    suspend fun addNewTask(title: String, description: String)
}

// Implementace rozhraní TaskModel
@OptIn(DelicateCoroutinesApi::class)
class TaskModelImpl(private val context: Context) : TaskModel {
    // Získání přístupu k DAO pro úkoly
    private val taskDao = TaskDatabase.getDatabase(context).taskDao()

    // Metoda pro získání všech úkolů (implementace z rozhraní TaskModel)
    override suspend fun getAllTasks(): List<Task> = withContext(Dispatchers.IO) {
        return@withContext taskDao.getAllTasks()
    }

    // Metoda pro odstranění úkolu (implementace z rozhraní TaskModel)
    override suspend fun removeTask(task: Task) {
        taskDao.deleteTask(task)
    }

    // Metoda pro vložení úkolu (implementace z rozhraní TaskModel)
    override suspend fun insertTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.insertTask(task)
    }

    // Nová metoda pro vložení nové úlohy s parametry názvu a popisu
    override suspend fun addNewTask(title: String, description: String) {
        // Vytvoření nové instance úkolu pomocí primárního konstruktoru entity Task
        val newTask = Task(title = title, description = description)
        // Volání metody pro vložení úkolu do databáze
        insertTask(newTask)
    }
}
