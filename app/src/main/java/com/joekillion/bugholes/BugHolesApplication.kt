package com.joekillion.bugholes

import android.app.Application
import com.joekillion.bugholes.data.AppDatabase
import com.joekillion.bugholes.data.GameRepository

class BugHolesApplication : Application() {
    val database by lazy { AppDatabase.create(this) }
    val repository by lazy { GameRepository(database.dao()) }
}
