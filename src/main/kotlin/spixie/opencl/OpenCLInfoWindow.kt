package spixie.opencl

import com.jogamp.opencl.JoclVersion
import javafx.scene.Scene
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.Window

class OpenCLInfoWindow(owner: Window): Stage() {
    init {
        val webView = WebView()
        webView.engine.loadContent(JoclVersion.getInstance().getOpenCLHtmlInfo(null).toString())
        scene = Scene(webView)
        initOwner(owner)
        showAndWait()
    }
}