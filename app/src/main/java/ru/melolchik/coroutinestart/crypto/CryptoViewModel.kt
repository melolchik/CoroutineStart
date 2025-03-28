package ru.melolchik.coroutinestart.crypto

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    val state: LiveData<State> = repository.getCurrencyList()
        .filter { it.isNotEmpty() }
        .map { State.Content(currencyList = it) as State }
        .onStart {
            emit(State.Loading)
        }
        .asLiveData()

}