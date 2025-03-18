package ru.melolchik.coroutinestart

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.math.BigInteger
import kotlin.concurrent.thread
import kotlin.coroutines.suspendCoroutine

class FactorialViewModel : ViewModel() {

    private val _state = MutableLiveData<State>()

 //   private val coroutineScope = CoroutineScope(Dispatchers.Main + CoroutineName("kjhfks"))

    val state: LiveData<State>
        get() = _state


    fun calculate(value: String?) {
        _state.value = Progress
        if (value.isNullOrBlank()) {
            _state.value = Error
            return
        }

        viewModelScope.launch {
            val number = value.toLong()
            //calculate
            val result = withContext(Dispatchers.Default) {
                delay(1000)
                factorial(number)
            }

            _state.value = Factorial(factorial = result)

        }

    }

//    private suspend fun factorial(number: Long): String {
//
//        return suspendCoroutine {
//            thread {
//                var result = BigInteger.ONE
//                for (i in 1..number) {
//                    result = result.multiply(BigInteger.valueOf(i))
//                }
//                it.resumeWith(Result.success(result.toString()))
//
//            }
//        }
//
//    }
//
//    private suspend fun factorial2(number: Long): String {
//
//        return withContext(Dispatchers.Default) {
//            var result = BigInteger.ONE
//            for (i in 1..number) {
//                result = result.multiply(BigInteger.valueOf(i))
//            }
//            result.toString()
//        }
//    }

    private fun factorial(number: Long): String {
            var result = BigInteger.ONE
            for (i in 1..number) {
                result = result.multiply(BigInteger.valueOf(i))
            }
            return result.toString()
    }
}