package com.pocketradar.smartcoach.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pocketradar.smartcoach.SmartCoachError

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                // Without a configured SDK every SmartCoach call throws notConfigured.
                val error = SampleApplication.configurationError
                if (error != null) ConfigurationErrorScreen(error) else HomeScreen()
            }
        }
    }
}

@Composable
private fun ConfigurationErrorScreen(error: SmartCoachError) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Text(stringResource(R.string.configuration_failed), style = MaterialTheme.typography.headlineSmall)
        Text(
            errorText(error),
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
