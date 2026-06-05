package com.example.miprimerrepositorio

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var editText: EditText
    private lateinit var buttonSend: Button
    private lateinit var textViewReceived: TextView

    companion object {
        private const val CHAT_PATH = "/chat_message"
        private const val TAG = "PhoneMainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        editText = findViewById(R.id.editText)
        buttonSend = findViewById(R.id.button)
        textViewReceived = findViewById(R.id.textViewLabel)

        buttonSend.setOnClickListener {
            val message = editText.text.toString()
            if (message.isNotEmpty()) {
                sendMessage(message)
                editText.text.clear()
            } else {
                Toast.makeText(this, "Escribe un mensaje", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessage(message: String) {
        Log.d(TAG, "Intentando enviar mensaje: $message")
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Intentar obtener todos los nodos conectados
                val nodes = Wearable.getNodeClient(this@MainActivity).connectedNodes.await()
                Log.d(TAG, "Nodos encontrados: ${nodes.size}")
                
                val payload = message.toByteArray(StandardCharsets.UTF_8)
                
                if (nodes.isEmpty()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "No hay reloj conectado", Toast.LENGTH_SHORT).show()
                    }
                }

                for (node in nodes) {
                    val result = Wearable.getMessageClient(this@MainActivity)
                        .sendMessage(node.id, CHAT_PATH, payload)
                        .await()
                    
                    withContext(Dispatchers.Main) {
                        Log.d(TAG, "Mensaje enviado a: ${node.displayName} (${node.id}). Result ID: $result")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e(TAG, "Error enviando mensaje", e)
                    Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.d(TAG, "onMessageReceived llamado con path: ${messageEvent.path}")
        if (messageEvent.path == CHAT_PATH) {
            val receivedMessage = String(messageEvent.data, StandardCharsets.UTF_8)
            Log.d(TAG, "Mensaje recibido del reloj: $receivedMessage")
            
            runOnUiThread {
                textViewReceived.text = "Reloj: $receivedMessage"
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        Log.d(TAG, "Listener registrado")
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
        Log.d(TAG, "Listener eliminado")
    }
}
