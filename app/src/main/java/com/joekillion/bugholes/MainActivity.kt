package com.joekillion.bugholes

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.joekillion.bugholes.ui.BugHolesApp
import com.joekillion.bugholes.ui.BugHolesTheme
import com.joekillion.bugholes.ui.MainViewModel
import com.joekillion.bugholes.ui.MainViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as BugHolesApplication
        setContent {
            BugHolesTheme {
                val vm: MainViewModel = viewModel(factory = MainViewModelFactory(app.repository))
                BugHolesApp(vm)
            }
        }
    }
}
