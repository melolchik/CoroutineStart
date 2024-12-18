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
		
#14.2 Async and Deferred
Задача вывести Toast в таком-то городе такая-то температура
Можно сделать такая-то
lifecycleScope.launch {
                jobCity.join()
                jobTemp.join()
                val city = binding.tvLocation.text
                val temp = binding.tvTemperature.text
                Toast.makeText(this@MainActivity,
                    "City : $city, Temp : $temp",
                    Toast.LENGTH_SHORT).show()
                binding.progress.isVisible = false
                binding.buttonLoad.isEnabled = true
            }
Но было бы лучше, если бы могли взять значение из корутины
Если хотим получать данные, нужно использовать другой корутин билдет async. Этот метод возвращает объект Deffered
Параметризированный интерфейс
Deffered наследуется от Job

binding.buttonLoad.setOnClickListener {
            binding.progress.isVisible = true
            binding.buttonLoad.isEnabled = false
            val deferredCity : Deferred<String> =  lifecycleScope.async {
                val city = loadCity()
                binding.tvLocation.text = city
                city - последнее значение вовзвращается в Deferred
            }
            val deferredTemp : Deferred<Int> =  lifecycleScope.async {
                val temp = loadTemperature()
                binding.tvTemperature.text = temp.toString()
                temp
            }

            lifecycleScope.launch {                
                val city = deferredCity.await() - делает тоже самое, что и join + возвращает значение
                val temp = deferredTemp.await()
                Toast.makeText(this@MainActivity,
                    "City : $city, Temp : $temp",
                    Toast.LENGTH_SHORT).show()
                binding.progress.isVisible = false
                binding.buttonLoad.isEnabled = true
            }
        }
		
#14.3 Создание приложения Factorial
#14.4 Stateful ViewModel

UDF архитектура - весь стейт ViewModel расположен в одном объекте

class State( val isError : Boolean = false,
    val isInProgress : Boolean = false,
    val factorial : String = "" )
	
class FactorialViewModel : ViewModel() {

    private val _state = MutableLiveData<State>()

    val state : LiveData<State>
        get() = _state



    fun calculate(value : String?){
        _state.value = State(isInProgress = true)
        if(value.isNullOrBlank()){
            _state.value = State(isInProgress = false, isError = true)
            return
        }

        viewModelScope.launch {
            val number = value.toLong()
            //calculate
            delay(1000)
            _state.value = State(factorial = number.toString())
        }


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