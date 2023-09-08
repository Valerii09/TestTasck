package com.example.testtasck

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_another)
        chronometer = findViewById(R.id.chronometer)
        this.recyclerView = findViewById(R.id.recyclerView)


        // Инициализация элементов интерфейса для ввода данных о тренировке
        editTextDate = findViewById(R.id.editTextDate)
        editTextExercise = findViewById(R.id.editTextExercise)
        editTextDuration = findViewById(R.id.editTextDuration)
        addWorkoutButton = findViewById(R.id.addWorkoutButton)

        startStopButton = findViewById(R.id.startStopButton)
        resetButton = findViewById(R.id.resetButton)
        workoutAdapter = WorkoutAdapter(workoutList)
        recyclerView.adapter = workoutAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        addWorkoutButton.setOnClickListener {
            val date = editTextDate.text.toString()
            val exercise = editTextExercise.text.toString()
            val duration = editTextDuration.text.toString()

            if (date.isNotBlank() && exercise.isNotBlank() && duration.isNotBlank()) {
                val workout = Workout(date, exercise, duration)
                workoutList.add(workout)
                workoutAdapter.notifyDataSetChanged()

                // Очистите поля ввода
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
                chronometer.base = SystemClock.elapsedRealtime() - (SystemClock.elapsedRealtime() - startTimeMillis)
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
                        chronometer.text = String.format("%02d:%02d.%d", elapsedMillis / 60000, (elapsedMillis % 60000) / 1000, tenths)
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


