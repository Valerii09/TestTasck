package com.example.testtasck

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

private const val FILE_PICKER_REQUEST_CODE = 123

private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
private lateinit var sharedPrefs: SharedPreferences

class MainActivity : AppCompatActivity() {

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Обработка изменения ориентации здесь, если необходимо.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Установка темы обратно на основную тему после отображения сплэш-экрана
        setTheme(R.style.SplashTheme)

        // Инициализация Firebase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(1) // Устанавливает интервал обновления данных (в секундах)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Пытаемся получить сохраненную ссылку из SharedPreferences
        val savedUrl = sharedPrefs.getString("savedUrl", "")

        if (savedUrl != null) {
            if (savedUrl.isNotEmpty()) {
                // Если сохраненная ссылка есть, используем ее
                loadWebViewWithUrl(savedUrl)

                // Проверяем условия для сохраненной ссылки
                if (isGoogleDevice(savedUrl) || isEmulator(savedUrl)) {
                    // Открываем заглушку (AnotherActivity)
                    openAnotherActivity()
                }
            } else {
                // Если сохраненной ссылки нет, выполняем запрос на получение данных из Firebase Remote Config
                fetchAndActivateRemoteConfig()
            }
        }

        setupUI()
    }

    private fun setupUI() {
        // Остальная часть вашего кода для настройки интерфейса пользователя
    }

    private fun loadWebViewWithUrl(url: String) {
        val webView = findViewById<WebView>(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()
        webView.loadUrl(url)
    }

    private fun openAnotherActivity() {
        val intent = Intent(this@MainActivity, AnotherActivity::class.java)
        startActivity(intent)
        Log.d("MainActivity", "Нажата кнопка для перехода на AnotherActivity")
    }

    private fun fetchAndActivateRemoteConfig() {
        try {
            // Выполняем запрос на получение данных
            mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val url = mFirebaseRemoteConfig.getString("url")

                        // Сохраняем полученную ссылку в SharedPreferences
                        with(sharedPrefs.edit()) {
                            putString("savedUrl", url)
                            apply()
                        }

                        // Проверяем условия
                        if (url.isEmpty() || isGoogleDevice(url) || isEmulator(url)) {
                            // Открываем заглушку (AnotherActivity)
                            openAnotherActivity()
                        } else {
                            // Открываем страницу с полученной ссылкой
                            loadWebViewWithUrl(url)
                        }
                    }
                }
        } catch (e: Exception) {
            showErrorScreen()
            Log.e("MainActivity", "Ошибка при обработке Firebase Remote Config: ${e.message}")
        }
    }

    private fun showErrorScreen() {
        setContentView(R.layout.activity_no_internet)
        val errorMessageTextView = findViewById<TextView>(R.id.textViewNoInternet)
        errorMessageTextView.setOnClickListener {
            val intent = Intent(this@MainActivity, NoInternetActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isGoogleDevice(url: String): Boolean {
        Log.d("MainActivity", "isGoogle:")
        return url.contains("google")
    }

    private fun isEmulator(url: String): Boolean {
        Log.d("MainActivity", "isEmulator:")
        return !url.isNotEmpty() || Build.FINGERPRINT.startsWith("generic")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnectedOrConnecting == true
    }

    fun clearSavedUrl() {
        // Получаем доступ к SharedPreferences
        val sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Создаем объект SharedPreferences.Editor для редактирования
        val editor = sharedPrefs.edit()

        // Удаляем сохраненную ссылку по ее ключу ("savedUrl" в данном случае)
        editor.remove("savedUrl")

        // Применяем изменения
        editor.apply()

        // Очищаем текст в EditText
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        searchEditText.text.clear()

        // Оповещение пользователя о удалении сохраненной ссылки (по вашему желанию)
        Toast.makeText(this, "Сохраненная ссылка удалена", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // код для работы с выбранным JSON файлом (uri)
            }
        }
    }
}
