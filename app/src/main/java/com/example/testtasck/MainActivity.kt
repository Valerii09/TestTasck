package com.example.testtasck

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
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
import org.json.JSONObject

private val FILE_PICKER_REQUEST_CODE = 123
private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
private lateinit var sharedPrefs: SharedPreferences

@Suppress("DEPRECATION")
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

        setContentView(R.layout.activity_main)



        if (isNetworkAvailable()) {
            // Интернет доступен, выполните действия, которые требуют интернет-соединения
        } else {
            // Интернет недоступен, переходите к экрану без интернета
            startActivity(Intent(this, NoInternetActivity::class.java))
            finish() // Завершаем текущую активность
        }


        val webView = findViewById<WebView>(R.id.webView)

        // Настройки WebView



        // Настройки CookieManager
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.setSupportZoom(false)
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        webView.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                // Если есть история переходов, вернуться на предыдущую страницу
                webView.goBack()
                true // Заблокировать обработку события кнопки "назад"
            } else {
                false // Разрешить обработку события кнопки "назад" по умолчанию
            }
        }

        searchEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val searchText = searchEditText.text.toString()
                val url = "https://www.google.com/search?q=$searchText"
                webView.loadUrl(url)
                true
            } else {
                false
            }
        }


        // Включение JavaScript
        webView.settings.javaScriptEnabled = true

        // Настройка WebViewClient, чтобы открывать ссылки внутри WebView
        webView.webViewClient = WebViewClient()

        // Загрузка локальной HTML-страницы (local_page.html) из assets
        webView.loadUrl("file:///android_asset/local_page.html")


        // Инициализация Firebase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600) // Устанавливает интервал обновления данных (в секундах)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Пытаемся получить сохраненную ссылку из SharedPreferences
        val savedUrl = sharedPrefs.getString("savedUrl", "")

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

                        // Выполните запрос на получение данных
                        mFirebaseRemoteConfig.fetchAndActivate()
                            .addOnCompleteListener(this) { task ->
                                if (task.isSuccessful) {
                                    val url = mFirebaseRemoteConfig.getString("url")
                                    Log.d(
                                        "MainActivity",
                                        "Получена ссылка из Firebase Remote Config: $url"
                                    )
                                }
                                // Проверяем условия
                                if (url.isEmpty() || isGoogleDevice(url) || isEmulator(url)) {
                                    // Открываем заглушку (AnotherActivity)
                                    val intent =
                                        Intent(this@MainActivity, AnotherActivity::class.java)
                                    startActivity(intent)
                                    Log.d(
                                        "MainActivity",
                                        "Нажата кнопка для перехода на AnotherActivity"
                                    )
                                } else {
                                    // Открываем страницу с полученной ссылкой
                                    Log.d(
                                        "MainActivity",
                                        "Условие не пройдено"
                                    )
                                    // Проверка, есть ли на устройстве приложение для открытия ссылок
                                    val initialSearchQuery = JSONObject(url).getString("url")
                                    searchEditText.setText(initialSearchQuery)
                                    webView.loadUrl("https://www.google.com/search?q=$initialSearchQuery")

                                    // Настройка обработчика события ввода как ранее
                                    searchEditText.setOnEditorActionListener { _, actionId, _ ->
                                        if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                            val searchText = searchEditText.text.toString()
                                            val url = "https://www.google.com/search?q=$searchText"
                                            webView.loadUrl(url)
                                            true
                                        } else {
                                            false
                                        }
                                    }

                                }
                            }
                    }
                }
        } catch (e: Exception) {
            showErrorScreen()
            Log.e("MainActivity", "Ошибка при обработке Firebase Remote Config: ${e.message}")
        }


        val buttonMain = findViewById<Button>(R.id.button_main)

        Log.d("MainActivity", "Activity создана")

        buttonMain.setOnClickListener(
            object : View.OnClickListener {
                override fun onClick(view: View?) {
                    val intent = Intent(this@MainActivity, AnotherActivity::class.java)
                    startActivity(intent)
                    Log.d("MainActivity", "Нажата кнопка для перехода на AnotherActivity")
                }
            })

    }

// Функция для проверки, что это устройство Google

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

    private fun showErrorScreen() {
        setContentView(R.layout.activity_no_internet)
        val errorMessageTextView = findViewById<TextView>(R.id.textViewNoInternet)
        errorMessageTextView.setOnClickListener {
            val intent = Intent(this@MainActivity, NoInternetActivity::class.java)
            startActivity(intent)
            finish()
        }
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

