package ru.melolchik.coroutinestart.crypto

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import ru.melolchik.coroutinestart.databinding.ActivityCryptoBinding

class CryptoActivity : AppCompatActivity() {

    private val binding  by lazy {
        ActivityCryptoBinding.inflate(this.layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[CryptoViewModel::class.java]
    }

    private val adapter = CryptoAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v: View, insets: WindowInsetsCompat ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        binding.recyclerViewCurrencyPriceList.adapter = adapter
        observeData()
    }

    private fun observeData() {
        viewModel.state.observe(this){
            when(it){
                is State.Initial -> {
                    binding.progressBarLoading.isVisible = false
                }
                is State.Loading -> {
                    binding.progressBarLoading.isVisible = true
                }
                is State.Content -> {
                    binding.progressBarLoading.isVisible = false
                    adapter.submitList(it.currencyList)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadData()
    }

    override fun onPause() {
        super.onPause()
        viewModel.stopLoading()
    }

    companion object {

        fun newIntent(context: Context) = Intent(context, CryptoActivity::class.java)
    }
}