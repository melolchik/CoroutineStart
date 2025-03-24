# CoroutineStart
#8.1 Асинхронное программирование с коллбэками
#8.2 Handler и Looper
#8.3 Проблемы при стандартном подходе к асинхронному программированию
#8.4 Введение в Kotlin Coroutines. Suspend функции

lifecycleScope - используем, чтобы у запросов был жизненный цикл ( Когда активити умирает, то все запросы отменяются)
Запускаем loadData внутри scope , который имеет ЖЦ
 
 binding.buttonLoad.setOnClickListener {
            lifecycleScope.launch {
                loadData()
            }

        }
Метод loadCity делаем прерываемым - метод прерывается и не блокирует основной поток
private suspend fun loadCity(): String {
        delay(5000)
        return "Moscow"
    }
		
тоже самое делаем со второй функцией
private suspend fun loadTemperature(city: String): Int {

        Toast.makeText(
            this,
            getString(R.string.loading_temperature_toast, city),
            Toast.LENGTH_SHORT
        ).show()
        delay(5000)
        return 17
    }
Suspend функция должна вызываться либо из корутины, либо из другой suspend функции
Поэтому loadData также suspend
private suspend fun loadData() {
        Log.d("MainActivity", "Load started: $this")
        binding.progress.isVisible = true
        binding.buttonLoad.isEnabled = false
		// прерываемый ,т.е. при вызове метода loadCity, мы вйдем из метода loadData до тех пор пока он не завершится, 
		// и когда он завершится, мы вернёмся в этот метод к следующей строчке
        val city = loadCity() 
        binding.tvLocation.text = city
        val temp = loadTemperature(city)
        binding.tvTemperature.text = temp.toString()
        binding.progress.isVisible = false
        binding.buttonLoad.isEnabled = true
        Log.d("MainActivity", "Load finished: $this")

    
	
#8.5 Корутины "под капотом"

все suspend функции - это функции с колбэками

private suspend fun loadData() {
//Первый блок
        Log.d("MainActivity", "Load started: $this")
        binding.progress.isVisible = true
        binding.buttonLoad.isEnabled = false
		// прерываемый ,т.е. при вызове метода loadCity, мы вйдем из метода loadData до тех пор пока он не завершится, 
		// и когда он завершится, мы вернёмся в этот метод к следующей строчке
        val city = loadCity() 
//Второй блок		
        binding.tvLocation.text = city
        val temp = loadTemperature(city)
//Третий блок		
        binding.tvTemperature.text = temp.toString()
        binding.progress.isVisible = false
        binding.buttonLoad.isEnabled = true
        Log.d("MainActivity", "Load finished: $this")
}

Напишем тот же метод без корутин

private fun loadWithoutCoroutine(step: Int = 0, obj: Any? = null) {
        when (step) {
            0 -> {
                Log.d("MainActivity", "Load started: $this")
                binding.progress.isVisible = true
                binding.buttonLoad.isEnabled = false
                loadCityWithoutCoroutine {
                    loadWithoutCoroutine(1, it)
                }
            }
            1 -> {
                val city = obj as String
                binding.tvLocation.text = city
                loadTemperatureWithoutCoroutine(city) {
                    loadWithoutCoroutine(2, it)
                }
            }
            2 -> {
                val temp = obj as Int
                binding.tvTemperature.text = temp.toString()
                binding.progress.isVisible = false
                binding.buttonLoad.isEnabled = true
                Log.d("MainActivity", "Load finished: $this")
            }
        }
    }
	//метод с колбэком
	  private fun loadCityWithoutCoroutine(callback: (String) -> Unit) {
        Handler(Looper.getMainLooper()).postDelayed({
            callback.invoke("Moscow")
        }, 5000)
    }
	//метод с колбэком
	 private fun loadTemperatureWithoutCoroutine(city: String, callback: (Int) -> Unit) {
        runOnUiThread {
            Toast.makeText(
                this,
                getString(R.string.loading_temperature_toast, city),
                Toast.LENGTH_SHORT
            ).show()
        }
        Handler(Looper.getMainLooper()).postDelayed({
            callback.invoke(17)
        }, 5000)
    }
	Внутри корутин примерно тоже самое - так называемая стейт-машина с колбэками
	
	Suspend функции никогда не должны блокировать поток. Если пишем свою Suspend функцию, то сами должны заботиться о том, 
	чтобы она не блокировала поток, модификатор suspend этого не делает
	
	Внутри активити используем lifecycleScope.launch
	Внутри ViewModel можно создать scope и отменить в onCleared
	private val scope = CoroutineScope(Dispatchers.Default)
	override fon onCleared(){
		super.onCleared()
		scope.cancel()
	}
	А можно использовать viewModelScope + его не нужно явно отменять в onCleared

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
	
#14.5 Sealed Classes

Теперь меняем State

open class State()

class Error : State()

class Progress : State()

class Result(val factorial : String) : State()

 private fun observeViewModel(){
        viewModel.state.observe(this){
            binding.progressBarLoading.visibility = View.GONE
            binding.buttonCalculate.isEnabled = true
            when(it){
                is Error -> {
                    Toast.makeText(this,
                        "You did not entered value",
                        Toast.LENGTH_SHORT)
                        .show()
                }
                is Progress ->{
                    binding.progressBarLoading.visibility = View.VISIBLE
                    binding.buttonCalculate.isEnabled = false
                }

                is Result -> {
                    binding.textViewFactorial.text = it.factorial
                }
            }

        }

    }
	
	Sealed класс - это enum классов и всех наследников нужно определить, либо в этом же файле, либо в этом же пакете
	И таким образом компилятор сможет отслеживать стейты при переборе в when. Появится предупреждение, что обработаны не все классы
	
	sealed class State()
	
	В результате
	
sealed class State()

object Error : State()

object Progress : State()

class Result(val factorial : String) : State()

class FactorialViewModel : ViewModel() {

    private val _state = MutableLiveData<State>()

    val state : LiveData<State>
        get() = _state



    fun calculate(value : String?){
        _state.value = Progress
        if(value.isNullOrBlank()){
            _state.value = Error
            return
        }

        viewModelScope.launch {
            val number = value.toLong()
            //calculate
            delay(1000)
            _state.value = Result(factorial = number.toString())
        }

    }
}

 private fun observeViewModel(){
        viewModel.state.observe(this){
            binding.progressBarLoading.visibility = View.GONE
            binding.buttonCalculate.isEnabled = true
            when(it){
                is Error -> {
                    Toast.makeText(this,
                        "You did not entered value",
                        Toast.LENGTH_SHORT)
                        .show()
                }
                is Progress ->{
                    binding.progressBarLoading.visibility = View.VISIBLE
                    binding.buttonCalculate.isEnabled = false
                }

                is Result -> {
                    binding.textViewFactorial.text = it.factorial
                }
            }
        }
    }
	
#14.6 WithContext and SuspendCoroutine
Долгая операция должна выполняться в другом потоке
Если нужно работать с большими числами нужно использовать BigInteger

private fun factorial(number : Long) : String{
        var result = BigInteger.ONE
        for(i in 1..number){
            result = result.multiply(BigInteger.valueOf(i))
        }
        return result.toString()
}
    
	
fun calculate(value: String?) {
        _state.value = Progress
        if (value.isNullOrBlank()) {
            _state.value = Error
            return
        }

        viewModelScope.launch {
            val number = value.toLong()
            //calculate
            delay(1000)
            val result = factorial(number)
            _state.value = Factorial(factorial = result)
        }

}
	
При больших числах функция выполняется долго и её нужно сделать прерываемой
##Превый способ - метод с колбэком suspendCoroutine
Если из метода нужно сделать suspend функцию нужно использовать suspendCoroutine, 
который использует Continuation<T>. В него нужно передавать результат.

private suspend fun factorial(number : Long) : String{

        return suspendCoroutine {
            thread {
                var result = BigInteger.ONE
                for(i in 1..number){
                    result = result.multiply(BigInteger.valueOf(i))
                }
				//очень важно вызвать результат!!! иначе корутина не завершится и зависнет
                it.resumeWith(Result.success(result.toString()))
            }
        }

    }
При ошибке в результат передают Result.failure<>()

##Второй способ - если нужно просто переключить поток

    private suspend fun factorial2(number: Long): String {

        return withContext(Dispatchers.Default) {
            var result = BigInteger.ONE            
            for (i in 1..number) {
                result = result.multiply(BigInteger.valueOf(i))
            }
            result.toString() //результат - последняя строка
        }
    }
	
#14.7 CoroutineScope and CoroutineContext
Переключение контекста очень удобная вещь и в рамках одного scope можно переключать контекст(поток) много раз 
Например функция factorial не Suspend и находится в другой библиотеке

 viewModelScope.launch {
            val number = value.toLong()
            //calculate
            withContext(Dispatchers.Default) {
                delay(1000)
                val result = factorial(number)
                withContext(Dispatchers.Main) {
                    _state.value = Factorial(factorial = result)
                }
            }
        }
		
Т.к. WithContext вовзвращает результат, можем упростить

 viewModelScope.launch {
			//главный 
            val number = value.toLong()
            //фоновый
            val result = withContext(Dispatchers.Default) {
                delay(1000)
                factorial(number)
			}
            //главный
            _state.value = Factorial(factorial = result)
                
            }
        }		
CoroutineScope - интерфейс, в котором одна переменная CoroutineContext
CoroutineContext состоит из 4х элемнтов
1)  Dispatcher - поток
2) Job
3) ExceptionHandler - обработка ошибок
4) CoroutineName - имя

Может использоваться один элемент или их сочетание через +
CoroutineContext используется в 
- WithContext
- при создании CoroutineScope
- в билдерах launch, async...

#14.8 Structured Concurrency

#14.9 Exception Handling

#14.10 Async vs Launch

#14.11 Cancelling Coroutines

Coroutine Flow

#15.1 Введение в Coroutine Flow
#15.2 Flow Builders
 flowOf(1,3,5,6) --> Flow<Int> - создание потока аналогично созданию клллекции
 Обычно asFlow и flowOf -  чаще всего используется в UnitTest
 
 Более практичный
 flow{
 this.emit(1)
 //здесь можно писать абслоютно любую логику
 //внутри используется CoroutineContext поэтому можно использовать suspend функции
 delay(900)
 }
 
В этот поток нельзя положить значения снаружи, можно промежуточные и терминальные

Чистые корутины - императивный подходе
Flow - реактивный подходе


object UsersRepository {

    private val users = mutableListOf("Nick", "John", "Max")

    suspend fun addUser(user: String) {
        delay(10)
        users.add(user)
    }

    suspend fun loadUsers(): List<String> {
        delay(10)
        return users.toList()
    }
}

class UsersViewModel : ViewModel() {

    private val repository = UsersRepository

    private val _users = MutableLiveData<List<String>>()
    val users: LiveData<List<String>> = _users

    init {
        loadUsers()
    }

    fun addUser(user: String) {
        viewModelScope.launch {
            repository.addUser(user)
        }
    }

//императивный подход - нет автоматического обновления данных и метод нужно вызывать при каждом обновлении списка
    private fun loadUsers() {
        viewModelScope.launch {
            _users.value = repository.loadUsers()
        }
    }
}


Чистые корутины не поддерживают реактиный подход. Нельзя подписаться на коллекцию - нужно самим следить когда обновляются данные
Проблемы автоматического обновления решаются с помощью flow & observer

fun loadUsers(): Flow<List<String>> = flow {
//пока делаем бесконечный цикл, чтобы emit происходил периодически 
//плохое решение, но на текущем этапе его достаточно
       while (true) {
           emit(users.toList())
           delay(500)
       }
    }	

    private fun loadUsers() {
        viewModelScope.launch {
		//подписываемся на поток
            repository.loadUsers().collect {
                _users.value = it
            }
        }
    }
 
 #15.3 Flow builder and terminal operators
 
 private fun getFlowByFlowBuilder() : Flow<Int>{
        return flowOf(1,5,9,12,38,46,54,99,111)
    }

//Вызов одного Flow из другого
private fun getFlow() : Flow<Int>{
        val first = getFlowByFlowBuilder()
        return flow {
//            first.collect{
//Но здесь можно добавлять доп.операторы в отличае от emitAll
//                emit(it)
//            }
//другой способ
            emitAll(first)
        }
Причём методы не suspend -т.к. содержат только промежуточные операции
Но чтобы flow запустить на выполнение нужен терминальный оператор. А все терминальные операторы являются suspend- функции
Поэтому запустить flow можно только из контекста корутины. Поэтому flow выполняется в том контексте, в котором вызван терминальный оператор

Другие терминальные операторы
toList() - причём создаётся список после окончания обработки данных. Если данные эммитятся бесконечно результат никогда не выполнится
first() - после выполнения и получения первого элемента , корутина прерывается и эмитты не происходят
last() - при бесконечном эмитте никогда не завершится
}