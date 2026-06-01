// Developer: Chetraj Jaishi
package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainTrackerScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TrackerViewModel

class MainActivity : ComponentActivity() {
  private val trackerViewModel: TrackerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      val isDark by trackerViewModel.isDarkMode.collectAsStateWithLifecycle()
      val isMonochrome by trackerViewModel.isMonochromeMode.collectAsStateWithLifecycle()
      
      MyApplicationTheme(darkTheme = isDark, isMonochrome = isMonochrome) {
        MainTrackerScreen(viewModel = trackerViewModel, modifier = Modifier.fillMaxSize())
      }
    }
  }
}
