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
	
	#SupervisorJob
	
	private val parentJob = SupervisorJob()
    private val exceptionHandler = CoroutineExceptionHandler{
        _, throwable -> Log.d(LOG_TAG,"CoroutineExceptionHandler catch $throwable")
    }
    private val coroutineScope = CoroutineScope(Dispatchers.Main + parentJob + exceptionHandler
	
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
        coroutineScope.launch {
			try{
				childJob3.await()
			}catch(ex : Exception){
			}
        }
    }

           ru.melolchik.coroutinestart          D  CoroutineExceptionHandler catch java.lang.RuntimeException: Error
           ru.melolchik.coroutinestart          D  second coroutine finished
           ru.melolchik.coroutinestart          D  first coroutine finished
	В случае исключения (или отмены) одной корутины ,остальные корутины продолжают работать. При этом ExceptionHandler работает также.
	
	viewModelScope как раз использует SupervisorJob + Dispatcher.Main и автоматические отменяется в функции onCleared!
		
#14.11 Cancelling Coroutines


fun method(){
       val job =  viewModelScope.launch (Dispatchers.Default + exceptionHandler){
        Log.d(LOG_TAG, "Started ")
            val before = System.currentTimeMillis()
            var count = 0
            for (i in 0 until 100_000_000){
                for (j in 0 until 100){
                    count++
                }
            }
            Log.d(LOG_TAG, "Finished ${System.currentTimeMillis() - before}")
        }
		//слушатель на завершение работы
        job.invokeOnCompletion {
            Log.d(LOG_TAG, "Coroutine was canceled $it")
        }
		//Метод выполняется около 10 сек.Отменим корутину через 3 сек 
        viewModelScope.launch {
            delay(3000)
            job.cancel()

        }
    
	Логи такие
  Started 
  Finished 10172 - корутина закончила действие через 10 сек( хотя была отменена через 3) и только потом стала отменена
  Coroutine was canceled kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled; job=StandaloneCoroutine{Cancelled}@27fda12
  
  Логика аналогична потокам и методу intrrupt(), который не останавливает поток, а только меняет его флаг isInterrupted
  Если вызвать cancel, он не останавливает выполнение корутины, только устанавливает флаг isActive в false
  
   fun method(){
       val job =  viewModelScope.launch (Dispatchers.Default + exceptionHandler){
        Log.d(LOG_TAG, "Started ")
            val before = System.currentTimeMillis()
            var count = 0
            for (i in 0 until 100_000_000){
                for (j in 0 until 100){
                    if(isActive) {
                        count++
                    }else{
                        throw CancellationException - особый вид исключений в корутинах, который не нужно обрабатывать в ExceptionHandler, 
						его не нужно обрабатывать в try catch, он не крашит приложение
						//Он говорит о том, что корутина была отменена
                    }
//                    ensureActive()
                    count++
                }
            }
            Log.d(LOG_TAG, "Finished ${System.currentTimeMillis() - before}")
        }
        job.invokeOnCompletion {
            Log.d(LOG_TAG, "Coroutine was canceled $it")
        }
        viewModelScope.launch {
            delay(3000)
            job.cancel()

        }
    }
Итог
 Started 
 Coroutine was canceled kotlinx.coroutines.JobCancellationException: StandaloneCoroutine was cancelled; job=StandaloneCoroutine{Cancelled}@4ec23ad
 
 Стандартные функции обрабатывают isActive, напрмер delay() и вместо проверки можно использовать ensureActive()



Coroutine Flow

#15.1 Введение в Coroutine 
Список не является потоком данных. Данные преобразуются в цикле.
В потоке данные преобразуются по очереди.
Поток выполняет работу только при наличии терминального оператора
Промежуточные и терминальные операторы
asSequence() - является потоком данных, НО НЕ ПОДДЕРЖИВАЮТ АСИНХРОНННУЮ ОБРАБОТКУ, т.е. нельзя использовать с корутинами
Пример:
Следующий код не допустим

fun method(){
         viewModelScope.launch {
             val numbers = listOf(3, 4, 5, 98, 19, 48, 25, 32).asSequence()
             numbers.filter { it.isPrime() }
                 .filter { it < 20 }
                 .forEach { Log.d(LOG_TAG, it.toString()) }
         }
    }
	
	
suspend fun Int.isPrime() : Boolean{
    if(this <= 1) return false
    for(i in 2 .. this/2){
        delay(50)
        if(this % i == 0) return false
    }
    return true
}

Поэтому используют FLOW

fun method(){
         viewModelScope.launch {
             val numbers = listOf(3, 4, 5, 98, 19, 48, 25, 32).asFlow() - flow builder
             numbers.filter { it.isPrime() }
                 .filter { it < 20 }
                 .collect { Log.d(LOG_TAG, it.toString()) } - аналог forEach для Sequence
         }
    }
	
#15.2 Flow Builders
 flowOf(1,3,5,6) --> Flow<Int> - создание потока аналогично созданию коллекции
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

Первоначальная инициализация для работы с приложением криптовалют
Добавляем CryptoActivity, CryptoAdapter 

object CryptoRepository {

    private val currencyNames = listOf("BTC", "ETH", "USDT", "BNB", "USDC")
    private val currencyList = mutableListOf<Currency>()

    fun getCurrencyList() = List<Currency> {
      
        delay(3000) - эмитация длительной загрузки
        generateCurrencyList()
        return currencyList.toList()            
       
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


class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    private val _state = MutableLiveData<State>(State.Initial)
    val state: LiveData<State> = _state

    init {
        loadData()
    }

    private fun loadData() {
	
		viewModelScope.launch{
		
		val currentState = _state.value
		if(currentState !is State.Content || currentState.currencyList.isEmpty()){
			_state.value = State.Loading
		}
		val currencyList = repository.getCurrencyList()
		_state.value = State.Content(currencyList = currencyList)
		delay(3000) - таймаут между обновлениями данных	
		}
	}
}

Здесь мы используем императивный подход, эта проблема решается с использованием реактивного подхода и Flow

Меняем репозиторий и viewModel

object CryptoRepository {

    private val currencyNames = listOf("BTC", "ETH", "USDT", "BNB", "USDC")
    private val currencyList = mutableListOf<Currency>()

    fun getCurrencyList() = flow<List<Currency>> {
        while (true) {
            delay(3000) - эмитация долгой загрузки
            generateCurrencyList()
            emit(currencyList.toList())
            delay(3000) - таймаут для обновлений 3 сек
        }
    }
	....
}


class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    private val _state = MutableLiveData<State>(State.Initial)
    val state: LiveData<State> = _state

    init {
        loadData()
    }

    private fun loadData() {
	
		viewModelScope.launch{
		
			val currentState = _state.value
			if(currentState !is State.Content || currentState.currencyList.isEmpty()){
				_state.value = State.Loading
			}
			repository.getCurrencyList().collect{
				_state.value = State.Content(currencyList = it)
			}
		
		}
	}
}

....
Теперь рассмотрим другие операторы flow
 private fun loadData() {
        viewModelScope.launch {
         repository.getCurrencyList()//flow функция
                .onStart { - реакция на подписку, перед тем как прилетят первые данные
                    val currentState = _state.value
                    if (currentState !is State.Content || currentState.currencyList.isEmpty()) {
                        _state.value = State.Loading
                    }
                }
                .onEach { _state.value = State.Content(currencyList = it) } - можно реагировать на каждый эммит
                .collect()
        }
    }
	
А можно по другому передать в scope. Это более читаемый вариант

    private fun loadData() {

        repository.getCurrencyList()
            .onStart { - реакция на подписку, перед тем как прилетят первые данные
                val currentState = _state.value
                if (currentState !is State.Content || currentState.currencyList.isEmpty()) {
                    _state.value = State.Loading
                }
            }
            .onEach { _state.value = State.Content(currencyList = it) } - можно реагировать на каждый эммит
            .launchIn(viewModelScope) - терминальный оператор, которая является НЕ suspend функцией, т.е. это исключение из правил терминальных операторов - 
			под капотом он вызывает launch и пустой метод collect!, т.е. тоже самое, что мы делали до этого
    }
	
#15.5 Map Flow to LiveData

.onStart { - реакция на подписку, перед тем как прилетят первые данные
                val currentState = _state.value
                if (currentState !is State.Content || currentState.currencyList.isEmpty()) {
                    _state.value = State.Loading
                }
            }
можно упростить, т.к. он вызывается при подписке и никакого _state изначально нет, его можно сразу установить в Loading

 private fun loadData() {

        repository.getCurrencyList()
            .onStart {
                _state.value = State.Loading
                
            }
            .onEach { _state.value = State.Content(currencyList = it) } 
            .launchIn(viewModelScope)
    }
В текущей версии установка state происходит в двух местах. Было бы лучше, если бы это было одно место
Лямбда внутри функции onStart является FlowCollector<List<Currency>> тоже что и внутри flow{}, поэтому здесь мы можем эмиттить значения
	
Преобразуем  Flow<List<Currency>> в Flow<State> используя map

 private fun loadData() {
        repository.getCurrencyList()
            .filter { it.isNotEmpty() }
            .map { State.Content(currencyList = it) as State } - преобразование коллекции в объект State - as State обязательно, чтобы передавать любой State
            .onStart {
                emit(State.Loading)
            }
            .onEach { _state.value = it } - установка в LiveData
            .launchIn(viewModelScope)
    }
	
!!!Ещё упрощаем. Если присмотреться LiveData и Flow делают одинаковые действия. И сейчас сделаем преобразование flow в LiveData:

 private fun loadData() {
        repository.getCurrencyList()
            .filter { it.isNotEmpty() }
            .map { State.Content(currencyList = it) as State } - преобразование коллекции в объект State - as State обязательно, чтобы передавать любой State
            .onStart {
                emit(State.Loading)
            }
            .onEach { _state.value = it } - установка в LiveData
            .asLiveData() - просто превращаем в LiveData
    }
	Итого, убираем всё лишнее
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
	
При сворачивании приложения загрузка не прекращается - в логах выводится onEach. Это не очень хорошо - трата ресурсов. Нужна привязка к методам активити onResume/onPause
Немного перепишем

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    private val _state = MutableLiveData<State>(State.Initial)
    val state: LiveData<State> = _state

//сохранима Job
    private var job : Job? = null

//    init {
//        loadData()
//    }

    public fun loadData() { // этот метод будем вызывать в onResume
        job = repository.getCurrencyList()
            .onStart {
               _state.value = State.Loading
            }
            .onEach {
                Log.d("CryptoViewModel","onEach" )
                _state.value = State.Content(currencyList = it) }
            .launchIn(viewModelScope)
    }
// Отменим job
    fun stopLoading(){ // этот метод будем вызывать в onPause
        job?.cancel()
    }
}

Вызовем методы загрузки и отмены в OnResume и OnPause. Всё работает корректно, но 

Тут есть минусы
1) Добавление Job
2) Public методы + не забыть их вызывать в соотв.методах ЖЦ активити
3) Progress показывается при каждом старте. При каждой новой подписке создаётся новый поток и каждый раз вызывается onStart
4) При перевороте загрузка начинается занова


onCompletion - цепочка завершается успешно или неуспешно

Решение проблемы с прогрессом:

 fun getCurrencyList() = flow<List<Currency>> {
        emit(currencyList.toList()) - эмитим начальное закешированное состояние, чтобы не показывался прогресс
        while (true) {
            delay(3000)
            generateCurrencyList()
            emit(currencyList.toList())
            delay(3000)
        }
    }
	
Но есть другая проблема - при повороте экрана, поток также пересоздаётся, что хотелось бы улучшить, чтобы загрузка не прерывалась
Для этого добавим задержку в stopLoading delay(5000) перед отменой Job
	
	
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
Мы получили рабочее решение, но достаточно громосткое

AsLiveData всё это обеспечивает
Убираем Job, убираем stopLoading
Возвращаем asLiveData - он обеспечивает необходимое поведение, о котором говорилось в уроке
- Если пользователь ушёл с экрана, загрузка приостанавливается через какой-то delay 
Есть параметр timeout - таймаут для переворота

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
            viewModel.state.collect {// меняем observe на collect!!! но выполняем всё в scope
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
	flowWithLifecycle применяется к тому flow, который находится выше него - UPSTREAM, т.е. к flow state
	Но flow ниже по цепочке trasform - будет работать даже после onPause, обрабатывать state, которые пришли раньше и не были обработаны
	
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


#15.8 Холодные Flow

suspend fun main(){

    val flow = getFlow()

    flow.collect{
        println(it)
    }
}

fun getFlow() : Flow<Int> = flow {
    repeat(100){
        println("Emitted: $it")
        emit(it)
        delay(1000)
    }
}

Что произойдёт, если у flow дважды вызвать collect?

suspend fun main(){

    val flow = getFlow()

    flow.collect{
        println(it)
    }
	
	flow.collect{
        println(it)
    }
}

Второй collect начнёт выполнятся только после полного выполнения первого

Emitted: 0
collect 1st: 0
Emitted: 1
collect 1st: 1
Emitted: 2
collect 1st: 2
Emitted: 3
collect 1st: 3
Emitted: 4
collect 1st: 4
Emitted: 0
collect 2nd: 0
Emitted: 1
collect 2nd: 1
Emitted: 2
collect 2nd: 2
Emitted: 3
collect 2nd: 3
Emitted: 4
collect 2nd: 4

Чтобы оба метода выполнялись одновременно их нужно запустить в разных коррутинах

val coroutineScope = CoroutineScope(Dispatchers.IO)
suspend fun main(){

    
    val flow = getFlow()
    
    val job1 = coroutineScope.launch {
        flow.collect {
            println("collect 1st: $it")
        }
    }
    val job2 = coroutineScope.launch {
        flow.collect {
            println("collect 2nd: $it")
        }
    }
    
    job1.join()
    job2.join()
}

Без join ничего выводиться не будет. Т.к. основной поток завершится и потянет за собой доп.корутины. 
Особенность реализации корутин - когда главный поток завершил свою работу, то все корутины отменяются. Под копотом диспатчеры используют потоки - демоны


Emitted: 0
Emitted: 0
collect 1st: 0
collect 2nd: 0
Emitted: 1
collect 1st: 1
Emitted: 1
collect 2nd: 1
Emitted: 2
collect 2nd: 2
Emitted: 2
collect 1st: 2
Emitted: 3
Emitted: 3
collect 2nd: 3
collect 1st: 3
Emitted: 4
collect 2nd: 4
Emitted: 4
collect 1st: 4

Process finished with exit code 0
Корутины выполняются параллельно и главный поток ждёт их завершения

Холодные поток. Особенности
1) Они не эмиттят данные , пока на них не подпишутся. Пока не будет вызван терминальный оператор

2) На каждую подписку создаётся новый flow - новый поток данных
Если поставить delay(5000) между корутинами, вторая подписка начнётся также с 0 
3) Если какому-то коллетору данные больше не нужны , то и поток прекратит своё выполнение . Пример : first

#15.9 Горячие Flow. MutableSharedFlow

Один из видов горячих потоков это SharedFlow, но это интерфейс и создать его можно только через ФУНКЦИЮ MutableSharedFlow() (хотя выглядит как конструктор объекта)

При создании холодных flow нужно сразу определить все элементы, которые в нём будут, Вставлять в него значения снаружи нельзя!Можно только подписываться

С горячими flow дело обстоит по другому. На них можно подписываться и эмиттить значения снаружи


suspend fun main(){

    val flow = MutableSharedFlow<Int>()
    
    coroutineScope.launch {
        repeat(5){
            println("Emitted: $it")
            flow.emit(it)
            delay(1000)
        }
    }

    val job1 = coroutineScope.launch {
        flow.collect {
            println("collect 1st: $it")
        }
    }
    val job2 = coroutineScope.launch {
        flow.collect {
            println("collect 2nd: $it")
        }
    }

    job1.join()
    job2.join()
}

Emitted: 0
Emitted: 1
collect 2nd: 1
collect 1st: 1
Emitted: 2
collect 2nd: 2
collect 1st: 2
Emitted: 3
collect 1st: 3
collect 2nd: 3
Emitted: 4
collect 1st: 4
collect 2nd: 4

Здесь видно, что flow эмиттит значения один раз, т.е. создаёт только один поток данных для всех подписчиков!!!! Поток шарится между подписчиками

Мы говорили,что холодные потоки, эмиттят занчения только когда есть подписчики, посмотрим , что с горячими потоками
Уберём первого подписчика и оставим только второго

suspend fun main(){

    val flow = MutableSharedFlow<Int>()

    coroutineScope.launch {
        repeat(20){
            println("Emitted: $it")
            flow.emit(it)
            delay(1000)
        }
    }

//    val job1 = coroutineScope.launch {
//        flow.collect {
//            println("collect 1st: $it")
//        }
//    }

    delay(5000)
    val job2 = coroutineScope.launch {
        flow.collect {
            println("collect 2nd: $it")
        }
    }

    //job1.join()
    job2.join()
}


Emitted: 0
Emitted: 1
Emitted: 2
Emitted: 3
Emitted: 4
Emitted: 5
Emitted: 6
collect 2nd: 6
Emitted: 7
collect 2nd: 7
Emitted: 8
collect 2nd: 8
 Впервые подписваемся через 5 сек, но данные эмитятся сразу, не смотря на то, что нет подписчиков и подписчик начинает получать значения с 6
 
В отличии от холодных, горячий flow продолжает эмиттитьь данные, даже если подписчику они не нужны, например при вызове first

Следующая особенность. Холодный поток заканчивает свою работы после последнего эмитта, горячий поток не завершается если эмиттов больше нет. Он не завершается никогда!!!

Таким образом hot flow имеет следующие особенности

1) Эмиттит значения не зависимо от наличия подписчиков
2) Подписчики получают одни и те же элементы - ОДИН поток данных
3) Когда подписчикам больше не нужны данные, flow продолжает работать
4) Когда в потоке больше нет данных, flow не завершается никогда

#15.10 Практика MutableSharedFlow

Рассмотрим CryptoActivity 
- уберём бесконечное обновление данных и добавим кнопку Refresh
- удаляем бесконечный цикл

fun getCurrencyList() = flow<List<Currency>> {
        emit(currencyList.toList())
        while (true) {
            delay(3000)
            generateCurrencyList()
            emit(currencyList.toList())
            delay(3000)
        }
    }
	
	меняем на
  fun getCurrencyList() = flow<List<Currency>> {       
            delay(3000)
            generateCurrencyList()
            emit(currencyList.toList())   
    }
	
	Теперь обновление нужно повестить на кнопку Refresh
	Один из способов создать SharedFlow со списком валют
	
	object CryptoRepository {

    ...........

    val currencySharedFlow = MutableSharedFlow<List<Currency>>()
    suspend fun loadData(){
            delay(3000)
            generateCurrencyList()
            currencySharedFlow.emit(currencyList.toList())
    }

    private fun generateCurrencyList()
	..............
	}
	
	Теперь loadData надо вызывать дважды в ViewModel
	
	
	class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository
    
    init {
        viewModelScope.launch { 
            repository.loadData()
        }
    }

    val state: Flow<State> = repository.currencySharedFlow
        .filter { it.isNotEmpty() }
        .map { State.Content(it) as State }
        .onStart {
            emit(State.Loading)
        }

    fun refreshList() {
        viewModelScope.launch {
            repository.loadData()
        }
    }

}

Всё работает, но есть проблемы
1) Мы наружу выдаём объект SharedMutableFlow. Так в него можно заэмитить данные

Исправим:
Изменяемый объект делаем приватным, а неизменяемый выносим наружу как с LiveData

 private val _currencySharedFlow = MutableSharedFlow<List<Currency>>()
    val currencySharedFlow : SharedFlow<List<Currency>> = _currencySharedFlow
    suspend fun loadData(){
            delay(3000)
            generateCurrencyList()
        _currencySharedFlow.emit(currencyList.toList())
    }
	
Проблема казалось бы решена, но это не совсем так, снаружи снова можно привести SharedFlow к MutableSharedFlow (явный каст через as) и заэмиттить данные 
Решение следующее

val currencySharedFlow : SharedFlow<List<Currency>> = _currencySharedFlow.asSharedFlow() - read only flow При преобразовании приложение упадёт

2) Хотелось бы,чтобы загрузка стартовала сразу без вызова метода init во ViewModel. Для этого можно использовать другой подход, который похож на Subjecy в RxJava
Вернём реализацию с холодным flow

Создадим 

private val refreshSharedFlow = MutableSharedFlow<Unit>() - тип не имеет значения, поэтому Unit

В холодном потоке подпишемся на горячий поток обновлений. Т.к. горячий никогда не заканчивается, то и холодный не будет заканчиваться


fun getCurrencyList() = flow<List<Currency>> {
            delay(3000)
            generateCurrencyList()
            emit(currencyList.toList())

            refreshSharedFlow.collect{
                delay(3000)
                generateCurrencyList()
                emit(currencyList.toList())
            }
    }
И добавим функцию обновления списка

suspend fun refreshList(){
       refreshSharedFlow.emit(Unit)
}
	
Итого
object CryptoRepository {

    ......
    private val refreshSharedFlow = MutableSharedFlow<Unit>()
    fun getCurrencyList() = flow<List<Currency>> {
            delay(3000)
            generateCurrencyList()
            emit(currencyList.toList())

            refreshSharedFlow.collect{
                delay(3000)
                generateCurrencyList()
                emit(currencyList.toList())
            }
    }

    suspend fun refreshList(){
        refreshSharedFlow.emit(Unit)
    }

    private fun generateCurrencyList() ...
}

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    val state: Flow<State> = repository.getCurrencyList()
        .filter { it.isNotEmpty() }
        .map { State.Content(it) as State }
        .onStart {
            emit(State.Loading)
        }

    fun refreshList() {
        viewModelScope.launch {
            repository.refreshList()
        }
    }

}	

Рассмотрели два способа обновления
1) SharedMutableFlow как список, в который эмитим обновлённый список
2) Когда SharedFlow используем как конрейнер для эвента обновления списка. 
И в холодном потоке на него подписываемся и делаем определённые действия, в данном случае обновляем список


#15.11 Промежуточные и кастомные операторы

Какие сейчас есть проблемы в приложении:
1) Нет прогресс-бара при обновлении списка
2) Кликать можем сколько угодно по кнопке Refresh - нужно дизейблить в момент загрузки

Обновим состояние кнопки в активити

 private fun observeData() {
        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.state
                    .collect {
                        when (it) {
                            is State.Initial -> {
                                binding.progressBarLoading.isVisible = false
                                binding.refreshButton.isEnabled = false
                            }

                            is State.Loading -> {
                                binding.progressBarLoading.isVisible = true
                                binding.refreshButton.isEnabled = false
                            }

                            is State.Content -> {
                                binding.progressBarLoading.isVisible = false
                                adapter.submitList(it.currencyList)
                                binding.refreshButton.isEnabled = true
                            }
                        }
                    }
            }
        }
    }
	
	Теперь нужно чтобы при повторной загрузке State менялся на Loading

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    val state: Flow<State> = repository.getCurrencyList()
        .filter { it.isNotEmpty() }
        .map { State.Content(it) as State }
        .onStart {
            emit(State.Loading)
        }

    fun refreshList() {
        viewModelScope.launch {
            repository.refreshList()
        }
    }

}

При клике на кнопку, должно прилетать состояние Loading, но у нас flow холодный, в него не заэмиттить значение, поэтому создаём новый flow внутрь ViewModel, 
который будет содержать состояние загрузки

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository
    
    private val loadingFlow = MutableSharedFlow<State>()

   .....

    fun refreshList() {
        
        viewModelScope.launch {
            loadingFlow.emit(State.Loading)
            repository.refreshList()
        }
    }
}

Теперь у нас два flow меняющих состояние, тепепрь в активити нужно реагировать на оба. Для этого используем merge. Но merge не функция Flow 

Поэтому напишем расширение 
 fun <T> Flow<T>.mergeWith(otherFlow : Flow<T>) : Flow<T>{
        return merge(this,otherFlow)
    }
	
Итог:

class CryptoViewModel : ViewModel() {

    private val repository = CryptoRepository

    private val loadingFlow = MutableSharedFlow<State>()

    val state: Flow<State> = repository.getCurrencyList()
        .filter { it.isNotEmpty() }
        .map { State.Content(it) as State }
        .onStart {

            emit(State.Loading)
        }.mergeWith(loadingFlow)

    fun <T> Flow<T>.mergeWith(otherFlow : Flow<T>) : Flow<T>{
        return merge(this,otherFlow)
    }

    fun refreshList() {

        viewModelScope.launch {
            loadingFlow.emit(State.Loading)
            repository.refreshList()
        }
    }

}


https://flowmarbles.com/ - операторы Kotlin Flow
