package spixie

import com.google.gson.Gson
import java.io.File

data class Settings(
        val ffmpeg:String
){
    companion object {
        fun default():Settings{
            return Settings(
                    "ffmpeg"
            )
        }

        fun load(): Settings{
            val settingsFile = File("settings.json")
            if(settingsFile.exists()){
                try{
                    return Gson().fromJson<Settings>(settingsFile.bufferedReader().use { it.readText() }, Settings::class.java)
                }catch (e:Exception){
                    return default()
                }
            }else{
                return default()
            }
        }
    }
}