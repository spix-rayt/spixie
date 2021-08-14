package spixie

import kotlinx.coroutines.*
import uk.co.electronstudio.sdl2gdx.SDL2ControllerManager
import java.lang.Exception
import java.util.concurrent.Executors

object Gamepad {
    val gamepadCoroutineDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    init {
        GlobalScope.launch(gamepadCoroutineDispatcher) {
            val controllerManager = SDL2ControllerManager(SDL2ControllerManager.InputPreference.RAW_INPUT)
            while (true) {
                controllerManager.pollState()
                controllerManager.controllers.forEach { controller ->
                    if(controller.getButton(11)) {
                        renderManager.requestRender()
                    }
                    if(controller.getButton(12)) {
                        renderManager.requestRender()
                    }
                    val rotate1 = controller.getAxis(0)
                    if(rotate1 !in -0.02..0.02) {
                        renderManager.requestRender()
                    }
                }
                delay(1)
            }
        }
    }
}