package lol.max.assistantgpt.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
fun InfoDialog(onDismissRequest: () -> Unit = {}) {
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = {
        TextButton(onClick = onDismissRequest) {
            Text(text = "Close")

        }
    }, icon = { Icon(imageVector = Icons.Filled.Info, contentDescription = "Info") },
        title = { Text(text = "About") }, text = {
            Text(
                text = """This app is powered by OpenAI's GPT API. It is not an official app from OpenAI.
                    
Web searches provided by the Google Custom Search Engine
Weather provided by the National Weather Service
Geocoding API provided by Google
                        
openai-java © TheoKanning
Retrofit and OkHttp © Square
JTokkit © Knuddels
Android libraries © Android Open Source Project

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.""",
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(8.dp)
            )
        }, properties = DialogProperties(usePlatformDefaultWidth = false)
    )
}