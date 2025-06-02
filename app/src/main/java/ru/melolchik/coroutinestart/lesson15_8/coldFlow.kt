package ru.melolchik.coroutinestart.lesson15_8

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch


val coroutineScope = CoroutineScope(Dispatchers.IO)
suspend fun main(){


    val flow = getFlow()

    val job1 = coroutineScope.launch {
        flow.collect {
            println("collect 1st: $it")
        }
    }
    val job2 = coroutineScope.launch {
        flow.collect {
            println("collect 2nd: $it")
        }
    }

    job1.join()
    job2.join()
}

fun getFlow() : Flow<Int> = flow {
    repeat(5){
        println("Emitted: $it")
        emit(it)
        delay(1000)
    }
}