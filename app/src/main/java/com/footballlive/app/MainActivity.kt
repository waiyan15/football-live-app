package com.footballlive.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    var currentPage: String = "channels"
}

class MainActivity : AppCompatActivity() {

    private lateinit var urlInput: EditText
    private lateinit var loadUrlButton: Button
    private lateinit var fileButton: Button
    private lateinit var gridView: GridView
    private lateinit var progress: ProgressBar
    private lateinit var searchInput: EditText
    private lateinit var categorySpinner: Spinner

    private lateinit var homeLayout: LinearLayout
    private lateinit var scoresLayout: LinearLayout
    private lateinit var favoritesLayout: LinearLayout

    private lateinit var navHome: TextView
    private lateinit var navChannels: TextView
    private lateinit var navScores: TextView
    private lateinit var navFavorites: TextView
    private lateinit var pageTitle: TextView

    private lateinit var viewModel: ChannelViewModel

    private val prefs by lazy {
        getSharedPreferences(
            "football_live_prefs",
            MODE_PRIVATE
        )
    }

    private val filePicker =
        registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                loadM3UFile(uri)
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

        homeLayout = findViewById(R.id.homeLayout)
        scoresLayout = findViewById(R.id.scoresLayout)
        favoritesLayout = findViewById(R.id.favoritesLayout)

        navHome = findViewById(R.id.navHome)
        navChannels = findViewById(R.id.navChannels)
        navScores = findViewById(R.id.navScores)
        navFavorites = findViewById(R.id.navFavorites)

        pageTitle = findViewById(R.id.pageTitle)

        loadUrlButton.setOnClickListener {

            val url =
                urlInput.text.toString().trim()

            if (url.isEmpty()) {

                Toast.makeText(
                    this,
                    "M3U / M3U8 URL ထည့်ပါ",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            viewModel.savedUrl = url

            loadM3U(url)
        }

        fileButton.setOnClickListener {
            filePicker.launch("*/*")
        }

        gridView.setOnItemClickListener {
                _,
                _,
                position,
                _ ->

            val adapter =
                gridView.adapter as? ArrayAdapter<Channel>

            if (adapter == null) {
                return@setOnItemClickListener
            }

            if (
                position < 0 ||
                position >= adapter.count
            ) {
                return@setOnItemClickListener
            }

            val channel =
                adapter.getItem(position)

            if (channel != null) {
                openPlayer(channel)
            }
        }

        searchInput.addTextChangedListener(
            object : android.text.TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {
                }

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {

                    if (
                        viewModel.currentPage ==
                        "channels"
                    ) {
                        filterChannels()
                    }
                }

                override fun afterTextChanged(
                    s: android.text.Editable?
                ) {
                }
            }
        )

        categorySpinner.onItemSelectedListener =
            object :
                AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (
                        viewModel.currentPage ==
                        "channels"
                    ) {
                        filterChannels()
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>?
                ) {
                }
            }

        navHome.setOnClickListener {
            showHome()
        }

        navChannels.setOnClickListener {
            showChannelsPage()
        }

        navScores.setOnClickListener {
            showScores()
        }

        navFavorites.setOnClickListener {
            showFavorites()
        }

        showChannelsPage()
    }

    private fun openPlayer(channel: Channel) {

        try {

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

        } catch (e: Exception) {

            Toast.makeText(
                this,
                "Player ဖွင့်မရပါ",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun showHome() {

        viewModel.currentPage = "home"

        homeLayout.visibility = View.VISIBLE
        scoresLayout.visibility = View.GONE
        favoritesLayout.visibility = View.GONE
        gridView.visibility = View.GONE

        urlInput.visibility = View.GONE
        loadUrlButton.visibility = View.GONE
        fileButton.visibility = View.GONE
        searchInput.visibility = View.GONE
        categorySpinner.visibility = View.GONE

        pageTitle.text = "🏠 Home"

        updateNav(navHome)
    }

    private fun showChannelsPage() {

        viewModel.currentPage = "channels"

        homeLayout.visibility = View.GONE
        scoresLayout.visibility = View.GONE
        favoritesLayout.visibility = View.GONE
        gridView.visibility = View.VISIBLE

        urlInput.visibility = View.VISIBLE
        loadUrlButton.visibility = View.VISIBLE
        fileButton.visibility = View.VISIBLE
        searchInput.visibility = View.VISIBLE
        categorySpinner.visibility = View.VISIBLE

        pageTitle.text = "📺 TV Channels"

        filterChannels()

        updateNav(navChannels)
    }

    private fun showScores() {

        viewModel.currentPage = "scores"

        homeLayout.visibility = View.GONE
        scoresLayout.visibility = View.VISIBLE
        favoritesLayout.visibility = View.GONE
        gridView.visibility = View.GONE

        urlInput.visibility = View.GONE
        loadUrlButton.visibility = View.GONE
        fileButton.visibility = View.GONE
        searchInput.visibility = View.GONE
        categorySpinner.visibility = View.GONE

        pageTitle.text = "⚽ Live Scores"

        updateNav(navScores)
    }

    private fun showFavorites() {

        viewModel.currentPage = "favorites"

        homeLayout.visibility = View.GONE
        scoresLayout.visibility = View.GONE
        favoritesLayout.visibility = View.VISIBLE
        gridView.visibility = View.GONE

        urlInput.visibility = View.GONE
        loadUrlButton.visibility = View.GONE
        fileButton.visibility = View.GONE
        searchInput.visibility = View.GONE
        categorySpinner.visibility = View.GONE

        pageTitle.text = "♥ Favorites"

        showFavoriteCards()

        updateNav(navFavorites)
    }

    private fun updateNav(selected: TextView) {

        val items =
            listOf(
                navHome,
                navChannels,
                navScores,
                navFavorites
            )

        items.forEach { item ->

            item.alpha =
                if (item == selected) {
                    1.0f
                } else {
                    0.55f
                }
        }
    }

    private fun showFavoriteCards() {

        favoritesLayout.removeAllViews()

        val favorites =
            viewModel.channels.filter {
                isFavorite(it)
            }

        if (favorites.isEmpty()) {

            val empty =
                TextView(this)

            empty.text =
                "♥\n\nNo Favorite Channels\n\nChannel တစ်ခုကို ♥ နှိပ်ပြီး Favorites ထဲထည့်ပါ"

            empty.setTextColor(Color.WHITE)

            empty.textSize = 16f

            empty.gravity = Gravity.CENTER

            favoritesLayout.addView(
                empty,
                LinearLayout.LayoutParams(
                    -1,
                    300
                )
            )

            return
        }

        favorites.forEach { channel ->

            val card =
                TextView(this)

            card.text =
                "▶  ${channel.name}\n     ${channel.category}"

            card.setTextColor(Color.WHITE)

            card.textSize = 16f

            card.setPadding(
                20,
                18,
                20,
                18
            )

            card.setBackgroundColor(
                Color.rgb(
                    21,
                    27,
                    34
                )
            )

            val params =
                LinearLayout.LayoutParams(
                    -1,
                    75
                )

            params.setMargins(
                14,
                6,
                14,
                6
            )

            favoritesLayout.addView(
                card,
                params
            )

            card.setOnClickListener {
                openPlayer(channel)
            }
        }
    }

    private fun isFavorite(
        channel: Channel
    ): Boolean {

        return prefs.getBoolean(
            "fav_${channel.url}",
            false
        )
    }

    private fun setFavorite(
        channel: Channel,
        favorite: Boolean
    ) {

        prefs.edit()
            .putBoolean(
                "fav_${channel.url}",
                favorite
            )
            .apply()
    }

    private fun setLoading(
        loading: Boolean
    ) {

        progress.visibility =
            if (loading) {
                View.VISIBLE
            } else {
                View.GONE
            }

        loadUrlButton.isEnabled =
            !loading

        fileButton.isEnabled =
            !loading
    }

    private fun loadM3U(
        url: String
    ) {

        setLoading(true)

        CoroutineScope(
            Dispatchers.IO
        ).launch {

            try {

                val connection =
                    URL(url)
                        .openConnection()
                        as HttpURLConnection

                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.requestMethod = "GET"

                connection.connect()

                val responseCode =
                    connection.responseCode

                if (
                    responseCode !in 200..299
                ) {
                    throw Exception(
                        "HTTP $responseCode"
                    )
                }

                val text =
                    connection.inputStream
                        .bufferedReader()
                        .use {
                            it.readText()
                        }

                connection.disconnect()

                val result =
                    parseM3U(text)

                withContext(
                    Dispatchers.Main
                ) {

                    viewModel.channels.clear()

                    viewModel.channels.addAll(
                        result
                    )

                    updateCategories()

                    setLoading(false)

                    showChannelsPage()

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

                    setLoading(false)

                    Toast.makeText(
                        this@MainActivity,
                        "M3U Load မအောင်မြင်ပါ\n${e.message ?: ""}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun loadM3UFile(
        uri: Uri
    ) {

        setLoading(true)

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

                    viewModel.channels.addAll(
                        result
                    )

                    updateCategories()

                    setLoading(false)

                    showChannelsPage()

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

                    setLoading(false)

                    Toast.makeText(
                        this@MainActivity,
                        "M3U File ဖတ်မရပါ\n${e.message ?: ""}",
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

        viewModel.channels.forEach { channel ->

            val category =
                channel.category.trim()

            if (
                category.isNotEmpty() &&
                !categories.contains(category)
            ) {

                categories.add(category)
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

        if (!::searchInput.isInitialized) {
            return
        }

        val search =
            searchInput.text
                .toString()
                .trim()
                .lowercase()

        val category =
            if (
                categorySpinner.selectedItem != null
            ) {
                categorySpinner.selectedItem
                    .toString()
            } else {
                "All"
            }

        val filtered =
            viewModel.channels.filter { channel ->

                val nameMatch =
                    channel.name
                        .lowercase()
                        .contains(search)

                val categoryMatch =
                    category == "All" ||
                    channel.category == category

                nameMatch &&
                    categoryMatch
            }

        showChannels(filtered)
    }

    private fun showChannels(
        channels: List<Channel>
    ) {

        val safeChannels =
            channels.toList()

        val adapter =
            object : ArrayAdapter<Channel>(
                this,
                R.layout.item_channel,
                safeChannels
            ) {

                override fun getView(
                    position: Int,
                    convertView: View?,
                    parent: ViewGroup
                ): View {

                    val view =
                        try {

                            convertView
                                ?: layoutInflater.inflate(
                                    R.layout.item_channel,
                                    parent,
                                    false
                                )

                        } catch (e: Exception) {

                            TextView(this@MainActivity).apply {
                                setTextColor(Color.WHITE)
                                textSize = 14f
                                gravity = Gravity.CENTER
                                setPadding(
                                    10,
                                    10,
                                    10,
                                    10
                                )
                            }
                        }

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

                    val favoriteButton =
                        view.findViewById<TextView>(
                            R.id.favoriteButton
                        )

                    if (name != null) {

                        name.text =
                            channel?.name
                                ?: "Unknown Channel"
                    }

                    if (category != null) {

                        category.text =
                            channel?.category
                                ?: "TV"
                    }

                    if (logo != null) {

                        logo.setImageResource(
                            android.R.drawable.ic_media_play
                        )
                    }

                    if (
                        channel != null &&
                        logo != null &&
                        channel.logo.isNotBlank()
                    ) {

                        loadLogo(
                            channel.logo,
                            logo
                        )
                    }

                    if (
                        channel != null &&
                        favoriteButton != null
                    ) {

                        favoriteButton.text =
                            if (
                                isFavorite(channel)
                            ) {
                                "♥"
                            } else {
                                "♡"
                            }

                        favoriteButton.setOnClickListener {

                            val newState =
                                !isFavorite(channel)

                            setFavorite(
                                channel,
                                newState
                            )

                            favoriteButton.text =
                                if (newState) {
                                    "♥"
                                } else {
                                    "♡"
                                }
                        }
                    }

                    return view
                }
            }

        gridView.adapter = adapter
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

                connection.connectTimeout = 3000
                connection.readTimeout = 3000
                connection.doInput = true

                connection.connect()

                val bitmap =
                    connection.inputStream
                        .use {
                            BitmapFactory.decodeStream(it)
                        }

                connection.disconnect()

                if (bitmap != null) {

                    withContext(
                        Dispatchers.Main
                    ) {

                        if (
                            !isFinishing &&
                            !isDestroyed
                        ) {

                            imageView.setImageBitmap(
                                bitmap
                            )
                        }
                    }
                }

            } catch (_: Exception) {
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
                line.startsWith(
                    "#EXTINF",
                    ignoreCase = true
                )
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
                    group
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.trim()
                        ?.ifEmpty {
                            "All"
                        }
                        ?: "All"

                val logoMatch =
                    Regex(
                        """tvg-logo="([^"]*)""""
                    ).find(line)

                logo =
                    logoMatch
                        ?.groupValues
                        ?.getOrNull(1)
                        ?.trim()
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
