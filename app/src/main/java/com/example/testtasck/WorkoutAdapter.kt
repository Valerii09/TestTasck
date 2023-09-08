package com.example.testtasck

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class WorkoutAdapter(private val workoutList: List<Workout>) : RecyclerView.Adapter<WorkoutAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateText: TextView = itemView.findViewById(R.id.dateText)
        val exerciseText: TextView = itemView.findViewById(R.id.exerciseText)
        val durationText: TextView = itemView.findViewById(R.id.durationText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.workout_item, parent, false)
        return ViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentItem = workoutList[position]
        holder.dateText.text = currentItem.date
        holder.exerciseText.text = currentItem.exercise
        holder.durationText.text = currentItem.duration
    }

    override fun getItemCount() = workoutList.size
}
