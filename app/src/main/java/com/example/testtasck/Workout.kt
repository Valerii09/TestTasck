package com.example.testtasck

import java.io.Serializable

data class Workout(val date: String, val exercise: String, val duration: String) : Serializable
val workout1 = Workout("2023-09-10", "squats", "30 minutes")
val workout2 = Workout("2023-09-11", "Push ups", "20 minutes")
