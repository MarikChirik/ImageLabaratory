package ru.tech.imageresizershrinker.main_screen.viewModel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.olshevski.navigation.reimagined.navController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import ru.tech.imageresizershrinker.BuildConfig
import ru.tech.imageresizershrinker.main_screen.components.Screen
import ru.tech.imageresizershrinker.resize_screen.components.ToastHostState
import ru.tech.imageresizershrinker.theme.md_theme_dark_primary
import ru.tech.imageresizershrinker.utils.*
import java.net.URL
import javax.inject.Inject
import javax.xml.parsers.DocumentBuilderFactory

@HiltViewModel
class MainViewModel @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : ViewModel() {

    private val _saveFolderUri = mutableStateOf<Uri?>(null)
    val saveFolderUri by _saveFolderUri

    private val _nightMode = mutableStateOf(2)
    val nightMode by _nightMode

    private val _dynamicColors = mutableStateOf(true)
    val dynamicColors by _dynamicColors

    private val _allowImageMonet = mutableStateOf(true)
    val allowImageMonet by _allowImageMonet

    private val _amoledMode = mutableStateOf(false)
    val amoledMode by _amoledMode

    private val _appPrimaryColor = mutableStateOf(md_theme_dark_primary)
    val appPrimaryColor by _appPrimaryColor

    val navController = navController<Screen>(Screen.Main)

    private val _uris = mutableStateOf<List<Uri>?>(null)
    val uris by _uris

    private val _showSelectDialog = mutableStateOf(false)
    val showSelectDialog by _showSelectDialog

    private val _showUpdateDialog = mutableStateOf(false)
    val showUpdateDialog by _showUpdateDialog

    private val _cancelledUpdate = mutableStateOf(false)

    private val _shouldShowDialog = mutableStateOf(true)
    val shouldShowDialog by _shouldShowDialog

    private val _tag = mutableStateOf("")
    val tag by _tag

    private val _changelog = mutableStateOf("")
    val changelog by _changelog

    val toastHostState = ToastHostState()

    init {
        tryGetUpdate()
        runBlocking {
            dataStore.edit { prefs ->
                _nightMode.value = prefs[NIGHT_MODE] ?: 2
                _dynamicColors.value = prefs[DYNAMIC_COLORS] ?: true
                _amoledMode.value = prefs[AMOLED_MODE] ?: false
                _appPrimaryColor.value = (prefs[APP_COLOR]?.let { Color(it) }) ?: md_theme_dark_primary
            }
        }
        dataStore.data.onEach { prefs ->
            _saveFolderUri.value = prefs[SAVE_FOLDER]?.let { uri ->
                if (uri.isEmpty()) null
                else Uri.parse(uri)
            }
            _nightMode.value = prefs[NIGHT_MODE] ?: 2
            _dynamicColors.value = prefs[DYNAMIC_COLORS] ?: true
            _allowImageMonet.value = prefs[IMAGE_MONET] ?: true
            _amoledMode.value = prefs[AMOLED_MODE] ?: false
            _appPrimaryColor.value = (prefs[APP_COLOR]?.let { Color(it) }) ?: md_theme_dark_primary
        }.launchIn(viewModelScope)
    }

    fun updatePrimaryColor(color: Int) {
        viewModelScope.launch {
            dataStore.edit {
                it[APP_COLOR] = color
            }
        }
    }

    fun updateDynamicColors() {
        viewModelScope.launch {
            dataStore.edit {
                it[DYNAMIC_COLORS] = !dynamicColors
            }
        }
    }

    fun updateAllowImageMonet() {
        viewModelScope.launch {
            dataStore.edit {
                it[IMAGE_MONET] = !allowImageMonet
            }
        }
    }

    fun updateAmoledMode() {
        viewModelScope.launch {
            dataStore.edit {
                it[AMOLED_MODE] = !amoledMode
            }
        }
    }

    fun setNightMode(mode: Int) {
        viewModelScope.launch {
            dataStore.edit {
                it[NIGHT_MODE] = mode
            }
        }
    }

    fun cancelledUpdate(showAgain: Boolean = false) {
        if (!showAgain) _cancelledUpdate.value = true
        _showUpdateDialog.value = false
    }

    fun tryGetUpdate(newRequest: Boolean = false, onNoUpdates: () -> Unit = {}) {
        if (!_cancelledUpdate.value || newRequest) {
            viewModelScope.launch {
                withContext(Dispatchers.IO) {
                    kotlin.runCatching {
                        val nodes = DocumentBuilderFactory
                            .newInstance()
                            .newDocumentBuilder()
                            .parse(
                                URL("$APP_RELEASES.atom")
                                    .openConnection()
                                    .getInputStream()
                            )
                            ?.getElementsByTagName("feed")

                        if (nodes != null) {
                            for (i in 0 until nodes.length) {
                                val element = nodes.item(i) as Element
                                val title = element.getElementsByTagName("entry")
                                val line = (title.item(0) as Element)
                                _tag.value = (line.getElementsByTagName("title")
                                    .item(0) as Element).textContent
                                _changelog.value = (line.getElementsByTagName("content")
                                    .item(0) as Element).textContent
                            }
                        }

                        if (tag != BuildConfig.VERSION_NAME) {
                            _showUpdateDialog.value = true
                        } else {
                            onNoUpdates()
                        }
                    }
                }
            }
        }
    }

    fun updateUri(uri: Uri?) {
        _uris.value = null
        uri?.let {
            _uris.value = listOf(uri)
        }
    }

    fun hideSelectDialog() {
        _showSelectDialog.value = false
    }

    fun updateUris(uris: List<Uri>?) {
        _uris.value = null
        _uris.value = uris

        val dest = navController.backstack.entries.lastOrNull()?.destination
        if (uris != null && dest == Screen.Main) _showSelectDialog.value = true
    }

    fun showToast(
        message: String,
        icon: ImageVector? = null,
    ) {
        viewModelScope.launch {
            toastHostState.showToast(
                message = message,
                icon = icon
            )
        }
    }

    fun shouldShowExitDialog(b: Boolean) {
        _shouldShowDialog.value = b
    }

    fun updateSaveFolderUri(uri: Uri?) {
        viewModelScope.launch {
            dataStore.edit {
                it[SAVE_FOLDER] = uri?.toString() ?: ""
            }
        }
    }

}