package lol.max.assistantgpt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState

enum class AlertDialogTypes {
    NONE,
}

@Composable
fun AlertDialogs(type: MutableState<AlertDialogTypes>) {
    when (type.value) {

        else -> return
    }

}