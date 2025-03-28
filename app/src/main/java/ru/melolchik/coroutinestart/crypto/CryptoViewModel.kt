package ru.melolchik.coroutinestart.crypto

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    val state: LiveData<State> = repository.getCurrencyList()
        .filter { it.isNotEmpty() }
        .map { State.Content(it) as State }
        .onStart {
            emit(State.Loading)
            Log.d("CryptoViewModel", "onStart ")
        }
        .onEach {
            Log.d("CryptoViewModel", "onEach")
            //_state.value = State.Content(currencyList = it)
        }
        .onCompletion {
            Log.d("CryptoViewModel", "onCompletion $it")
        }
        .asLiveData()


}