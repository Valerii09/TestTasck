package gofacts.sporhosee

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class WebViewActivity : AppCompatActivity() {

    // Переменные для работы с Firebase Remote Config и WebView
    private lateinit var mFirebaseRemoteConfig: FirebaseRemoteConfig
    private lateinit var sharedPrefs: SharedPreferences
    private lateinit var webView: ScrollableWebView

    // Переменные для работы с выбором файлов
    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mCapturedImageURI: Uri? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Устанавливаем макет для активности
        setContentView(R.layout.activity_webview)
        webView = findViewById(R.id.webView)
        sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)

        // Получаем URL из Intent
        val url = intent.extras?.getString("url").toString()

        // Настраиваем WebView
        webView.webViewClient = WebViewClient()
        webView.webChromeClient = ChromeClient()
        val webSettings = webView.settings
        webSettings.apply {
            javaScriptEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            domStorageEnabled = true
            databaseEnabled = true
            setSupportZoom(false)
            allowFileAccess = true
            allowContentAccess = true
        }

        // Если есть сохраненное состояние WebView, восстанавливаем его; иначе загружаем URL
        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl(url)
        }

        // Настройки для управления куками
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)

        // Настройка обработки кнопки "назад" в WebView
        webView.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
                webView.goBack()
                true // Заблокировать обработку события кнопки "назад"
            } else {
                false // Разрешить обработку события кнопки "назад" по умолчанию
            }
        }

        // Инициализация Firebase Remote Config
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        val configSettings =
            FirebaseRemoteConfigSettings.Builder().setMinimumFetchIntervalInSeconds(1).build()
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings)

        // Получаем сохраненный URL или загружаем локальную HTML-страницу
        val savedUrl = sharedPrefs.getString("savedUrl", "")
        if (savedUrl != null && savedUrl.isNotEmpty()) {
            webView.loadUrl(savedUrl)
            Log.d("WebViewActivity", "Открываем URL в WebView: $savedUrl")
        } else {
            webView.loadUrl("file:///android_asset/local_page.html")
            Log.d("WebViewActivity", "Загружаем локальную HTML-страницу")
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Сохраняем состояние WebView при уничтожении активности
        webView.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        // Восстанавливаем состояние WebView после пересоздания активности
        webView.restoreState(savedInstanceState)
    }

    // Обработка нажатия кнопки "назад"
    @Suppress("DEPRECATION")
    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack() // Вернуться по истории браузера, если возможно
        } else {
            // Кнопка "Назад" заблокирована
            Toast.makeText(this, "Нельзя вернуться назад", Toast.LENGTH_SHORT).show()
        }
    }

    // Обработка результатов выбора файлов
    @Suppress("DEPRECATION")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
            super.onActivityResult(requestCode, resultCode, data)
            return
        }
        var results: Array<Uri>? = null
        if (resultCode == RESULT_OK) {
            if (data == null) {
                if (mCameraPhotoPath != null) {
                    results = arrayOf(Uri.parse(mCameraPhotoPath))
                }
            } else {
                val dataString = data.dataString
                if (dataString != null) {
                    results = arrayOf(Uri.parse(dataString))
                }
            }
        }
        mFilePathCallback!!.onReceiveValue(results)
        mFilePathCallback = null
    }

    // Внутренный класс для обработки выбора файлов
    @Suppress("DEPRECATION")
    inner class ChromeClient : WebChromeClient() {
        override fun onShowFileChooser(
            view: WebView, filePath: ValueCallback<Array<Uri>>, fileChooserParams: FileChooserParams
        ): Boolean {
            if (mFilePathCallback != null) {
                mFilePathCallback!!.onReceiveValue(null)
            }
            mFilePathCallback = filePath
            var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent!!.resolveActivity(packageManager) != null) {
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                } catch (ex: IOException) {
                    Log.e("ErrorCreatingFile", "Unable to create Image File", ex)
                }
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.absolutePath
                    takePictureIntent.putExtra(
                        MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile)
                    )
                } else {
                    takePictureIntent = null
                }
            }
            val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
            contentSelectionIntent.type = "image/*"
            val intentArray: Array<Intent?>
            intentArray = takePictureIntent?.let { arrayOf(it) } ?: arrayOfNulls(0)
            val chooserIntent = Intent(Intent.ACTION_CHOOSER)
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE)
            return true
        }

        fun openFileChooser(uploadMsg: ValueCallback<Uri?>?) {
            mUploadMessage = uploadMsg
            val imageStorageDir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), "AndroidExampleFolder"
            )
            if (!imageStorageDir.exists()) {
                imageStorageDir.mkdirs()
            }
            val file = File(
                imageStorageDir.toString() + File.separator + "IMG_" + System.currentTimeMillis()
                    .toString() + ".jpg"
            )
            mCapturedImageURI = Uri.fromFile(file)
            val captureIntent = Intent(
                MediaStore.ACTION_IMAGE_CAPTURE
            )
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "image/*"
            val chooserIntent = Intent.createChooser(i, "Image Chooser")
            chooserIntent.putExtra(
                Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(captureIntent)
            )
            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
        }

        fun openFileChooser(uploadMsg: ValueCallback<Uri?>?, capture: String?) {
            openFileChooser(uploadMsg)
        }
    }

    companion object {
        private const val INPUT_FILE_REQUEST_CODE = 1
        private const val FILECHOOSER_RESULTCODE = 1
    }

    @SuppressLint("SimpleDateFormat")
    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }
}
