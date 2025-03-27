package ru.melolchik.coroutinestart.crypto

import ru.melolchik.coroutinestart.cript.Currency

sealed class State {
    object Initial : State()
    object Loading : State()

    data class Content(val currencyList : List<Currency>) : State()
}