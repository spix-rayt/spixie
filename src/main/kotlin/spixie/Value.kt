package spixie

class Value<T>(initial:T) {
    var value:T = initial
        set(value) {
            field = value
            for (onChangedCallback in onChangedCallbacks) {
                onChangedCallback(value)
            }
        }

    var input:Value<T>? = null
        set(value) {
            field?.removeOnChanged(inputOnChangedCallback)
            inputOnChangedCallback = value?.onChanged { newValue -> this.value = calcNewValue(newValue) }
            field = value
            update()
        }
    private var inputOnChangedCallback: ((newValue: T) -> Unit)? = null

    private var onChangedCallbacks = listOf<(newValue:T) -> Unit>()

    fun onChanged(callback: (newValue:T) -> Unit): (newValue: T) -> Unit {
        onChangedCallbacks+=callback
        return callback
    }

    fun removeOnChanged(callback: ((newValue:T) -> Unit)?){
        if(callback!=null){
            onChangedCallbacks-=callback
        }
    }

    fun update(){
        val freezedInput = input
        if(freezedInput != null){
            value = calcNewValue(freezedInput.value)
        }
    }

    //override this
    var calcNewValue:(inputNewValue:T) -> T = { inputNewValue -> inputNewValue }
}