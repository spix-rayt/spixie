package spixie

import kotlinx.coroutines.*
import uk.co.electronstudio.sdl2gdx.SDL2ControllerManager
import java.lang.Exception

@ObsoleteCoroutinesApi
object Gamepad {
    val gamepadCoroutineDispatcher = newSingleThreadContext("gamepad")

    fun start() {
        GlobalScope.launch(gamepadCoroutineDispatcher) {
            val controllerManager = SDL2ControllerManager(SDL2ControllerManager.InputPreference.RAW_INPUT)
            while (true) {
                try {
                    controllerManager.pollState()
                    controllerManager.controllers.forEach { controller ->
                        if(controller.getButton(11)) {
                            withContext(Core.renderManager.renderCoroutineDispatcher) {
                                Core.renderManager.requestRender()
                            }
                        }
                        if(controller.getButton(12)) {
                            withContext(Core.renderManager.renderCoroutineDispatcher) {
                                Core.renderManager.requestRender()
                            }
                        }
                        val rotate1 = controller.getAxis(0)
                        if(rotate1 !in -0.02..0.02) {
                            withContext(Core.renderManager.renderCoroutineDispatcher) {
                            }
                        }
                    }
                    delay(1)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }
}