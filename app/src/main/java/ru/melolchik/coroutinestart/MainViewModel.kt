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
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class MainViewModel : ViewModel() {
    private val exceptionHandler = CoroutineExceptionHandler{
        _, throwable -> Log.d(LOG_TAG, "OnExceptionHandler $throwable")

    }

    fun method(){
       val job =  viewModelScope.launch (Dispatchers.Default + exceptionHandler){
        Log.d(LOG_TAG, "Started ")
            val before = System.currentTimeMillis()
            var count = 0
            for (i in 0 until 100_000_000){
                for (j in 0 until 100){
//                    if(isActive) {
//                        count++
//                    }else{
//                        throw CancellationException()
//                    }
                    ensureActive()
                    count++
                }
            }
            Log.d(LOG_TAG, "Finished ${System.currentTimeMillis() - before}")
        }
        job.invokeOnCompletion {
            Log.d(LOG_TAG, "Coroutine was canceled $it")
        }
        viewModelScope.launch {
            delay(3000)
            job.cancel()

        }
    }


    companion object{
        private const val LOG_TAG = "MainViewModel"
    }
}