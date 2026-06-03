package com.karishma.swiggyanimation.celestial

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CelestialViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = CelestialDatabase.get(app).todoDao()

    val selectedDate = MutableStateFlow(todayString())

    @OptIn(ExperimentalCoroutinesApi::class)
    val todos = selectedDate
        .flatMapLatest { dao.todosForDate(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addTodo(text: String, hour: Int) {
        if (text.isBlank()) return
        viewModelScope.launch {
            dao.insert(TodoEntity(text = text.trim(), hour = hour, date = selectedDate.value))
        }
    }

    fun deleteTodo(todo: TodoEntity) {
        viewModelScope.launch { dao.delete(todo) }
    }

    fun setDate(day: Int, month: Int, year: Int) {
        selectedDate.value = "%04d-%02d-%02d".format(year, month, day)
    }
}

private fun todayString(): String {
    val cal = java.util.Calendar.getInstance()
    return "%04d-%02d-%02d".format(
        cal.get(java.util.Calendar.YEAR),
        cal.get(java.util.Calendar.MONTH) + 1,
        cal.get(java.util.Calendar.DAY_OF_MONTH)
    )
}
