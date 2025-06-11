package ru.melolchik.coroutinestart.crypto

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import ru.melolchik.coroutinestart.cript.Currency
import kotlin.random.Random

object CryptoRepository {

    private val currencyNames = listOf("BTC", "ETH", "USDT", "BNB", "USDC")
    private val currencyList = mutableListOf<Currency>()

    private val refreshSharedFlow = MutableSharedFlow<Unit>()

    private val coroutineScope = CoroutineScope(Dispatchers.Default)
    val currencyListFlow = flow<List<Currency>> {
            delay(3000)
            generateCurrencyList()
            emit(currencyList.toList())

            refreshSharedFlow.collect{
                delay(3000)
                generateCurrencyList()
                emit(currencyList.toList())
            }
    }.stateIn(
        coroutineScope,
        SharingStarted.Lazily,
        initialValue = currencyList.toList())

    suspend fun refreshList(){
        refreshSharedFlow.emit(Unit)
    }

    private fun generateCurrencyList() {
        val prices = buildList {
            repeat(currencyNames.size) {
                add(Random.nextInt(1000, 2000))
            }
        }
        val newData = buildList {
            for ((index, currencyName) in currencyNames.withIndex()) {
                val price = prices[index]
                val currency = Currency(name = currencyName, price = price)
                add(currency)
            }
        }
        currencyList.clear()
        currencyList.addAll(newData)
    }
}
