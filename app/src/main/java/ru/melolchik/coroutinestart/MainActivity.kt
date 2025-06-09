package ru.melolchik.coroutinestart

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import ru.melolchik.coroutinestart.crypto.CryptoActivity
import ru.melolchik.coroutinestart.databinding.ActivityMainBinding
import ru.melolchik.coroutinestart.team_score.TeamScoreActivity

class MainActivity : AppCompatActivity() {

    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private val viewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
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
        //viewModel.method()
        binding.buttonUsersActivity.setOnClickListener {
            startActivity(UsersActivity.newIntent(this))
        }
        binding.buttonCryptoActivity.setOnClickListener {
            startActivity(CryptoActivity.newIntent(this))
        }
        binding.teamScoreActivity.setOnClickListener {
            startActivity(TeamScoreActivity.newIntent(this))
        }
    }


}