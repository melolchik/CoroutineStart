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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
             getFlowByFlowBuilder().filter { it.isPrime() }
                 .filter { it < 20 }
                 .collect { Log.d(LOG_TAG,"Output $it") }
         }
    }

    private fun getFlowByFlowBuilder() : Flow<Int>{
        val numbers = listOf(1,5,9,12,38,46,54,99,111)
        return flow {
            for (i in numbers) {
                this.emit(i)
                Log.d(LOG_TAG, "Emitted $i")
            }
        }
    }


    companion object{
        private const val LOG_TAG = "MainViewModel"
    }
}