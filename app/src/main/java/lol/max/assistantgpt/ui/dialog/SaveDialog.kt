package lol.max.assistantgpt.ui.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onPlaced

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaveDialog(onDismissRequest: () -> Unit, onConfirm: (name: String) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    val textField = FocusRequester()
    AlertDialog(onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                if (name == "" || name.length > 20) return@TextButton
                onConfirm(name)
                onDismissRequest()
            }) {
                Text(text = "Save")
            }
        },
        title = {
            Text(
                text = "Save Chat"
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                isError = name == "" || name.length > 20,
                singleLine = true,
                label = {
                    Text(
                        text = "Enter a name"
                    )
                },
                modifier = Modifier
                    .focusRequester(textField)
                    .onPlaced { textField.requestFocus() })
        })
}