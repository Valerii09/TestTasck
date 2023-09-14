package gofacts.sporhosee

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView

class ScrollableWebView : WebView {
    private var isScrollable = true

    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> isScrollable && super.onTouchEvent(event)
            else -> super.onTouchEvent(event)
        }
    }

}
