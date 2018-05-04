package spixie.static

import com.google.gson.Gson
import java.io.File

val Settings = Props.load()

data class Props(
        val ffmpeg:String
){
    companion object {
        fun default(): Props {
            return Props(
                    "ffmpeg"
            )
        }

        fun load(): Props {
            val settingsFile = File("settings.json")
            if(settingsFile.exists()){
                try{
                    return Gson().fromJson<Props>(settingsFile.bufferedReader().use { it.readText() }, Settings::class.java)
                }catch (e:Exception){
                    return default()
                }
            }else{
                return default()
            }
        }
    }
}