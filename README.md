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
Набор правил, которые используются при работе с корутинами
1) Каждая корутина должна быть запущена внутри скоупа, с каим-нибудь ж.ц.
2) Все корутины запускаются в виде иерархии объектов Job

Corutine Scope 

- Dispatcher
- Job
- Coroutine Name
- Exception Handler

Job - находится на вершине иерархии

       Job*
	/		\
childJob1  childeJob2 - ДРЕВОВИДНАЯ СТРУКТУРА

class MainViewModel : ViewModel() {

    private val parentJob = Job()
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob)
    fun method(){
        val childJob1 = coroutineScope.launch {
            delay(3000)
            Log.d(LOG_TAG,"first coroutine finished")
        }
        val childJob2 = coroutineScope.launch {
            delay(2000)
            Log.d(LOG_TAG,"second coroutine finished")
        }
        Log.d(LOG_TAG,parentJob.children.contains(childJob1).toString()) - #TRUE
        Log.d(LOG_TAG,parentJob.children.contains(childJob2).toString())	#TRUE
    }

    override fun onCleared() {
        super.onCleared()
        coroutineScope.cancel()
    }

    companion object{
        private const val LOG_TAG = "MainViewModel"
    }
}

3) Пока дочерние Job-ы не завершили свою работу, родительская Job будет активной и не будет закрыта isActive = true

 fun method(){
        val childJob1 = coroutineScope.launch {
            delay(3000)
            Log.d(LOG_TAG,"first coroutine finished")
        }
        val childJob2 = coroutineScope.launch {
            delay(2000)
            Log.d(LOG_TAG,"second coroutine finished")
        }
		
		thread{
			Tread.sleep(1000)
			Log.d(LOG_TAG,"Parent job is Active ${parentJob.isActive()}") - #TRUE
		}
        Log.d(LOG_TAG,parentJob.children.contains(childJob1).toString()) - #TRUE
        Log.d(LOG_TAG,parentJob.children.contains(childJob2).toString())	#TRUE
    }
4) Поведение корутин при отмене корутин - Если родительская корутина отменяется - отменяются и дочернии. Но не НАОБОРОТ -
 если отменяется дочерняя, родительская и все остальные будут работать
 
  fun method(){
        val childJob1 = coroutineScope.launch {
            delay(3000)
            Log.d(LOG_TAG,"first coroutine finished") // Лог не будет выведен
        }
        val childJob2 = coroutineScope.launch {
            delay(2000)
            Log.d(LOG_TAG,"second coroutine finished") // Лог не будет выведен
        }
		
		thread{
			Tread.sleep(1000)
			parentJob.cancel() // отменяем родительскую
			Log.d(LOG_TAG,"Parent job is Active ${parentJob.isActive()}") //FALSE
		}
        Log.d(LOG_TAG,parentJob.children.contains(childJob1).toString()) - #TRUE
        Log.d(LOG_TAG,parentJob.children.contains(childJob2).toString())	#TRUE
    }
5) в следующем разделе

#14.9 Exception Handling

Исключения 
Добавим третью корутину с Exception

 fun method(){
        val childJob1 = coroutineScope.launch {
            delay(3000)
            Log.d(LOG_TAG,"first coroutine finished")
        }
        val childJob2 = coroutineScope.launch {
            delay(2000)
            Log.d(LOG_TAG,"second coroutine finished")
        }

        val childJob3 = coroutineScope.launch {
            delay(1000)
            error() #!!!
            Log.d(LOG_TAG,"second coroutine finished")
        }
        Log.d(LOG_TAG,parentJob.children.contains(childJob1).toString())
        Log.d(LOG_TAG,parentJob.children.contains(childJob2).toString())
    }
  private fun error(){
        throw RuntimeException("Error")
    }
	
	При запуске такое приложение упадёт, т.к. нет обработчика ошибок
	
	Если обернуть корутину 3 в try..catch - это не поможет, поэтому это не имеет смысла.Если корутина вызывает исключение, то это проблема корутины
	Если нужно поймать исключение - то нужно именно этот участок внутри корутины обернуть в try..catch
	Но есть и другой способ
	Создадим
	private val exceptionHandler = CoroutineExceptionHandler{
        _, throwable -> Log.d(LOG_TAG,"CoroutineExceptionHandler catch $throwable")
    }
	и передаём его в скоуп
	 private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob + exceptionHandler)
	 
	 Далее, зпускем
	 Выведется лог CoroutineExceptionHandler catch RuntimeException
	 Больше никакие логи о завершении корутин не выводятся. Вывод. Исключение произошло в корутине 3, при этом родительская корутина сумела оьработать это исключение
	 
	Отсюда мы видим, что дочерние корутины передают исключения вверх по иерархии своим родительским корутинам
	
				 /-->  Job*
				/	/		\
		 /-------> Job  	Job
		/		/
Exception	Job

    И это правило 5 из Structured Concurrency : Если в какой-то из Job произошло исключение, то это исключение передаётся вверх по иерархии
	И если родительская корутина не умеет обрабатывать исключения, то будет краш, но если передать ExceptionHandler, то оно будет обработано там
	И второе - если произошло исключение в какой-то корутине, то все остальные корутины, запущенные в том же скоупе будут отменены.
	Это не всегда так.
	
	
#14.10 Async vs Launch

Заменим третью корутину на Async

class MainViewModel : ViewModel() {

    private val parentJob = Job()
    private val exceptionHandler = CoroutineExceptionHandler{
        _, throwable -> Log.d(LOG_TAG,"CoroutineExceptionHandler catch $throwable")
    }
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob + exceptionHandler)
    fun method(){
        val childJob1 = coroutineScope.launch {
            delay(3000)
            Log.d(LOG_TAG,"first coroutine finished")
        }
        val childJob2 = coroutineScope.launch {
            delay(2000)
            Log.d(LOG_TAG,"second coroutine finished")
        }

        val childJob3 = coroutineScope.async {
            delay(1000)
            error()
            Log.d(LOG_TAG,"second coroutine finished")
        }
        
        Log.d(LOG_TAG,parentJob.children.contains(childJob1).toString())
        Log.d(LOG_TAG,parentJob.children.contains(childJob2).toString())
    }
	
При запуске приложение не падат и в лог ничего не выводит. Т.е. все корутины отменены, но ExceptionHandler не поймал Exception
Дело в том, что при вызове async исключение сохраняется в объекте Deferred!!и его не будет ловить ExceptionHandler

Сделаем следующее. Вернём launch а async  добавим внутрь. Уберём обработчки ExceptionHandler


val childJob3 = coroutineScope.launch {
			async{
				delay(1000)
				error()
				Log.d(LOG_TAG,"second coroutine finished")
			}
        }

Кажется, что исключение должно попасть в Deferred,но в данном случае приложение падает.
Здесь получается внутри корутины launch произошло исключение и оно пошло вверх по иерархии

Если сделать так
val childJob3 = coroutineScope.async {
			async{
				delay(1000)
				error()
				Log.d(LOG_TAG,"second coroutine finished")
			}
        }
		то исключение внутри async попадет в объект Deferred и не пойдёт вверх по иерархии
		
		Но исключение никуда не девается. Если сделаем так
CoroutineScope.launch{
	childeJob3.await() - этот метод бросит исключение, его можно обернуть здесь в try..catch
	}
	то исключение уйдёт на обработку в ExceptionHandler
		
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

#15.4 Операторы жизненного цикла Flow

 private fun loadData() {
        viewModelScope.launch {
         repository.getCurrencyList()//flow функция
                .onStart {
                    val currentState = _state.value
                    if (currentState !is State.Content || currentState.currencyList.isEmpty()) {
                        _state.value = State.Loading
                    }
                }
                .onEach { _state.value = State.Content(currencyList = it) }
                .collect()
        }
    }
	
А можно по другому передать в scope

    private fun loadData() {

        repository.getCurrencyList()
            .onStart {
                val currentState = _state.value
                if (currentState !is State.Content || currentState.currencyList.isEmpty()) {
                    _state.value = State.Loading
                }
            }
            .onEach { _state.value = State.Content(currencyList = it) }
            .launchIn(viewModelScope) - терминальная функция, которая является не suspend
    }
	
#15.5 Map Flow to LiveData

Предыдущий срлслб упрощаем  Flow<List<Currency>> map to Flow<State> В onStart можно эмиттить данные
 private fun loadData() {

        repository.getCurrencyList()
            .filter { it.isNotEmpty() }
            .map { State.Content(currencyList = it) as State }
            .onStart {
                emit(State.Loading)
            }
            .onEach { _state.value = it }
            .launchIn(viewModelScope)
    }
	
!!!Ещё упрощаем
class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    val state: LiveData<State> = repository.getCurrencyList()
        .filter { it.isNotEmpty() }
        .map { State.Content(currencyList = it) as State }
        .onStart {
            emit(State.Loading)
        }
        .asLiveData() - просто превращаем в LiveData

}
	
	
#15.6 
Вернём код 
fun loadData() {
        repository.getCurrencyList()
            .onStart {
               _state.value = State.Loading
            }
            .onEach {
                Log.d("CryptoViewModel","onEach" ) //добавим комментарий
                _state.value = State.Content(currencyList = it) }
            .launchIn(viewModelScope)
    }
	
При сворачивании приложения загрузка не прекращается
Немного перепишем

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    private val _state = MutableLiveData<State>(State.Initial)
    val state: LiveData<State> = _state

    private var job : Job? = null

//    init {
//        loadData()
//    }

    public fun loadData() {
        job = repository.getCurrencyList()
            .onStart {
               _state.value = State.Loading
            }
            .onEach {
                Log.d("CryptoViewModel","onEach" )
                _state.value = State.Content(currencyList = it) }
            .launchIn(viewModelScope)
    }

    fun stopLoading(){
        job?.cancel()
    }
}

Вызовем методы загрузки и отмены в OnResume и OnPause

Тут есть минусы
1) Добавление Jop
2) Public методы + не забыть их вызывать в соотв.методах ЖЦ активити
3) Progres показывается при старте
4) При перевороте загрузка начинается занова


onCompletion - цепочка завершается успешно или неуспешно

Решение некоторых из проблем


 fun getCurrencyList() = flow<List<Currency>> {
        emit(currencyList.toList()) - эмитим начальное закешированное состояние
        while (true) {
            delay(3000)
            generateCurrencyList()
            emit(currencyList.toList())
            delay(3000)
        }
    }
	
	
class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    private val _state = MutableLiveData<State>(State.Initial)
    val state: LiveData<State> = _state

    private var job : Job? = null
    private var isResumed = false - не сразу отменяем загрузку данных, а с задержкой

    public fun loadData() {
        isResumed = true
        if(job != null){
            return
        }
        job = repository.getCurrencyList()
            .onStart {
               _state.value = State.Loading
                Log.d("CryptoViewModel","onStart " )
            }
            .onEach {
                Log.d("CryptoViewModel","onEach" )
                _state.value = State.Content(currencyList = it) }
            .onCompletion {
                Log.d("CryptoViewModel","onCompletion $it" )
            }
            .launchIn(viewModelScope)
    }

    fun stopLoading(){
        viewModelScope.launch {
            delay(5000)
            if(!isResumed){
                job?.cancel()
                job = null
            }else{
                isResumed = false
            }
        }
    }
}

AsLiveData всё это обеспечивает
- Если пользователь ушёл с экрана, загрузка приостанавливается через какой-то delay 
Есть параметр timeout

#15.7 Использование Flow на Ui-слое

Предназначение LiveData такое же как Flow. Поэтому на flow можно подписываться на UI-слое и отказаться от доп.объекта LiveData
Изменим возвращаемое значение на Flow

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    val state: Flow<State> = repository.getCurrencyList()
        .filter { it.isNotEmpty() }
        .map { State.Content(it) as State }
        .onStart {
            emit(State.Loading)
            Log.d("CryptoViewModel", "onStart ")
        }
        .onEach {
            Log.d("CryptoViewModel", "onEach")
            //_state.value = State.Content(currencyList = it)
        }
        .onCompletion {
            Log.d("CryptoViewModel", "onCompletion $it")
        }        


}

В активити

 private fun observeData() {
        lifecycleScope.launch {
            viewModel.state.collect {
                when (it) {
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
    }

Но в таком виде данные обновляются в фоне
Можно сохранить job и отменять его в onPause. Всё будет работать. Но при развороте экрана flow завершает работу. 
При этом добавить таймер не получится. Активити при перевороте экрана уничтожается и скоуп завершит свою работу.
Это будем решать в след.уроках
Ок, но это решение с job неудобное. Есть другие способы

1)#repeatOnLifecycle - flow завязан на lifecycle
 private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) { 
                viewModel.state.collect {
                    when (it) {
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
        }
    }
	
2)flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
 private fun observeData() {
        lifecycleScope.launch {            
            viewModel.state
                .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .collect {
                when (it) {
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
    }
	
	У этих двух способов есть одно отличие 
	flowWithLifecycle применяется к тому flow, который находится выше него - UPSTREAM
	
	пример
	
	viewModel.state
                .flowWithLifecycle(lifecycle, Lifecycle.State.RESUMED)
                .transform {
                    Log.d("CryptoViewModel", "transform")
                    delay(10_000)
                    emit(it)
                }
				
	transform - будет выполнятся не смотря на то,что flow выше будет отменён
3) launchWhenResumed и прочие являются небезопасными и не рекомендуются