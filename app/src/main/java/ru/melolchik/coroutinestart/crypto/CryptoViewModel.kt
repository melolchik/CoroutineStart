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
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    private val loadingFlow = MutableSharedFlow<State>()

    val state: Flow<State> = repository.currencyListFlow
        .filter { it.isNotEmpty() }
        .map { State.Content(it) as State }
        .onStart {

            emit(State.Loading)
        }.mergeWith(loadingFlow)

    val state2: Flow<State> = repository.currencyListFlow
        .filter { it.isNotEmpty() }
        .map { State.Content(it) as State }
        .onStart {

            emit(State.Loading)
        }.mergeWith(loadingFlow)

    fun <T> Flow<T>.mergeWith(otherFlow : Flow<T>) : Flow<T>{
        return merge(this,otherFlow)
    }

    fun refreshList() {

        viewModelScope.launch {
            loadingFlow.emit(State.Loading)
            repository.refreshList()
        }
    }

}