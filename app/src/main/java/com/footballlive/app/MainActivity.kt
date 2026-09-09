package com.footballlive.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import java.net.URL

data class Channel(
    val name: String,
    val url: String
)

class ChannelViewModel : ViewModel() {
    val channels = ArrayList<Channel>()
    var savedUrl: String = ""
}

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var loadUrlButton: Button
    private lateinit var fileButton: Button
    private lateinit var gridView: GridView
    private lateinit var progress: ProgressBar

    private lateinit var viewModel: ChannelViewModel

    private val filePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri?.let {
                loadM3UFile(it)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        viewModel = ViewModelProvider(this)[ChannelViewModel::class.java]

        urlInput = findViewById(R.id.urlInput)
        loadUrlButton = findViewById(R.id.loadButton)
        fileButton = findViewById(R.id.fileButton)
        gridView = findViewById(R.id.channelGrid)
        progress = findViewById(R.id.progress)

        // အရင် playlist ရှိရင် ပြန်ပြ
        if (viewModel.channels.isNotEmpty()) {
            showChannels(viewModel.channels)
        }

        if (viewModel.savedUrl.isNotEmpty()) {
            urlInput.setText(viewModel.savedUrl)
        }

        loadUrlButton.setOnClickListener {

            val url = urlInput.text.toString().trim()

            if (url.isEmpty()) {

                Toast.makeText(
                    this,
                    "M3U / M3U8 URL ထည့်ပါ",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                viewModel.savedUrl = url
                loadM3U(url)
            }
        }

        fileButton.setOnClickListener {

            filePicker.launch("*/*")
        }

        gridView.setOnItemClickListener { _, _, position, _ ->

            if (position < viewModel.channels.size) {

                val channel = viewModel.channels[position]

                val intent =
                    Intent(
                        this,
                        PlayerActivity::class.java
                    )

                intent.putExtra(
                    "name",
                    channel.name
                )

                intent.putExtra(
                    "url",
                    channel.url
                )

                startActivity(intent)
            }
        }
    }

    private fun loadM3U(m3uUrl: String) {

        progress.visibility = View.VISIBLE

        loadUrlButton.isEnabled = false
        fileButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val text =
                    URL(m3uUrl)
                        .openStream()
                        .bufferedReader()
                        .use { it.readText() }

                val result = parseM3U(text)

                withContext(Dispatchers.Main) {

                    viewModel.channels.clear()
                    viewModel.channels.addAll(result)

                    showChannels(viewModel.channels)

                    progress.visibility = View.GONE

                    loadUrlButton.isEnabled = true
                    fileButton.isEnabled = true

                    Toast.makeText(
                        this@MainActivity,
                        "${result.size} channels loaded",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    progress.visibility = View.GONE

                    loadUrlButton.isEnabled = true
                    fileButton.isEnabled = true

                    Toast.makeText(
                        this@MainActivity,
                        "M3U / M3U8 Load မအောင်မြင်ပါ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadM3UFile(uri: Uri) {

        progress.visibility = View.VISIBLE

        loadUrlButton.isEnabled = false
        fileButton.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {

            try {

                val text =
                    contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        ?: throw Exception(
                            "File မဖတ်နိုင်ပါ"
                        )

                val result = parseM3U(text)

                withContext(Dispatchers.Main) {

                    viewModel.channels.clear()
                    viewModel.channels.addAll(result)

                    showChannels(viewModel.channels)

                    progress.visibility = View.GONE

                    loadUrlButton.isEnabled = true
                    fileButton.isEnabled = true

                    Toast.makeText(
                        this@MainActivity,
                        "${result.size} channels loaded",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                withContext(Dispatchers.Main) {

                    progress.visibility = View.GONE

                    loadUrlButton.isEnabled = true
                    fileButton.isEnabled = true

                    Toast.makeText(
                        this@MainActivity,
                        "M3U File ဖတ်မရပါ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun showChannels(
        channels: List<Channel>
    ) {

        val names =
            channels.map {
                it.name
            }

        val adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                names
            )

        gridView.adapter = adapter
    }

    private fun parseM3U(
        text: String
    ): List<Channel> {

        val result =
            ArrayList<Channel>()

        val lines =
            text.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() }

        var channelName = ""

        for (line in lines) {

            if (line.startsWith("#EXTINF")) {

                channelName =
                    line.substringAfter(
                        ",",
                        "Unknown Channel"
                    ).trim()

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
