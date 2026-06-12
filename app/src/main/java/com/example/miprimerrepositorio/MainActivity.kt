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
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity(), MessageClient.OnMessageReceivedListener {

    private lateinit var editText: EditText
    private lateinit var buttonSend: Button
    private lateinit var buttonGet: Button
    private lateinit var buttonPost: Button
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
        buttonGet = findViewById(R.id.buttonGet)
        buttonPost = findViewById(R.id.buttonPost)
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

        buttonGet.setOnClickListener {
            performGetRequest()
        }

        buttonPost.setOnClickListener {
            performPostRequest()
        }
    }

    private fun performGetRequest() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://jsonplaceholder.typicode.com/posts/1")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    withContext(Dispatchers.Main) {
                        textViewReceived.text = "GET Response: $response"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        textViewReceived.text = "GET Error: $responseCode"
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textViewReceived.text = "GET Exception: ${e.message}"
                }
            }
        }
    }

    private fun performPostRequest() {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val url = URL("https://jsonplaceholder.typicode.com/posts")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; utf-8")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true

                val jsonInputString = "{\"title\": \"foo\", \"body\": \"bar\", \"userId\": 1}"

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(jsonInputString)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_CREATED || responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.use { it.readText() }
                    withContext(Dispatchers.Main) {
                        textViewReceived.text = "POST Response: $response"
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        textViewReceived.text = "POST Error: $responseCode"
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    textViewReceived.text = "POST Exception: ${e.message}"
                }
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
