package ru.melolchik.coroutinestart.lesson15_16

import android.util.Log
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.retry

suspend fun main() {
    val flow = loadDataFlow();

    flow
        .map { State.Content(it) as State }
        .onStart { emit(State.Loading) }
        .retry(2) {
            true
        }
        .catch {
            //println("Catch ex = $it")
            emit(State.Error)
        }
        .collect {
            //println("Collected  $it")
            when (it){
                is State.Content -> {
                    println("Collected ${it.value}")
                }
                State.Error -> {
                    println("Error")
                }
                State.Loading -> {
                    println("Loading...")
                }
            }
    }

}

fun loadDataFlow(): Flow<Int> = flow {
    repeat(5) {
        delay(500)
        emit(it)
    }
    throw RuntimeException("Exception from flow")

}

sealed class State(){
    data class Content(val value : Int) : State()
    object Loading : State()
    object Error : State()
}