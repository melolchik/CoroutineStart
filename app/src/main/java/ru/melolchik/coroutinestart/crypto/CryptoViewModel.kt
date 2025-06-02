package ru.melolchik.coroutinestart.crypto

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    val state: Flow<State> = repository.getCurrencyList()
        .filter { it.isNotEmpty() }
        .map { State.Content(it) as State }
        .onStart {
            emit(State.Loading)
        }

    fun refreshList() {
        viewModelScope.launch {
            repository.refreshList()
        }
    }

}