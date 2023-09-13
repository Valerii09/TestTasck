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
import org.json.JSONObject

private const val FILE_PICKER_REQUEST_CODE = 123 // Код запроса для выбора файла

private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig // Инициализация Firebase Remote Config
private lateinit var sharedPrefs: SharedPreferences // Инициализация SharedPreferences

class MainActivity : AppCompatActivity() {

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Обработка изменения ориентации, если необходимо.
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
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.setSupportZoom(false)
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        // Устанавливаем обработчик кнопки "назад" в WebView
        webView.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                // Если есть история переходов, вернуться на предыдущую страницу
                webView.goBack()
                true // Заблокировать обработку события кнопки "назад"
            } else {
                false // Разрешить обработку события кнопки "назад" по умолчанию
            }
        }

        // Устанавливаем обработчик действия при поиске в EditText
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

        // Настройка WebViewClient, чтобы открывать ссылки внутри WebView
        webView.webViewClient = WebViewClient()

        // Загрузка локальной HTML-страницы (local_page.html) из assets
        webView.loadUrl("file:///android_asset/local_page.html")

        // Инициализация Firebase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(1) // Устанавливает интервал обновления данных (в секундах)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        // Инициализация SharedPreferences
        sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Пытаемся получить сохраненную ссылку из SharedPreferences
        val savedUrl = sharedPrefs.getString("savedUrl", "")

        if (savedUrl != null) {
            if (savedUrl.isNotEmpty()) {
                // Если сохраненная ссылка есть, используем ее
                searchEditText.setText(savedUrl)
                webView.loadUrl(savedUrl)

                // Проверяем условия для сохраненной ссылки
                if (isGoogleDevice(savedUrl) || isEmulator(savedUrl)) {
                    // Открываем заглушку (AnotherActivity)
                    val intent = Intent(this@MainActivity, AnotherActivity::class.java)
                    startActivity(intent)
                    Log.d("MainActivity", "Нажата кнопка для перехода на AnotherActivity")
                }
            } else {
                // Если сохраненной ссылки нет, выполняем запрос на получение данных из Firebase Remote Config
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
                                    val intent = Intent(this@MainActivity, AnotherActivity::class.java)
                                    startActivity(intent)
                                    Log.d("MainActivity", "Нажата кнопка для перехода на AnotherActivity")
                                } else {
                                    // Открываем страницу с полученной ссылкой
                                    Log.d("MainActivity", "Условие не пройдено")
                                    // Устанавливаем полученную ссылку в EditText
                                    searchEditText.setText(url)
                                    // Загружаем страницу с этой ссылкой
                                    webView.loadUrl(url)
                                    // Другие действия по обработке ссылки...
                                }
                            }
                        }
                } catch (e: Exception) {
                    showErrorScreen()
                    Log.e("MainActivity", "Ошибка при обработке Firebase Remote Config: ${e.message}")
                }
            }
        }

        val buttonMain = findViewById<Button>(R.id.button_main)

        Log.d("MainActivity", "Activity создана")

        // Устанавливаем обработчик клика на кнопку "Перейти на другую страницу"
        buttonMain.setOnClickListener {
            val intent = Intent(this@MainActivity, AnotherActivity::class.java)
            startActivity(intent)
            Log.d("MainActivity", "Нажата кнопка для перехода на AnotherActivity")
        }

        val buttonClearSavedUrl = findViewById<Button>(R.id.button_clear_saved_url)

        // Устанавливаем обработчик клика на кнопку "Очистить сохраненную ссылку"
        buttonClearSavedUrl.setOnClickListener {
            clearSavedUrl()
        }
    }

    // Функция для проверки, что это устройство Google
    private fun isGoogleDevice(url: String): Boolean {
        Log.d("MainActivity", "isGoogle:")
        return url.contains("google")
    }

    // Функция для проверки, что это эмулятор
    private fun isEmulator(url: String): Boolean {
        Log.d("MainActivity", "isEmulator:")
        return !url.isNotEmpty() || Build.FINGERPRINT.startsWith("generic")
    }

    // Функция для проверки доступности сети
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo?.isConnectedOrConnecting == true
    }

    // Функция для удаления сохраненной ссылки
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

    // Функция для отображения экрана ошибки
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

