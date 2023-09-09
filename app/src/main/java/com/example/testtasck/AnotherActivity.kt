package com.example.testtasck

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.View
import android.widget.Button
import android.os.SystemClock
import android.widget.Chronometer
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.example.testtasck.WorkoutAdapter



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
    private fun saveLastWorkout(workout: Workout) {
        val gson = Gson()
        val json = gson.toJson(workout)
        sharedPreferences.edit().putString("lastWorkout", json).apply()
    }

    private fun saveWorkoutList() {
        val gson = Gson()
        val json = gson.toJson(workoutList)
        sharedPreferences.edit().putString("workoutList", json).apply()
    }

    private fun removeLastWorkout() {
        if (workoutList.isNotEmpty()) {
            workoutList.removeAt(workoutList.size - 1)
            workoutAdapter.notifyItemRemoved(workoutList.size)
            saveWorkoutList()
        }
    }

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_another)

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
        val workout = Workout("2023-09-10", "Приседания", "30 минут")
        workoutList.add(workout)
        workoutAdapter.notifyDataSetChanged() // Обновить RecyclerView



        val buttonMain = findViewById<Button>(R.id.backButton)

        buttonMain.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View?) {
                val intent = Intent(this@AnotherActivity, MainActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                startActivity(intent)
            }
        })

    }
}


