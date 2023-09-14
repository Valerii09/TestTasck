package com.example.testtasck

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class MainActivity : AppCompatActivity() {

    private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)

        // Проверяем доступность сети
        if (isNetworkAvailable()) {
            initializeFirebaseRemoteConfig()
        } else {
            // Интернет недоступен, переходим к экрану без интернета
            startActivity(Intent(this, NoInternetActivity::class.java))
            finish()
        }

        // Инициализация SharedPreferences
        sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        val savedUrl = sharedPrefs.getString("savedUrl", "")

        if (savedUrl.isNullOrEmpty()) {
            // Если сохраненная ссылка отсутствует или пустая, открываем AnotherActivity
            openAnotherActivity()
        } else {
            processRemoteConfigData()
        }
    }

    private fun initializeFirebaseRemoteConfig() {
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(1)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
    }

    private fun processRemoteConfigData() {
        try {
            // Выполняем запрос на получение данных
            mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val url = mFirebaseRemoteConfig.getString("url")

                        if (url.isNotEmpty() || isGoogleDevice(url) || isEmulator(url)) {
                            openAnotherActivity()

                        } else {
                            with(sharedPrefs.edit()) {
                                putString("savedUrl", url)
                                apply()
                            }
                            openAnotherActivity()
                        }
                    } else {
                        handleFetchFailure()
                    }
                }
        } catch (e: Exception) {
            handleFetchFailure()
        }
    }

    private fun openAnotherActivity() {
        val intent = Intent(this@MainActivity, AnotherActivity::class.java)
        startActivity(intent)
        Log.d("MainActivity", "Первый запуск приложения, открываем AnotherActivity")
    }

    private fun handleFetchFailure() {
        showToast("Failed to fetch data. Please check your network connection.")
        showErrorScreenWithMessage("Failed to fetch data. Please check your network connection.")
        Log.e("MainActivity", "Error while fetching data from Firebase Remote Config")
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnectedOrConnecting == true
    }

    private fun showErrorScreenWithMessage(errorMessage: String) {
        val errorIntent = Intent(this, NoInternetActivity::class.java)
        errorIntent.putExtra("message", errorMessage)
        startActivity(errorIntent)
    }

    private fun isGoogleDevice(url: String): Boolean {
        Log.d("MainActivity", "isGoogleDevice:")
        return url.contains("google")
    }

    private fun isEmulator(url: String): Boolean {
        Log.d("MainActivity", "isEmulator:")
        return !url.isNotEmpty() || Build.FINGERPRINT.startsWith("generic")
    }

}
