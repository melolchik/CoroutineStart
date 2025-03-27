package ru.melolchik.coroutinestart.crypto

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import ru.melolchik.coroutinestart.cript.Currency
import kotlin.random.Random

object CryptoRepository {

    private val currencyNames = listOf("BTC", "ETH", "USDT", "BNB", "USDC")
    private val currencyList = mutableListOf<Currency>()

    fun getCurrencyList() = flow<List<Currency>> {
        while (true) {
            delay(3000)
            generateCurrencyList()
            emit(currencyList.toList())
            delay(3000)
        }
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
