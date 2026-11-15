package app.injaz.starter

import android.app.Application
import app.injaz.starter.data.AppGraph

class RaseedApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}
