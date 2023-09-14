package gofacts.sporhosee
import android.annotation.SuppressLint
import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Button
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


@Suppress("DEPRECATION")
class AnotherActivity : AppCompatActivity() {

    private lateinit var chronometer: Chronometer
    private lateinit var startStopButton: Button
    private lateinit var resetButton: Button
    private var isRunning = false
    private var startTimeMillis: Long = 0
    private var lastTenths = 0

    private lateinit var recyclerView: RecyclerView
    private lateinit var workoutAdapter: WorkoutAdapter
    private val workoutList = mutableListOf<Workout>()

    private lateinit var editTextDate: EditText
    private lateinit var editTextExercise: EditText
    private lateinit var editTextDuration: EditText
    private lateinit var addWorkoutButton: Button

    private val sharedPreferences by lazy {
        getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
    }
    /**
     * Сохраняет последнюю тренировку в SharedPreferences.
     */
    private fun saveLastWorkout(workout: Workout) {
        val gson = Gson()
        val json = gson.toJson(workout)
        sharedPreferences.edit().putString("lastWorkout", json).apply()
    }
    /**
     * Сохраняет список тренировок в SharedPreferences.
     */
    private fun saveWorkoutList() {
        val gson = Gson()
        val json = gson.toJson(workoutList)
        sharedPreferences.edit().putString("workoutList", json).apply()
    }
    /**
     * Удаляет последнюю тренировку из списка тренировок.
     * Если список тренировок не пустой, удаляется последний элемент,
     * и обновляется адаптер для отображения изменений.
     * Затем список тренировок сохраняется в SharedPreferences.
     */
    private fun removeLastWorkout() {
        if (workoutList.isNotEmpty()) {
            workoutList.removeAt(workoutList.size - 1)
            workoutAdapter.notifyItemRemoved(workoutList.size)
            saveWorkoutList()
        }
    }
    /**
     * Загружает список тренировок из SharedPreferences.
     * Если в SharedPreferences есть сохраненный список тренировок,
     * он извлекается, десериализуется с помощью библиотеки Gson,
     * и затем загружается в список тренировок workoutList.
     * После этого вызывается метод notifyDataSetChanged() адаптера,
     * чтобы обновить отображение изменений.
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun loadWorkoutList() {
        val json = sharedPreferences.getString("workoutList", null)
        if (!json.isNullOrBlank()) {
            val gson = Gson()
            val type = object : TypeToken<MutableList<Workout>>() {}.type
            workoutList.clear()
            workoutList.addAll(gson.fromJson(json, type))
            workoutAdapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_another)
        supportActionBar?.hide()

        val deleteLastWorkoutButton = findViewById<Button>(R.id.deleteLastWorkoutButton)
        deleteLastWorkoutButton.setOnClickListener {
            removeLastWorkout()
        }

        chronometer = findViewById(R.id.chronometer)
        recyclerView = findViewById(R.id.recyclerView)

        editTextDate = findViewById(R.id.editTextDate)
        editTextExercise = findViewById(R.id.editTextExercise)
        editTextDuration = findViewById(R.id.editTextDuration)
        addWorkoutButton = findViewById(R.id.addWorkoutButton)

        startStopButton = findViewById(R.id.startStopButton)
        resetButton = findViewById(R.id.resetButton)
        workoutAdapter = WorkoutAdapter(workoutList)
        recyclerView.adapter = workoutAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadWorkoutList() // Загрузить сохраненный список тренировок

        /**
         * Обработчик нажатия на кнопку "Добавить тренировку".
         * Извлекает значения из текстовых полей даты, упражнения и длительности.
         * Если все поля не пустые, создает объект тренировки и добавляет его в список тренировок.
         * Затем сохраняет обновленный список тренировок в SharedPreferences,
         * обновляет адаптер для отображения изменений и сохраняет последнюю тренировку.
         * Очищает текстовые поля после добавления тренировки.
         */
        addWorkoutButton.setOnClickListener {
            val date = editTextDate.text.toString()
            val exercise = editTextExercise.text.toString()
            val duration = editTextDuration.text.toString()

            if (date.isNotBlank() && exercise.isNotBlank() && duration.isNotBlank()) {
                val workout = Workout(date, exercise, duration)
                workoutList.add(workout)
                saveWorkoutList() // Сохранить обновленный список
                workoutAdapter.notifyDataSetChanged()

                val lastWorkout = workoutList.lastOrNull()
                if (lastWorkout != null) {
                    saveLastWorkout(lastWorkout)
                }

                editTextDate.text.clear()
                editTextExercise.text.clear()
                editTextDuration.text.clear()
            }
        }
        chronometer.format = "00:%s"

        // базовое значение секундомера 0
        chronometer.base = SystemClock.elapsedRealtime()

        /**
         * Обработчик нажатия на кнопку "Старт/Стоп".
         * Если секундомер запущен, останавливает его и обновляет состояние кнопки.
         * Если секундомер остановлен, запускает его с текущим временем и обновляет состояние кнопки.
         */
        startStopButton.setOnClickListener {
            if (isRunning) {
                chronometer.stop()
                isRunning = false
                startStopButton.text = "Старт"
            } else {
                if (startTimeMillis == 0L) {
                    startTimeMillis = SystemClock.elapsedRealtime()
                }
                chronometer.base =
                    SystemClock.elapsedRealtime() - (SystemClock.elapsedRealtime() - startTimeMillis)
                chronometer.start()
                isRunning = true
                startStopButton.text = "Стоп"
            }
        }
        resetButton.setOnClickListener {
            chronometer.stop()
            isRunning = false
            chronometer.base = SystemClock.elapsedRealtime()
            startTimeMillis = 0L // Обнуляем начальное время
            lastTenths = 0
            startStopButton.text = "Старт"
        }
        // Обновление разряда с десятыми долями секунд каждые 100 миллисекунд


        // Обновление разряда с десятыми долями секунд каждые 100 миллисекунд
        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                if (isRunning) {
                    val elapsedMillis = SystemClock.elapsedRealtime() - chronometer.base
                    val tenths = (elapsedMillis % 1000)
                    if (tenths != lastTenths.toLong()) {
                        chronometer.text = String.format(
                            "%02d:%02d.%d",
                            elapsedMillis / 60000,
                            (elapsedMillis % 60000) / 1000,
                            tenths
                        )
                        lastTenths = tenths.toInt()
                    }
                }
                handler.postDelayed(this, 100)
            }
        })

        // Пример добавления тренировки:
        val workout = Workout("2023-09-10", "squats", "30 minutes")
        workoutList.add(workout)
        workoutAdapter.notifyDataSetChanged() // Обновить RecyclerView


    }
}


