package spixie.static

import com.google.gson.Gson
import java.io.File

val Settings = Props.load()

data class Props(
        val ffmpeg:String
){
    companion object {
        private fun default(): Props {
            return Props(
                    "ffmpeg"
            )
        }

        fun load(): Props {
            val settingsFile = File("settings.json")
            return if(settingsFile.exists()){
                try{
                    Gson().fromJson<Props>(settingsFile.bufferedReader().use { it.readText() }, Settings::class.java)
                }catch (e:Exception){
                    default()
                }
            }else{
                default()
            }
        }
    }
}