package com.example.testtasck

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.testtasck.R


class NoInternetActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_no_internet)


        val retryButton = findViewById<Button>(R.id.buttonRetry)

        retryButton.setOnClickListener {
            // Попытка повторного подключения к интернету
            //код для проверки доступности интернета и перехода на главный экран при восстановлении соединения
            if (isInternetAvailable()) {
                val mainIntent = Intent(this, MainActivity::class.java)
                startActivity(mainIntent)
                finish()
            }
        }
    }

    // Функция для проверки доступности интернета
    private fun isInternetAvailable(): Boolean {

         val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
         val networkInfo = connectivityManager.activeNetworkInfo
         return networkInfo != null && networkInfo.isConnected
        return false
    }
}
