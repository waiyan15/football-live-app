package com.footballlive.app

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.*
import java.net.URL

data class Channel(
    val name: String,
    val url: String
)

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var loadButton: Button
    private lateinit var listView: ListView
    private lateinit var progress: ProgressBar

    private val channels = ArrayList<Channel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        urlInput = findViewById(R.id.urlInput)
        loadButton = findViewById(R.id.loadButton)
        listView = findViewById(R.id.channelList)
        progress = findViewById(R.id.progress)

        loadButton.setOnClickListener {
            val url = urlInput.text.toString().trim()

            if (url.isEmpty()) {
                Toast.makeText(
                    this,
                    "M3U URL ထည့်ပါ",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                loadM3U(url)
            }
        }

        listView.setOnItemClickListener { _, _, position, _ ->

            val channel = channels[position]

            val intent = Intent(
                this,
                PlayerActivity::class.java
            )

            intent.putExtra("name", channel.name)
            intent.putExtra("url", channel.url)

            startActivity(intent)
        }
    }

    private fun loadM3U(m3uUrl: String) {

        progress.visibility = ProgressBar.VISIBLE
        loadButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {

            try {
                val text = URL(m3uUrl)
                    .openStream()
                    .bufferedReader()
                    .use { it.readText() }

                val result = parseM3U(text)

                withContext(Dispatchers.Main) {

                    channels.clear()
                    channels.addAll(result)

                    val names = channels.map {
                        it.name
                    }

                    val adapter = ArrayAdapter(
                        this@MainActivity,
                        android.R.layout.simple_list_item_1,
                        names
                    )

                    listView.adapter = adapter

                    progress.visibility = ProgressBar.GONE
                    loadButton.isEnabled = true

                    Toast.makeText(
                        this@MainActivity,
                        "${channels.size} channels loaded",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    progress.visibility = ProgressBar.GONE
                    loadButton.isEnabled = true

                    Toast.makeText(
                        this@MainActivity,
                        "M3U Load မအောင်မြင်ပါ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun parseM3U(
        text: String
    ): List<Channel> {

        val result = ArrayList<Channel>()

        val lines = text
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var channelName = ""

        for (line in lines) {

            if (line.startsWith("#EXTINF")) {

                channelName = line
                    .substringAfter(
                        ",",
                        "Unknown Channel"
                    )
                    .trim()

            } else if (
                !line.startsWith("#") &&
                channelName.isNotEmpty()
            ) {

                result.add(
                    Channel(
                        channelName,
                        line
                    )
                )

                channelName = ""
            }
        }

        return result
    }
}
