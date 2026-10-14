package com.example

import android.app.Application
import com.example.data.db.AuraDatabase
import com.example.engine.RollbackEngine
import com.example.engine.TaskTimePlanner

class AuraApplication : Application() {

    lateinit var database: AuraDatabase
        private set

    lateinit var rollbackEngine: RollbackEngine
        private set

    lateinit var taskPlanner: TaskTimePlanner
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = AuraDatabase.getInstance(this)
        rollbackEngine = RollbackEngine(database.auraDao())
        taskPlanner = TaskTimePlanner()
    }

    companion object {
        lateinit var instance: AuraApplication
            private set
    }
}
