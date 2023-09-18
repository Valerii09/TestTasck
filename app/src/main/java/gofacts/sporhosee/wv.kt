package gofacts.sporhosee

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Parcelable
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.webkit.*
import android.widget.Toast
import androidx.activity.ComponentActivity
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class WV:ComponentActivity(){
    private var mUploadMessage: ValueCallback<Uri?>? = null
    private var mCapturedImageURI: Uri? = null
    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null
    private var mCameraPhotoPath: String? = null
    var webView: WebView? = null

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp =
            SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "JPEG_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_PICTURES
        )
        return File.createTempFile(
            imageFileName,  /* prefix */
            ".jpg",  /* suffix */
            storageDir /* directory */
        )
    }

    override fun onSaveInstanceState(outState: Bundle) { // Here You have to save count value
        super.onSaveInstanceState(outState)
        webView?.saveState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)

        webView?.restoreState(savedInstanceState)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.wv)

        val url = intent.extras?.getString("url").toString()

        val sharedPrefs = getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val savedUrl = sharedPrefs.getString("savedUrl", "")

        Log.d(
            "WV", "saved_url: $savedUrl"
        )

        webView = findViewById<View>(R.id.webView) as WebView
        webView!!.webViewClient = WebViewClient()
        webView!!.webChromeClient = ChromeClient()
        val webSettings = webView!!.settings
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
        if (savedInstanceState != null) {
            webView!!.restoreState(savedInstanceState)
        } else {
            // Загружаем сохраненную ссылку, если она есть
            if (!savedUrl.isNullOrEmpty()) {
                webView!!.loadUrl(savedUrl)
            }
        }

        webView!!.settings.apply {
            domStorageEnabled = true
            javaScriptCanOpenWindowsAutomatically = true
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)

        Log.d(
            "WV", "прошла условие Значение URL из Remote Config: $savedUrl"
        )

    }
    override fun onBackPressed() {
        if (webView!!.canGoBack()) {
            webView!!.goBack()
        }
    }




    inner class ChromeClient : WebChromeClient() {
        // For Android 5.0
        override fun onShowFileChooser(
            view: WebView,
            filePath: ValueCallback<Array<Uri>>,
            fileChooserParams: FileChooserParams
        ): Boolean {
            // Double check that we don't have any existing callbacks
            if (mFilePathCallback != null) {
                mFilePathCallback!!.onReceiveValue(null)
            }
            mFilePathCallback = filePath
            var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            if (takePictureIntent!!.resolveActivity(packageManager) != null) {
                // Create the File where the photo should go
                var photoFile: File? = null
                try {
                    photoFile = createImageFile()
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath)
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e("ErrorCreatingFile", "Unable to create Image File", ex)
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:" + photoFile.absolutePath
                    takePictureIntent.putExtra(
                        MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile)
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

        // openFileChooser for Android 3.0+
        // openFileChooser for Android < 3.0
        @JvmOverloads
        fun openFileChooser(uploadMsg: ValueCallback<Uri?>?, acceptType: String? = "") {
            mUploadMessage = uploadMsg
            // Create AndroidExampleFolder at sdcard
            // Create AndroidExampleFolder at sdcard
            val imageStorageDir = File(
                Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES
                ), "AndroidExampleFolder"
            )
            if (!imageStorageDir.exists()) {
                // Create AndroidExampleFolder at sdcard
                imageStorageDir.mkdirs()
            }

            // Create camera captured image file path and name
            val file = File(
                imageStorageDir.toString() + File.separator + "IMG_"
                        + System.currentTimeMillis().toString() + ".jpg"
            )
            mCapturedImageURI = Uri.fromFile(file)

            // Camera capture image intent
            val captureIntent = Intent(
                MediaStore.ACTION_IMAGE_CAPTURE
            )
            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCapturedImageURI)
            val i = Intent(Intent.ACTION_GET_CONTENT)
            i.addCategory(Intent.CATEGORY_OPENABLE)
            i.type = "image/*"

            // Create file chooser intent
            val chooserIntent = Intent.createChooser(i, "Image Chooser")

            // Set camera intent to file chooser
            chooserIntent.putExtra(
                Intent.EXTRA_INITIAL_INTENTS, arrayOf<Parcelable>(captureIntent)
            )

            // On select image call onActivityResult method of activity
            startActivityForResult(chooserIntent, FILECHOOSER_RESULTCODE)
        }

        //openFileChooser for other Android versions
        fun openFileChooser(
            uploadMsg: ValueCallback<Uri?>?,
            acceptType: String?,
            capture: String?
        ) {
            openFileChooser(uploadMsg, acceptType)
        }
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (requestCode != INPUT_FILE_REQUEST_CODE || mFilePathCallback == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }
            var results: Array<Uri>? = null

            // Check that the response is a good one
            if (resultCode == RESULT_OK) {
                if (data == null) {
                    // If there is not data, then we may have taken a photo
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
        } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.KITKAT) {
            if (requestCode != FILECHOOSER_RESULTCODE || mUploadMessage == null) {
                super.onActivityResult(requestCode, resultCode, data)
                return
            }
            if (requestCode == FILECHOOSER_RESULTCODE) {
                if (null == mUploadMessage) {
                    return
                }
                var result: Uri? = null
                try {
                    result = if (resultCode != RESULT_OK) {
                        null
                    } else {

                        // retrieve from the private variable if the intent is null
                        if (data == null) mCapturedImageURI else data.data
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        applicationContext, "activity :$e",
                        Toast.LENGTH_LONG
                    ).show()
                }
                mUploadMessage!!.onReceiveValue(result)
                mUploadMessage = null
            }
        }
        return
    }

    companion object {
        private const val INPUT_FILE_REQUEST_CODE = 1
        private const val FILECHOOSER_RESULTCODE = 1
    }

}
