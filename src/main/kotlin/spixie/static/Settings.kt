package spixie.static

import com.google.gson.Gson
import java.io.File
import java.nio.file.Files

val Settings = Props.load()

data class Props(
        val ffmpeg:String
){
    companion object {
        fun load(): Props {
            val settingsFile = File("settings.json")
            if(!settingsFile.exists()){
                Files.write(settingsFile.toPath(), this::class.java.getResourceAsStream("/settings.json").readBytes())
            }
            return Gson().fromJson(settingsFile.bufferedReader().use { it.readText() }, Props::class.java)
        }
    }
}