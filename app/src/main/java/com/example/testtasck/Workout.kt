package com.example.testtasck

import java.io.Serializable

data class Workout(val date: String, val exercise: String, val duration: String) : Serializable
val workout1 = Workout("2023-09-10", "Приседания", "30 минут")
val workout2 = Workout("2023-09-11", "Отжимания", "20 минут")
