package spixie.arrangement

import spixie.visualEditor.Module

class SerializedProject {

    var bpm: Double = 160.0

    var offset: Double = 0.0

    lateinit var module: Module.SerializedModule

    lateinit var graphs: ArrayList<ArrangementGraphsContainer>
}