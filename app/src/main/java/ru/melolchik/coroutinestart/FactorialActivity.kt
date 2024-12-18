package ru.melolchik.coroutinestart

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import ru.melolchik.coroutinestart.databinding.ActivityFactorialBinding

class FactorialActivity  : AppCompatActivity() {

    private val binding by lazy {
        ActivityFactorialBinding.inflate(layoutInflater)
    }

    private val viewModel by lazy{
        ViewModelProvider(this)[FactorialViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        observeViewModel()
        binding.buttonCalculate.setOnClickListener{
            viewModel.calculate(binding.editTextNumber.text.toString())
        }
    }

    private fun observeViewModel(){
        viewModel.state.observe(this){
            if(it.isInProgress) {
                binding.progressBarLoading.visibility = View.VISIBLE
                binding.buttonCalculate.isEnabled = false
            }else{
                binding.progressBarLoading.visibility = View.GONE
                binding.buttonCalculate.isEnabled = true
            }

            if(it.isError){
                Toast.makeText(this,
                    "You did not entered value",
                    Toast.LENGTH_SHORT)
                    .show()
            }

            binding.textViewFactorial.text = it.factorial
        }

    }
}