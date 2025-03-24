package ru.melolchik.coroutinestart

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

suspend fun Int.isPrime() : Boolean{
    if(this <= 1) return false
    for(i in 2 .. this/2){
        delay(50)
        if(this % i == 0) return false
    }
    return true
}
class MainViewModel : ViewModel() {


     fun method(){
         viewModelScope.launch {
             val numbers = listOf(3, 4, 5, 98, 19, 48, 25, 32).asFlow()
             numbers.filter { it.isPrime() }
                 .filter { it < 20 }
                 .collect { Log.d(LOG_TAG, it.toString()) }
         }
    }


    companion object{
        private const val LOG_TAG = "MainViewModel"
    }
}