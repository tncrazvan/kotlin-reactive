package dev.razshare.reactive

import java.util.WeakHashMap

typealias Runner = (store:Reactive)->Unit

fun reactive(runner: Runner){
    Reactive(runner)
}

typealias StoreCallback<T> = (value:T)->Unit
typealias StoreUnsubscriber = ()->Unit

class Store<T>(private var value:T) {
    fun get() = value
    fun set(value:T)  {
        this.value = value
        for ((callback) in subscribers){
            callback(value)
        }
    }
    private val subscribers = WeakHashMap<StoreCallback<T>, StoreCallback<T>>()
    fun subscribe(callback:StoreCallback<T>):StoreUnsubscriber{
        subscribers[callback] = callback
        callback(value)
        return { subscribers.remove(callback) }
    }
}

class Reactive (private val run: Runner) {
    private var queued = true
    private val unsubscribers = ArrayList<StoreUnsubscriber>()
    private val map = HashMap<Int, Store<*>>()

    init {
        start()
    }

    fun<T> stores(value:T): Store<T> {
        val id = value.hashCode()
        if(map.containsKey(id)){
            return map[id] as Store<T>
        }

        val store = Store(value)
        map[id] = store

        val unsubscriber = store.subscribe {
            if(!queued){
                queued = true
            }
        }

        unsubscribers.add(unsubscriber)
        return store
    }

    private fun start(){
        while(queued){
            queued = false
            run(this)
            for (unsubscriber in unsubscribers){
                unsubscriber()
            }
        }
    }
}