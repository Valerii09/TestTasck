package com.example.testtasck

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import org.json.JSONObject

private val FILE_PICKER_REQUEST_CODE = 123
private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
private lateinit var sharedPrefs: SharedPreferences

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val webView = findViewById<WebView>(R.id.webView)
        val searchEditText = findViewById<EditText>(R.id.searchEditText)
        webView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true

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
            }

        val buttonMain = findViewById<Button>(R.id.button_main)

        Log.d("MainActivity", "Activity создана")

        buttonMain.setOnClickListener(object : View.OnClickListener {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // код для работы с выбранным JSON файлом (uri)
            }
        }
    }
}

