package ru.melolchik.coroutinestart.lesson15_14

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

suspend fun main(){

    val scope = CoroutineScope(Dispatchers.Default)

    val flow = MutableStateFlow(0)

    val producer = scope.launch {
        delay(500)
        repeat(10){
            println("Emitted $it")
            flow.emit(it)
            println("After Emit $it")
            delay(200)
        }
    }

    val consumer = scope.launch {
        flow.collectLatest{
            println("Collecting started: $it")
            delay(5000)
            println("Collecting finished: $it")
        }
    }
    producer.join()
    consumer.join()
}