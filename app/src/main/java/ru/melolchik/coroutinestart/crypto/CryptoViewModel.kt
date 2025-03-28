package ru.melolchik.coroutinestart.crypto

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onCompletion

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    private val _state = MutableLiveData<State>(State.Initial)
    val state: LiveData<State> = _state

    private var job : Job? = null
    private var isResumed = false

//    init {
//        loadData()
//    }

    public fun loadData() {
        isResumed = true
        if(job != null){
            return
        }
        job = repository.getCurrencyList()
            .onStart {
               _state.value = State.Loading
                Log.d("CryptoViewModel","onStart " )
            }
            .onEach {
                Log.d("CryptoViewModel","onEach" )
                _state.value = State.Content(currencyList = it) }
            .onCompletion {
                Log.d("CryptoViewModel","onCompletion $it" )
            }
            .launchIn(viewModelScope)
    }

    fun stopLoading(){
        viewModelScope.launch {
            delay(5000)
            if(!isResumed){
                job?.cancel()
                job = null
            }else{
                isResumed = false
            }
        }
    }
}