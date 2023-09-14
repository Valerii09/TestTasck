package com.example.testtasck

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings

class WebViewActivity : AppCompatActivity() {

    private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var webView: ScrollableWebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webview)
        webView = findViewById(R.id.webView)
        sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Получаем экземпляр CookieManager для управления куками
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Находим элементы интерфейса
        val searchEditText = findViewById<EditText>(R.id.searchEditText)


        // Настройка WebView
        val webSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webSettings.databaseEnabled = true
        webSettings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        webSettings.setSupportZoom(false)
        webSettings.allowFileAccess = true
        webSettings.allowContentAccess = true
        webSettings.loadWithOverviewMode = true
        webSettings.useWideViewPort = true

        webView.setOnKeyListener { _, keyCode, event ->
            // Обработка кнопки "назад" в WebView
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                webView.goBack()
                true // Заблокировать обработку события кнопки "назад"
            } else {
                false // Разрешить обработку события кнопки "назад" по умолчанию
            }
        }

        webView.webViewClient = WebViewClient()

        // Инициализация Firebase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(1)
            .build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        // Обработка нажатия кнопки поиска
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

        // Получаем URL из Intent или SharedPreferences
        val savedUrl = sharedPrefs.getString("savedUrl", "")

        if (savedUrl != null && savedUrl.isNotEmpty()) {
            // Устанавливаем URL в WebView
            webView.loadUrl(savedUrl)
            Log.d("WebViewActivity", "Открываем URL в WebView: $savedUrl")
        } else {
            // Загружаем локальную HTML-страницу
            webView.loadUrl("file:///android_asset/local_page.html")
            Log.d("WebViewActivity", "Загружаем локальную HTML-страницу")
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Companion.FILE_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            data?.data?.let { uri ->
                // код для работы с выбранным JSON файлом (uri)
            }
        }
    }

    companion object {
        private const val FILE_PICKER_REQUEST_CODE = 123
    }

    // Функция для удаления сохраненной ссылки из SharedPreferences
    fun clearSavedUrl() {
        val savedUrl = sharedPrefs.getString("savedUrl", "")

        if (savedUrl.isNullOrEmpty()) {
            // Локальная ссылка пуста или отсутствует
            Log.d("MainActivity", "Локальная ссылка пуста или отсутствует.")
            Toast.makeText(this, "Локальная ссылка отсутствует.", Toast.LENGTH_SHORT).show()
        } else {
            // Удаляем сохраненную ссылку
            val editor = sharedPrefs.edit()
            editor.remove("savedUrl")
            editor.apply()

            // Очищаем текст в EditText
            val searchEditText = findViewById<EditText>(R.id.searchEditText)
            searchEditText.text.clear()

            // Оповещение пользователя о удалении сохраненной ссылки
            Toast.makeText(this, "Сохраненная ссылка удалена", Toast.LENGTH_SHORT).show()
        }
    }

}
