package spixie.render

import com.jogamp.opencl.JoclVersion
import javafx.scene.Scene
import javafx.scene.web.WebView
import javafx.stage.Stage
import javafx.stage.Window

class OpenCLInfoWindow(owner: Window): Stage() {
    init {
        val webView = WebView()
        webView.engine.loadContent(JoclVersion.getInstance().getOpenCLHtmlInfo(null).toString())
        webView.zoom = 0.8
        scene = Scene(webView)
        initOwner(owner)
        showAndWait()
    }
}