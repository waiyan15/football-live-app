package com.footballlive.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL

data class Channel(
    val name: String,
    val url: String,
    val category: String = "All",
    val logo: String = ""
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
    private lateinit var searchInput: EditText
    private lateinit var categorySpinner: Spinner

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

        viewModel =
            ViewModelProvider(this)[ChannelViewModel::class.java]

        urlInput = findViewById(R.id.urlInput)
        loadUrlButton = findViewById(R.id.loadButton)
        fileButton = findViewById(R.id.fileButton)
        gridView = findViewById(R.id.channelGrid)
        progress = findViewById(R.id.progress)
        searchInput = findViewById(R.id.searchInput)
        categorySpinner = findViewById(R.id.categorySpinner)

        if (viewModel.channels.isNotEmpty()) {
            updateCategories()
            showChannels(viewModel.channels)
        }

        if (viewModel.savedUrl.isNotEmpty()) {
            urlInput.setText(viewModel.savedUrl)
        }

        loadUrlButton.setOnClickListener {

            val url =
                urlInput.text.toString().trim()

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

            val adapter =
                gridView.adapter as? ArrayAdapter<Channel>

            val channel =
                adapter?.getItem(position)

            if (channel != null) {

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

        searchInput.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filterChannels()
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {}
            }
        )

        categorySpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    filterChannels()
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {}
            }
    }

    private fun loadM3U(
        url: String
    ) {

        progress.visibility =
            View.VISIBLE

        loadUrlButton.isEnabled =
            false

        fileButton.isEnabled =
            false

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val text =
                    URL(url)
                        .openStream()
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                val result =
                    parseM3U(text)

                withContext(
                    Dispatchers.Main
                ) {

                    viewModel.channels.clear()
                    viewModel.channels.addAll(result)

                    updateCategories()
                    showChannels(result)

                    progress.visibility =
                        View.GONE

                    loadUrlButton.isEnabled =
                        true

                    fileButton.isEnabled =
                        true

                    Toast.makeText(
                        this@MainActivity,
                        "${result.size} channels loaded",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                withContext(
                    Dispatchers.Main
                ) {

                    progress.visibility =
                        View.GONE

                    loadUrlButton.isEnabled =
                        true

                    fileButton.isEnabled =
                        true

                    Toast.makeText(
                        this@MainActivity,
                        "M3U Load မအောင်မြင်ပါ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadM3UFile(
        uri: Uri
    ) {

        progress.visibility =
            View.VISIBLE

        loadUrlButton.isEnabled =
            false

        fileButton.isEnabled =
            false

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val text =
                    contentResolver
                        .openInputStream(uri)
                        ?.bufferedReader()
                        ?.use {
                            it.readText()
                        }
                        ?: throw Exception(
                            "File မဖတ်နိုင်ပါ"
                        )

                val result =
                    parseM3U(text)

                withContext(
                    Dispatchers.Main
                ) {

                    viewModel.channels.clear()
                    viewModel.channels.addAll(result)

                    updateCategories()
                    showChannels(result)

                    progress.visibility =
                        View.GONE

                    loadUrlButton.isEnabled =
                        true

                    fileButton.isEnabled =
                        true

                    Toast.makeText(
                        this@MainActivity,
                        "${result.size} channels loaded",
                        Toast.LENGTH_SHORT
                    ).show()
                }

            } catch (e: Exception) {

                withContext(
                    Dispatchers.Main
                ) {

                    progress.visibility =
                        View.GONE

                    loadUrlButton.isEnabled =
                        true

                    fileButton.isEnabled =
                        true

                    Toast.makeText(
                        this@MainActivity,
                        "M3U File ဖတ်မရပါ",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateCategories() {

        val categories =
            ArrayList<String>()

        categories.add("All")

        viewModel.channels.forEach {

            if (
                it.category.isNotBlank() &&
                !categories.contains(it.category)
            ) {
                categories.add(it.category)
            }
        }

        categorySpinner.adapter =
            ArrayAdapter(
                this,
                android.R.layout.simple_spinner_dropdown_item,
                categories
            )
    }

    private fun filterChannels() {

        val search =
            searchInput.text
                .toString()
                .trim()
                .lowercase()

        val category =
            categorySpinner.selectedItem
                ?.toString()
                ?: "All"

        val filtered =
            viewModel.channels.filter {

                val nameMatch =
                    it.name
                        .lowercase()
                        .contains(search)

                val categoryMatch =
                    category == "All" ||
                    it.category == category

                nameMatch && categoryMatch
            }

        showChannels(filtered)
    }

    private fun showChannels(
        channels: List<Channel>
    ) {

        val adapter =
            object : ArrayAdapter<Channel>(
                this,
                R.layout.item_channel,
                channels
            ) {

                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: android.view.ViewGroup
                ): View {

                    val view =
                        convertView
                            ?: layoutInflater.inflate(
                                R.layout.item_channel,
                                parent,
                                false
                            )

                    val channel =
                        getItem(position)

                    val name =
                        view.findViewById<TextView>(
                            R.id.channelName
                        )

                    val category =
                        view.findViewById<TextView>(
                            R.id.channelCategory
                        )

                    val logo =
                        view.findViewById<ImageView>(
                            R.id.channelLogo
                        )

                    name.text =
                        channel?.name
                            ?: "Unknown Channel"

                    category.text =
                        channel?.category
                            ?: "TV"

                    logo.setImageResource(
                        android.R.drawable.ic_media_play
                    )

                    if (
                        channel != null &&
                        channel.logo.isNotBlank()
                    ) {

                        loadLogo(
                            channel.logo,
                            logo
                        )
                    }

                    return view
                }
            }

        gridView.adapter =
            adapter
    }

    private fun loadLogo(
        logoUrl: String,
        imageView: ImageView
    ) {

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val connection =
                    URL(logoUrl)
                        .openConnection()
                        as HttpURLConnection

                connection.connectTimeout =
                    5000

                connection.readTimeout =
                    5000

                connection.doInput =
                    true

                connection.connect()

                val bitmap =
                    BitmapFactory.decodeStream(
                        connection.inputStream
                    )

                connection.disconnect()

                if (bitmap != null) {

                    withContext(
                        Dispatchers.Main
                    ) {

                        imageView.setImageBitmap(
                            bitmap
                        )
                    }
                }

            } catch (_: Exception) {
                // Logo မရရင် default icon ကိုပဲပြ
            }
        }
    }

    private fun parseM3U(
        text: String
    ): List<Channel> {

        val result =
            ArrayList<Channel>()

        val lines =
            text.lines()
                .map {
                    it.trim()
                }
                .filter {
                    it.isNotEmpty()
                }

        var channelName = ""
        var category = "All"
        var logo = ""

        for (line in lines) {

            if (
                line.startsWith("#EXTINF")
            ) {

                channelName =
                    line.substringAfter(
                        ",",
                        "Unknown Channel"
                    ).trim()

                val group =
                    Regex(
                        """group-title="([^"]*)""""
                    ).find(line)

                category =
                    group?.groupValues?.get(1)
                        ?: "All"

                val logoMatch =
                    Regex(
                        """tvg-logo="([^"]*)""""
                    ).find(line)

                logo =
                    logoMatch?.groupValues?.get(1)
                        ?: ""

            } else if (
                !line.startsWith("#") &&
                channelName.isNotEmpty()
            ) {

                result.add(
                    Channel(
                        name = channelName,
                        url = line,
                        category = category,
                        logo = logo
                    )
                )

                channelName = ""
                category = "All"
                logo = ""
            }
        }

        return result
    }
}
