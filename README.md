# CoroutineStart
#8.1 Асинхронное программирование с коллбэками
#8.2 Handler и Looper
#8.3 Проблемы при стандартном подходе к асинхронному программированию
#8.4 Введение в Kotlin Coroutines. Suspend функции
#8.5 Корутины "под капотом"

#14.1 Job and Coroutine Builders

Поменяем loadTemperature , чтобы не зависело от города
И теперь loadCity и loadTemperature можно запускать одноврменно
Для этого их нужно выполнять в разных корутинах

 binding.buttonLoad.setOnClickListener {
            binding.progress.isVisible = true
            binding.buttonLoad.isEnabled = false
            lifecycleScope.launch {
                val city = loadCity()
                binding.tvLocation.text = city
            }
            lifecycleScope.launch {
                val temp = loadTemperature()
                binding.tvTemperature.text = temp.toString()
            }
        }
		
		lifecycleScope.launch - возвращает Job
		
binding.buttonLoad.setOnClickListener {
            binding.progress.isVisible = true
            binding.buttonLoad.isEnabled = false
            val jobCity =  lifecycleScope.launch {
                val city = loadCity()
                binding.tvLocation.text = city
            }
            val jobTemp =  lifecycleScope.launch {
                val temp = loadTemperature()
                binding.tvTemperature.text = temp.toString()
            }

            lifecycleScope.launch {
                jobCity.join() - ожидание
                jobTemp.join() - ожидание
                binding.progress.isVisible = false
                binding.buttonLoad.isEnabled = true
            }
        }