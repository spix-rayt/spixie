package spixie

class SerialWorker(val work: () -> Unit) {
    var thread = Thread(Runnable {  })
    private @Volatile var restart = false

    fun run(){
        if(thread.isAlive){
            restart = true
        }else{
            thread = Thread(Runnable {
                do {
                    restart = false
                    work()
                } while (restart)
            })
            thread.start()
        }
    }
}