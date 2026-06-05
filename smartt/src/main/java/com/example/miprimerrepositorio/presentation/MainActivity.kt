package com.example.miprimerrepositorio.presentation

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.*
import com.example.miprimerrepositorio.presentation.theme.MiPrimerRepositorioTheme
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {

    private var receivedMessage = mutableStateOf("Esperando mensaje...")
    private var textToSend = mutableStateOf("")

    companion object {
        private const val CHAT_PATH = "/chat_message"
        private const val TAG = "WatchMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MiPrimerRepositorioTheme {
                val listState = rememberTransformingLazyColumnState()
                
                AppScaffold {
                    ScreenScaffold(scrollState = listState) { contentPadding ->
                        TransformingLazyColumn(
                            state = listState,
                            contentPadding = contentPadding,
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                Text(
                                    text = "Chat con Teléfono",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 10.dp)
                                )
                            }

                            item {
                                Card(
                                    onClick = { /* Opcional: acción al tocar el mensaje */ },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = receivedMessage.value,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }

                            item {
                                // En Wear OS 3+ se prefiere usar botones de acción rápida
                                // o una interfaz de entrada de texto dedicada.
                                // Para este chat, usaremos botones predefinidos o podrías
                                // integrar RemoteInput.
                                Button(
                                    onClick = { sendMessage("¡Hola desde el reloj!") },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                ) {
                                    Text("Enviar 'Hola'")
                                }
                            }

                            item {
                                Button(
                                    onClick = { sendMessage("¿Cómo vas?") },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                ) {
                                    Text("Enviar '¿Cómo vas?'")
                                }
                            }
                            
                            item {
                                Button(
                                    onClick = { sendMessage("Entendido ✅") },
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)
                                ) {
                                    Text("Enviar 'Entendido'")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun sendMessage(message: String) {
        Log.d(TAG, "Intentando enviar mensaje desde reloj: $message")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                Log.d(TAG, "Nodos encontrados desde reloj: ${nodes.size}")
                
                val payload = message.toByteArray(StandardCharsets.UTF_8)
                
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No hay nodos conectados para enviar el mensaje")
                }

                for (node in nodes) {
                    val result = Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, CHAT_PATH, payload)
                        .await()
                    
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Mensaje enviado al teléfono: ${node.displayName} (${node.id}). Result ID: $result")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error enviando mensaje desde reloj", e)
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived en reloj llamado con path: ${messageEvent.path}")
        if (messageEvent.path == CHAT_PATH) {
            val message = String(messageEvent.data, StandardCharsets.UTF_8)
            Log.d(TAG, "Mensaje recibido del teléfono: $message")
            receivedMessage.value = "Teléfono: $message"
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        Log.d(TAG, "Listener registrado en reloj")
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
        Log.d(TAG, "Listener eliminado en reloj")
    }
}
