package io.ceezix.cryptography

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.ceezix.api.GeminiRepository
import io.ceezix.data.AppDatabase
import io.ceezix.data.DataStoreManager
import io.ceezix.data.WorkspaceFile
import io.ceezix.data.WorkspaceRepository
import io.ceezix.data.GitCommitEntity
import io.ceezix.data.WorkspaceFileSnapshot
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ceezix.models.ChallengeDatabase
import io.ceezix.models.CipherChallenge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ConsoleLogEntry(
    val type: String, // "info", "log", "error", "warn"
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

class CezixViewModel(application: Application) : AndroidViewModel(application) {

    private val dataStoreManager = DataStoreManager(application)
    private val geminiRepository = GeminiRepository()
    
    // --- Room Database Virtual Workspace System ---
    private val database = AppDatabase.getDatabase(application)
    private val workspaceRepository = WorkspaceRepository(database.workspaceDao())
    
    val workspaceFiles = MutableStateFlow<List<WorkspaceFile>>(emptyList())
    
    // --- Code Editor UI Workspace States ---
    private val _openTabIds = MutableStateFlow<List<Int>>(emptyList())
    val openTabIds: StateFlow<List<Int>> = _openTabIds.asStateFlow()
    
    private val _selectedFileId = MutableStateFlow<Int?>(null)
    val selectedFileId: StateFlow<Int?> = _selectedFileId.asStateFlow()

    private val _activeTab = MutableStateFlow(0) // 0: Editor Workspace, 1: HTML Canvas Runner, 2: Codebook/Logs, 3: AI Copilot
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()
    
    val editorContent = MutableStateFlow("")
    val editorTheme = MutableStateFlow("Dracula Dark") // "Dracula Dark", "Monokai", "Code Green", "Retro Orange"
    val editorFontSize = MutableStateFlow(15) // font size in sp
    
    // Search and Replace values
    val searchQuery = MutableStateFlow("")
    val replaceQuery = MutableStateFlow("")
    val isSearchBoxVisible = MutableStateFlow(false)

    // --- Virtual Offline WebView Console ---
    private val _consoleLogs = MutableStateFlow<List<ConsoleLogEntry>>(emptyList())
    val consoleLogs: StateFlow<List<ConsoleLogEntry>> = _consoleLogs.asStateFlow()

    // --- AI Pairing Assistant States ---
    private val _aiOutput = MutableStateFlow<String?>(null)
    val aiOutput: StateFlow<String?> = _aiOutput.asStateFlow()
    
    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    // --- Core Cipher Playground States (Preserved & Styled) ---
    val currentMode = MutableStateFlow("Caesar") // "Caesar", "ROT13", "Atbash", "Vigenere"
    val inputText = MutableStateFlow("MDUV DUH WR EH GHSOXBHG DW GDZQ")
    val currentShift = MutableStateFlow(3)
    val vigenereKey = MutableStateFlow("KEY")
    val wheelTheme = MutableStateFlow("Cyber Neon") // "Cyber Neon", "Ubuntu", "VS Code", "GitHub", "Minimalist", "Antique"

    private val _totalSolves = MutableStateFlow(0)
    val totalSolves: StateFlow<Int> = _totalSolves.asStateFlow()

    private val _currentStreak = MutableStateFlow(0)
    val currentStreak: StateFlow<Int> = _currentStreak.asStateFlow()

    private val _userXp = MutableStateFlow(0)
    val userXp: StateFlow<Int> = _userXp.asStateFlow()

    private val _solvedChallengeIds = MutableStateFlow<Set<String>>(emptySet())
    val solvedChallengeIds: StateFlow<Set<String>> = _solvedChallengeIds.asStateFlow()

    private val _savedCodes = MutableStateFlow<List<String>>(emptyList())
    val savedCodes: StateFlow<List<String>> = _savedCodes.asStateFlow()

    private val _activeChallenge = MutableStateFlow<CipherChallenge?>(ChallengeDatabase.challenges.firstOrNull())
    val activeChallenge: StateFlow<CipherChallenge?> = _activeChallenge.asStateFlow()

    private val _challengeMessage = MutableStateFlow<String?>(null)
    val challengeMessage: StateFlow<String?> = _challengeMessage.asStateFlow()

    // --- Git Version Control Engine States ---
    private val moshi = Moshi.Builder().add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory()).build()
    private val snapshotListType = Types.newParameterizedType(List::class.java, WorkspaceFileSnapshot::class.java)
    private val snapshotAdapter = moshi.adapter<List<WorkspaceFileSnapshot>>(snapshotListType)

    private val _gitInitialized = MutableStateFlow(false)
    val gitInitialized: StateFlow<Boolean> = _gitInitialized.asStateFlow()

    private val _gitRemoteUrl = MutableStateFlow("")
    val gitRemoteUrl: StateFlow<String> = _gitRemoteUrl.asStateFlow()

    private val _gitStagedPaths = MutableStateFlow<Set<String>>(emptySet())
    val gitStagedPaths: StateFlow<Set<String>> = _gitStagedPaths.asStateFlow()

    private val _gitPushedCommits = MutableStateFlow<Set<String>>(emptySet())
    val gitPushedCommits: StateFlow<Set<String>> = _gitPushedCommits.asStateFlow()

    private val _gitCommits = MutableStateFlow<List<GitCommitEntity>>(emptyList())
    val gitCommits: StateFlow<List<GitCommitEntity>> = _gitCommits.asStateFlow()

    init {
        // Observe Room DB file list
        var hasCheckedUpgrade = false
        viewModelScope.launch {
            workspaceRepository.allFiles.collect { files ->
                workspaceFiles.value = files
                if (files.isEmpty()) {
                    seedDefaultWorkspace()
                } else {
                    if (!hasCheckedUpgrade) {
                        hasCheckedUpgrade = true
                        val indexFile = files.find { it.path == "index.html" || it.name == "index.html" }
                        if (indexFile != null && !indexFile.content.contains("theme-btn")) {
                            upgradeExistingSeededWorkspace(files)
                        }
                    }
                    if (_selectedFileId.value == null) {
                        // Preselect first file on launch
                        val defaultFile = files.find { !it.isFolder } ?: files.firstOrNull()
                        defaultFile?.let { openFileInTab(it) }
                    }
                }
            }
        }

        // Collect local variables from DataStore (for profile matching)
        viewModelScope.launch {
            dataStoreManager.totalSolves.collect { _totalSolves.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.currentStreak.collect { _currentStreak.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.acquiredXp.collect { _userXp.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.solvedChallengeIds.collect { _solvedChallengeIds.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.savedCodes.collect { _savedCodes.value = it }
        }
        // Collect Git states
        viewModelScope.launch {
            dataStoreManager.gitInitialized.collect { _gitInitialized.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.gitRemoteUrl.collect { _gitRemoteUrl.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.gitStagedPaths.collect { _gitStagedPaths.value = it }
        }
        viewModelScope.launch {
            dataStoreManager.gitPushedCommits.collect { _gitPushedCommits.value = it }
        }
        viewModelScope.launch {
            workspaceRepository.allCommits.collect { _gitCommits.value = it }
        }
    }

    // --- Workspace Hierarchy Manipulation ---
    private suspend fun seedDefaultWorkspace() {
        val defaultFiles = listOf(
            WorkspaceFile(
                name = "README.md",
                path = "README.md",
                isFolder = false,
                content = """# 🌌 Welcome to the Cezix Cyber Decoder Wheel Space!

A lightweight, fully interactive circular CSS-based decoder wheel prototype. This interactive codespace serves as a visualization sandbox for Caesar cipher encoding and decoding.

### ✨ Dynamic Capabilities Highlight:
- 🎡 **Interactive Concentric Rotor Wheel**: Supports touch swipe drag interaction and slide step adjustments.
- 🔗 **Real-time Rotary Pathway Logger**: Traces character shifts step-by-step so you can inspect how alphabetical mapping functions.
- ⚡ **Local Compiler Web Live Sandbox**: Instant responsive WebView compiler frames rendering CSS, HTML, and JS in real-time.
- 🐞 **Live JS Log Inspector Console**: Custom console logger monitoring and displaying warnings or state tracebacks.

---
*💡 Prototype hack: Change the `--neon-blue` and `--neon-purple` color tokens in `style.css` to instantly customize your cyberpunk overlay theme!*
"""
            ),
            WorkspaceFile(
                name = "index.html",
                path = "index.html",
                isFolder = false,
                content = getUpgradedHtml()
            ),
            WorkspaceFile(
                name = "style.css",
                path = "style.css",
                isFolder = false,
                content = getUpgradedCss()
            ),
            WorkspaceFile(
                name = "script.js",
                path = "script.js",
                isFolder = false,
                content = getUpgradedJs()
            ),
            WorkspaceFile(
                name = "data.json",
                path = "data.json",
                isFolder = false,
                content = """{
  "editorName": "Ceezix Code Editor",
  "appTheme": "Dark Cyberpunk",
  "features": [
    "Workspace Explorer",
    "Live Preview Web Frame",
    "Debug Compiler Console",
    "Smart AI Copilot",
    "Clipboard Exporter"
  ],
  "author": "Ceezix Team",
  "version": "1.8.0"
}
"""
            )
        )
        workspaceRepository.insertFiles(defaultFiles)
    }

    fun openFileInTab(file: WorkspaceFile) {
        viewModelScope.launch {
            val fileId = file.id
            val currentTabs = _openTabIds.value.toMutableList()
            if (!currentTabs.contains(fileId)) {
                currentTabs.add(fileId)
                _openTabIds.value = currentTabs
            }
            // Save currently active file edit state to database first
            val lastActiveId = _selectedFileId.value
            if (lastActiveId != null) {
                workspaceRepository.updateFileContent(lastActiveId, editorContent.value)
            }
            
            _selectedFileId.value = fileId
            editorContent.value = file.content
        }
    }

    fun closeTab(fileId: Int) {
        viewModelScope.launch {
            val currentTabs = _openTabIds.value.toMutableList()
            if (currentTabs.contains(fileId)) {
                currentTabs.remove(fileId)
                _openTabIds.value = currentTabs
            }
            
            if (_selectedFileId.value == fileId) {
                // If closing selected, opt for next open tab or null
                if (currentTabs.isNotEmpty()) {
                    val nextId = currentTabs.last()
                    val nextFile = workspaceRepository.getFileById(nextId)
                    _selectedFileId.value = nextId
                    editorContent.value = nextFile?.content ?: ""
                } else {
                    _selectedFileId.value = null
                    editorContent.value = ""
                }
            }
        }
    }

    fun selectActiveTab(tabIndex: Int) {
        _activeTab.value = tabIndex
        if (tabIndex == 1) {
            // Recompile or assemble composite html output whenever switching to Browser Frame
            recompilePreviewData()
        }
    }

    fun saveActiveFileContent() {
        val activeId = _selectedFileId.value ?: return
        viewModelScope.launch {
            workspaceRepository.updateFileContent(activeId, editorContent.value)
            
            // Refresh files
            val updated = workspaceRepository.getFileById(activeId)
            updated?.let {
                val currentList = workspaceFiles.value.toMutableList()
                val idx = currentList.indexOfFirst { item -> item.id == activeId }
                if (idx >= 0) {
                    currentList[idx] = it
                    workspaceFiles.value = currentList
                }
            }
        }
    }

    fun createNewFileInWorkspace(fileName: String, isFolder: Boolean) {
        viewModelScope.launch {
            val extension = if (isFolder) "" else fileName.substringAfterLast(".", "")
            val defaultContent = when (extension) {
                "html" -> "<!-- New HTML Structure -->\n<!DOCTYPE html>\n<html>\n<head>\n  <title>New Single Page</title>\n</head>\n<body>\n  <h1>File Created!</h1>\n</body>\n</html>"
                "css" -> "/* Style sheet */\nbody {\n  margin: 0;\n  padding: 8px;\n  background: #121214;\n  color: #fff;\n}"
                "js" -> "// JS code block\nconsole.log(\"Hello from $fileName\");"
                "json" -> "{\n  \"name\": \"$fileName\",\n  \"status\": \"created\"\n}"
                else -> "# $fileName\nEnter your markdown contents here."
            }
            val path = fileName // keep it clean at base layer
            val newFile = WorkspaceFile(
                name = fileName,
                path = path,
                isFolder = isFolder,
                content = if (isFolder) "" else defaultContent
            )
            val insertedId = workspaceRepository.insertFile(newFile)
            if (!isFolder) {
                // Instantly open the newly created file in tab
                val insertedFile = workspaceRepository.getFileById(insertedId.toInt())
                insertedFile?.let { openFileInTab(it) }
            }
        }
    }

    fun deleteFileFromWorkspace(file: WorkspaceFile) {
        viewModelScope.launch {
            closeTab(file.id)
            workspaceRepository.deleteFile(file)
        }
    }

    fun clearCompleteConsole() {
        _consoleLogs.value = emptyList()
    }

    // --- Code Formatter Engine ---
    fun formatActiveFile() {
        val currentContent = editorContent.value
        val activeSelectedId = _selectedFileId.value ?: return
        val extension = workspaceFiles.value.find { it.id == activeSelectedId }?.name?.substringAfterLast(".", "") ?: ""
        if (currentContent.isBlank()) return
        
        val formatted = when (extension.lowercase()) {
            "html" -> formatHtml(currentContent)
            "js", "javascript" -> formatJavascript(currentContent)
            "css" -> formatCss(currentContent)
            "json" -> formatJson(currentContent)
            else -> formatGenericCode(currentContent)
        }
        
        editorContent.value = formatted
        saveActiveFileContent()
    }

    private fun formatHtml(content: String): String {
        val lines = content.split("\n")
        val result = StringBuilder()
        var indentLevel = 0
        val tab = "  "
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                result.appendLine()
                continue
            }
            
            // Decrease indent before printing if it's a closing tag
            val isClosing = trimmed.startsWith("</") || trimmed.startsWith("</html>") || trimmed.startsWith("</body>") || trimmed.startsWith("</head>")
            if (isClosing && indentLevel > 0) {
                indentLevel--
            }
            
            result.append(tab.repeat(indentLevel)).appendLine(trimmed)
            
            // Increase indent for next lines if it's an opening tag
            val isOpening = (trimmed.startsWith("<") && !trimmed.startsWith("</") && !trimmed.contains("/>") && !trimmed.startsWith("<!") &&
                            (trimmed.endsWith(">") || trimmed.contains(">"))) &&
                            !trimmed.startsWith("<img") && !trimmed.startsWith("<br") && !trimmed.startsWith("<input") && !trimmed.startsWith("<link") && !trimmed.startsWith("<meta")
            if (isOpening) {
                indentLevel++
            }
        }
        return result.toString().trim()
    }

    private fun formatJavascript(content: String): String {
        val lines = content.split("\n")
        val result = StringBuilder()
        var indentLevel = 0
        val tab = "  "
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                result.appendLine()
                continue
            }
            
            if (trimmed.startsWith("}") || trimmed.startsWith("]")) {
                if (indentLevel > 0) indentLevel--
            }
            
            result.append(tab.repeat(indentLevel)).appendLine(trimmed)
            
            if (trimmed.endsWith("{") || trimmed.endsWith("[")) {
                indentLevel++
            }
        }
        return result.toString().trim()
    }
    
    private fun formatCss(content: String): String {
        val lines = content.split("\n")
        val result = StringBuilder()
        var indentLevel = 0
        val tab = "  "
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) {
                result.appendLine()
                continue
            }
            
            if (trimmed.startsWith("}")) {
                if (indentLevel > 0) indentLevel--
            }
            
            result.append(tab.repeat(indentLevel)).appendLine(trimmed)
            
            if (trimmed.endsWith("{")) {
                indentLevel++
            }
        }
        return result.toString().trim()
    }

    private fun formatJson(content: String): String {
        return formatJavascript(content)
    }

    private fun formatGenericCode(content: String): String {
        val lines = content.split("\n")
        val result = StringBuilder()
        for (line in lines) {
            result.appendLine(line.trimEnd())
        }
        return result.toString().trim()
    }

    fun appendConsoleLog(type: String, message: String) {
        val current = _consoleLogs.value.toMutableList()
        current.add(ConsoleLogEntry(type, message))
        _consoleLogs.value = current
    }

    // --- HTML Live Preview virtual engine injector ---
    val compositeHtmlOutput = MutableStateFlow("")

    fun recompilePreviewData() {
        // Find index.html. If missing, look for first available html, or build dynamically
        val files = workspaceFiles.value
        val htmlFile = files.find { it.name.endsWith(".html") }
        if (htmlFile == null) {
            compositeHtmlOutput.value = """<html><body><h2>No HTML page found in workspace!</h2><p>Create an <code>index.html</code> to begin.</p></body></html>"""
            return
        }

        var bundledHtml = htmlFile.content
        if (_selectedFileId.value == htmlFile.id) {
            // Use current draft buffer so it previews in real-time without needing a manual disk save!
            bundledHtml = editorContent.value
        }

        // Retrieve secondary scripts/styles from database/editor drafts
        val cssFile = files.find { it.name.endsWith(".css") }
        var cssContent = cssFile?.content ?: ""
        if (cssFile != null && _selectedFileId.value == cssFile.id) {
            cssContent = editorContent.value
        }

        val jsFile = files.find { it.name.endsWith(".js") }
        var jsContent = jsFile?.content ?: ""
        if (jsFile != null && _selectedFileId.value == jsFile.id) {
            jsContent = editorContent.value
        }

        // To monitor console.log logs, we inject an elegant Javascript Interceptor overlay code block!
        val consoleInterceptorScript = """
            <script>
            (function() {
                var _log = console.log;
                var _warn = console.warn;
                var _error = console.error;
                var _info = console.info;
                
                function sendToApp(type, args) {
                    var msg = Array.prototype.slice.call(args).map(function(item) {
                        try {
                            return typeof item === 'object' ? JSON.stringify(item) : String(item);
                        } catch(e) { return String(item); }
                    }).join(' ');
                    // Trigger web-to-app bridge scheme or message
                    if (window.CezixConsoleBridge) {
                        window.CezixConsoleBridge.postMessage(JSON.stringify({type: type, message: msg}));
                    }
                    _log("INTECEPT[" + type + "]: " + msg);
                }
                
                console.log = function() {
                    sendToApp('log', arguments);
                    _log.apply(console, arguments);
                };
                console.warn = function() {
                    sendToApp('warn', arguments);
                    _warn.apply(console, arguments);
                };
                console.error = function() {
                    sendToApp('error', arguments);
                    _error.apply(console, arguments);
                };
                console.info = function() {
                    sendToApp('info', arguments);
                    _info.apply(console, arguments);
                };
                
                // Track global unhandled script failures
                window.onerror = function(message, source, lineno, colno, error) {
                    var errorMsg = message + " at line " + lineno;
                    if (window.CezixConsoleBridge) {
                        window.CezixConsoleBridge.postMessage(JSON.stringify({type: 'error', message: errorMsg}));
                    }
                };
            })();
            </script>
        """.trimIndent()

        // 1. Inject Console logger right at the head
        bundledHtml = if (bundledHtml.contains("<head>")) {
            bundledHtml.replace("<head>", "<head>\n$consoleInterceptorScript")
        } else {
            consoleInterceptorScript + "\n" + bundledHtml
        }

        // 2. Inject css inline or replace style.css link tags
        if (cssContent.isNotBlank()) {
            val inlineStyle = "<style>\n$cssContent\n</style>"
            // replace <link rel="stylesheet"...> or simple append
            val linkRegex = """<link\s+[^>]*href=["'][^"']*style\.css["'][^>]*>""".toRegex(RegexOption.IGNORE_CASE)
            bundledHtml = if (bundledHtml.contains(linkRegex)) {
                bundledHtml.replace(linkRegex, inlineStyle)
            } else if (bundledHtml.contains("</head>")) {
                bundledHtml.replace("</head>", "$inlineStyle\n</head>")
            } else {
                bundledHtml + "\n" + inlineStyle
            }
        }

        // 3. Inject javascript inline or replace script tag
        if (jsContent.isNotBlank()) {
            val inlineScript = "<script>\n$jsContent\n</script>"
            val scriptRegex = """<script\s+[^>]*src=["'][^"']*script\.js["'][^>]*>\s*</script>""".toRegex(RegexOption.IGNORE_CASE)
            bundledHtml = if (bundledHtml.contains(scriptRegex)) {
                bundledHtml.replace(scriptRegex, inlineScript)
            } else if (bundledHtml.contains("</body>")) {
                bundledHtml.replace("</body>", "$inlineScript\n</body>")
            } else {
                bundledHtml + "\n" + inlineScript
            }
        }

        compositeHtmlOutput.value = bundledHtml
    }

    // --- Cezix Gemini Assistant Code Integration ---
    fun askAiAboutCurrentCode(userCustomInstruction: String? = null) {
        val files = workspaceFiles.value
        val activeFileIdVal = _selectedFileId.value
        val activeFile = files.find { it.id == activeFileIdVal } ?: files.find { !it.isFolder } ?: return
        
        val activeContent = if (activeFile.id == activeFileIdVal) editorContent.value else activeFile.content
        val apiPrompt = buildString {
            appendLine("You are an Elite Assistant Developer and Programming Tutor integrated inside Cezix Code Editor.")
            appendLine("We are editing a code file named '${activeFile.name}'.")
            appendLine("Here is the current content of the file:")
            appendLine("```${activeFile.name.substringAfterLast(".", "")}")
            appendLine(activeContent)
            appendLine("```")
            appendLine()
            if (userCustomInstruction != null) {
                appendLine("The user has asked the following specific coding request: \"$userCustomInstruction\"")
            } else {
                appendLine("Please perform a review of this code. Explain its execution flow and provide any useful suggestions, code refactoring optimizations, or fixes for syntax defects.")
            }
            appendLine()
            appendLine("IMPORTANT DIRECTIONS:")
            appendLine("1. Format your response cleanly and visually.")
            appendLine("2. If suggesting code improvements, provide the optimized COMPLETE code replacement at the very end enclosed inside a single ``` code block (formatted with the file extension) so that Cezix Editor can auto-extract and instantly replace it!")
            appendLine("3. Avoid overly verbose handshakes. Be sharp, functional, and helpful.")
        }

        _isAiLoading.value = true
        _aiOutput.value = null

        viewModelScope.launch {
            val apiKey = io.ceezix.BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
                // Call robust offline editor companion
                val output = runOfflineCodeAssistant(activeFile, userCustomInstruction)
                _aiOutput.value = output
                _isAiLoading.value = false
            } else {
                try {
                    val request = io.ceezix.api.GenerateContentRequest(
                        contents = listOf(io.ceezix.api.Content(parts = listOf(io.ceezix.api.Part(text = apiPrompt)))),
                        generationConfig = io.ceezix.api.GenerationConfig(temperature = 0.3f),
                        systemInstruction = io.ceezix.api.Content(parts = listOf(io.ceezix.api.Part(text = "You are a professional Android Code Companion for the Cezix Editor.")))
                    )
                    val response = io.ceezix.api.RetrofitClient.service.generateContent(apiKey, request)
                    val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                        ?: "No suggestion generated. Try re-sending your instruction."
                    _aiOutput.value = responseText
                } catch (e: Exception) {
                    Log.e("CezixViewModel", "Gemini API failed in Code Companion", e)
                    _aiOutput.value = runOfflineCodeAssistant(activeFile, userCustomInstruction, errorMsg = e.localizedMessage)
                } finally {
                    _isAiLoading.value = false
                }
            }
        }
    }

    private fun runOfflineCodeAssistant(file: WorkspaceFile, request: String?, errorMsg: String? = null): String {
        val baseMsg = if (errorMsg != null) " (Network/API Error: $errorMsg)" else ""
        val keywordSearch = request?.lowercase() ?: ""
        
        val templateOutput = when {
            keywordSearch.contains("button") || keywordSearch.contains("interactive") -> """
<!-- Beautiful New Button Component -->
<button class="btn btn-interactive" onclick="console.log('Action tick!')">Interactive Ripple Event</button>
            """.trimIndent()
            keywordSearch.contains("flex") || keywordSearch.contains("grid") -> """
/* Responsive flex grid style sheet */
.grid-container {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: center;
  align-items: center;
}
            """.trimIndent()
            else -> """
// Suggested script refinement and optimization
console.log("Cezix code companion injected successful run block.");
            """.trimIndent()
        }

        return """
⚔️ CEZIX OFFLINE CODE COMPILATION ASSISTANT$baseMsg
======================================================
Your file '${file.name}' is matching standard syntax profiles.

[Assistant Review]
- **File Type**: ${file.name.substringAfterLast(".", "Standard text")}
- **Size**: ${editorContent.value.length} characters
- **Optimization Strategy**: Keep your closures clean, separate concerns into distinct helper script frames, and monitor interactive outcomes via the Built-In Review tab.

[Code Snippet Proposed]
```${file.name.substringAfterLast(".", "")}
$templateOutput
```
======================================================
*Tip: Configure your Gemini API key inside Google Studio secrets to unlock full high-context AI pairing!*
        """.trimIndent()
    }

    fun mergeAiProposedCode(suggestedText: String) {
        // Attempt to extract the last code block wrapped inside ``` or take suggestedText
        var finalCode = suggestedText
        val blockRegex = """```[\w]*\n([\s\S]*?)```""".toRegex()
        val matchResult = blockRegex.findAll(suggestedText).lastOrNull()
        if (matchResult != null) {
            val content = matchResult.groupValues[1]
            if (content.isNotBlank()) {
                finalCode = content
            }
        }
        
        // Inject into current editorContent
        editorContent.value = finalCode
        saveActiveFileContent()
        _aiOutput.value = "✅ Injected suggested layout successfully!"
    }

    // --- Persisted Progression Cipher Helpers (Preserved & Styled) ---
    fun selectChallenge(challenge: CipherChallenge) {
        _activeChallenge.value = challenge
        _challengeMessage.value = null
        inputText.value = challenge.ciphertext
        currentMode.value = challenge.cipherType
    }

    fun checkChallengeAnswer() {
        val currentChallenge = _activeChallenge.value ?: return
        val currentDecrypted = getProcessedOutput().trim().lowercase()

        val expectedDecrypted = when (currentChallenge.cipherType) {
            "Caesar" -> CipherEngine.decryptCaesar(currentChallenge.ciphertext, currentChallenge.expectedShift)
            "Atbash" -> CipherEngine.decryptAtbash(currentChallenge.ciphertext)
            "Vigenere" -> CipherEngine.decryptVigenere(currentChallenge.ciphertext, currentChallenge.expectedKey)
            else -> ""
        }.trim().lowercase()

        if (currentDecrypted == expectedDecrypted) {
            viewModelScope.launch {
                dataStoreManager.markChallengeSolved(currentChallenge.id, currentChallenge.xpReward)
                _challengeMessage.value = "SUCCESS: Decryption verified! Unlocked +${currentChallenge.xpReward} XP!"
            }
        } else {
            viewModelScope.launch {
                dataStoreManager.resetStreak()
                _challengeMessage.value = "DECRYPTION MISMATCH: Check your alignment key or shift value!"
            }
        }
    }

    fun getProcessedOutput(): String {
        val text = inputText.value
        return when (currentMode.value) {
            "Caesar" -> CipherEngine.decryptCaesar(text, currentShift.value)
            "ROT13" -> CipherEngine.decryptCaesar(text, 13)
            "Atbash" -> CipherEngine.decryptAtbash(text)
            "Vigenere" -> CipherEngine.decryptVigenere(text, vigenereKey.value)
            "Base64" -> CipherEngine.decryptBase64(text)
            else -> text
        }
    }

    fun getProcessedOutputEncrypted(): String {
        val text = inputText.value
        return when (currentMode.value) {
            "Caesar" -> CipherEngine.encryptCaesar(text, currentShift.value)
            "ROT13" -> CipherEngine.encryptCaesar(text, 13)
            "Atbash" -> CipherEngine.encryptAtbash(text)
            "Vigenere" -> CipherEngine.encryptVigenere(text, vigenereKey.value)
            "Base64" -> CipherEngine.encryptBase64(text)
            else -> text
        }
    }

    fun analyzeWithGemini(clues: String) {
        val cipherToAnalyze = inputText.value
        if (cipherToAnalyze.isBlank()) return

        _isAiLoading.value = true
        _aiOutput.value = null

        viewModelScope.launch {
            val report = geminiRepository.analyzeCipher(cipherToAnalyze, clues)
            _aiOutput.value = report
            _isAiLoading.value = false
        }
    }

    fun saveCurrentToNotebook() {
        viewModelScope.launch {
            val raw = inputText.value
            val dec = getProcessedOutput()
            val shift = if (currentMode.value == "Caesar") currentShift.value else if (currentMode.value == "ROT13") 13 else 0
            dataStoreManager.saveCode(raw, dec, shift)
        }
    }

    fun deleteFromNotebook(entry: String) {
        viewModelScope.launch {
            dataStoreManager.deleteCode(entry)
        }
    }

    fun injectDependency(language: String, packageName: String, version: String, templateCode: String) {
        viewModelScope.launch {
            val files = workspaceFiles.value
            
            if (language.lowercase() == "node") {
                // Find or create package.json
                val packageJsonFile = files.find { it.name.lowercase() == "package.json" }
                val currentPackageJson = packageJsonFile?.content ?: """{
  "name": "ceezix-sandbox-app",
  "version": "1.0.0",
  "description": "Cezix sandbox compiled offline application",
  "main": "script.js",
  "dependencies": {}
}"""
                
                // Parse and inject dependency into json string manually to keep it super solid
                val updatedJson = try {
                    if (currentPackageJson.contains("\"dependencies\":")) {
                        if (currentPackageJson.contains("\"$packageName\"")) {
                            currentPackageJson
                        } else {
                            currentPackageJson.replace(
                                "\"dependencies\": {",
                                "\"dependencies\": {\n    \"$packageName\": \"$version\","
                            )
                        }
                    } else {
                        currentPackageJson.replace(
                            "}",
                            ",\n  \"dependencies\": {\n    \"$packageName\": \"$version\"\n  }\n}"
                        )
                    }
                } catch (e: Exception) {
                    currentPackageJson
                }

                if (packageJsonFile != null) {
                    workspaceRepository.updateFileContent(packageJsonFile.id, updatedJson)
                } else {
                    val newFile = WorkspaceFile(
                        name = "package.json",
                        path = "package.json",
                        isFolder = false,
                        content = updatedJson
                    )
                    workspaceRepository.insertFile(newFile)
                }

                // In Node, inject helper template into script.js or active JS tab
                val scriptJsFile = files.find { it.name.lowercase() == "script.js" }
                val divider = "\n\n// ==========================================\n// 📦 INJECTED MODULE: $packageName\n// ==========================================\n"
                
                if (scriptJsFile != null) {
                    val activeId = _selectedFileId.value
                    if (activeId == scriptJsFile.id) {
                        editorContent.value = editorContent.value + divider + templateCode
                        saveActiveFileContent()
                    } else {
                        workspaceRepository.updateFileContent(scriptJsFile.id, scriptJsFile.content + divider + templateCode)
                        openFileInTab(scriptJsFile)
                    }
                } else {
                    val newFile = WorkspaceFile(
                        name = "script.js",
                        path = "script.js",
                        isFolder = false,
                        content = templateCode
                    )
                    val insertedId = workspaceRepository.insertFile(newFile)
                    val insertedFile = workspaceRepository.getFileById(insertedId.toInt())
                    insertedFile?.let { openFileInTab(it) }
                }

            } else if (language.lowercase() == "python") {
                // Find or create requirements.txt
                val reqTxtFile = files.find { it.name.lowercase() == "requirements.txt" }
                val currentReqContent = reqTxtFile?.content ?: ""
                
                val updatedReqs = if (currentReqContent.contains(packageName)) {
                    currentReqContent
                } else {
                    if (currentReqContent.isEmpty()) "$packageName$version" else "$currentReqContent\n$packageName$version"
                }

                if (reqTxtFile != null) {
                    workspaceRepository.updateFileContent(reqTxtFile.id, updatedReqs)
                } else {
                    val newFile = WorkspaceFile(
                        name = "requirements.txt",
                        path = "requirements.txt",
                        isFolder = false,
                        content = updatedReqs
                    )
                    workspaceRepository.insertFile(newFile)
                }

                // In Python, inject helper template into app.py or script.py or a dedicated demo
                val pyFile = files.find { it.name.endsWith(".py") } ?: files.find { it.name.lowercase() == "script.py" }
                val divider = "\n\n# ==========================================\n# 📦 INJECTED MODULE: $packageName\n# ==========================================\n"
                
                if (pyFile != null) {
                    val activeId = _selectedFileId.value
                    if (activeId == pyFile.id) {
                        editorContent.value = editorContent.value + divider + templateCode
                        saveActiveFileContent()
                    } else {
                        workspaceRepository.updateFileContent(pyFile.id, pyFile.content + divider + templateCode)
                        openFileInTab(pyFile)
                    }
                } else {
                    val newFile = WorkspaceFile(
                        name = "app.py",
                        path = "app.py",
                        isFolder = false,
                        content = templateCode
                    )
                    val insertedId = workspaceRepository.insertFile(newFile)
                    val insertedFile = workspaceRepository.getFileById(insertedId.toInt())
                    insertedFile?.let { openFileInTab(it) }
                }
            }
        }
    }

    fun loadProjectTemplate(templateName: String) {
        viewModelScope.launch {
            workspaceRepository.clearWorkspace()
            _openTabIds.value = emptyList()
            _selectedFileId.value = -1
            
            val filesToCreate = when (templateName) {
                "React CDN Sandbox" -> listOf(
                    WorkspaceFile(
                        name = "index.html",
                        path = "index.html",
                        isFolder = false,
                        content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>React Sandbox Hub</title>
    <!-- CDN-based React, ReactDOM, Babel, and TailwindCSS for live browser compiles -->
    <script src="https://unpkg.com/react@18/umd/react.development.js" crossorigin></script>
    <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js" crossorigin></script>
    <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        body {
            background-color: #0f172a;
            color: #f1f5f9;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
        }
    </style>
</head>
<body>
    <div id="react-root"></div>
    <script type="text/babel" src="script.js"></script>
</body>
</html>"""
                    ),
                    WorkspaceFile(
                        name = "script.js",
                        path = "script.js",
                        isFolder = false,
                        content = """// React 18 live functional template inside Ceezix Sandbox
const { useState, useEffect } = React;

function ReactDashboard() {
    const [clicks, setClicks] = useState(0);
    const [rotValue, setRotValue] = useState(3);
    const [plainText, setPlainText] = useState("HELLO REACT");
    const [cipherText, setCipherText] = useState("");

    // Caesar Shift logic inside React Component!
    useEffect(() => {
        let output = "";
        for (let i = 0; i < plainText.length; i++) {
            let code = plainText.charCodeAt(i);
            if (code >= 65 && code <= 90) {
                output += String.fromCharCode(((code - 65 + rotValue) % 26) + 65);
            } else if (code >= 97 && code <= 122) {
                output += String.fromCharCode(((code - 97 + rotValue) % 26) + 97);
            } else {
                output += plainText[i];
            }
        }
        setCipherText(output);
    }, [plainText, rotValue]);

    return (
        <div className="max-w-md mx-auto p-6 bg-slate-900 rounded-2xl shadow-xl border border-slate-800 text-center mt-6">
            <h1 className="text-2xl font-black text-cyan-400 mb-2">⚡ OFFICIAL REACT SANDBOX</h1>
            <p className="text-slate-400 text-xs mb-6">Fully compiled client-side via React 18, Babel, and Tailwind</p>
            
            <div className="bg-slate-950 p-4 rounded-xl border border-slate-800/80 mb-6 text-left">
                <label className="text-[10px] font-bold tracking-wider text-cyan-500 block mb-1">INPUT TEXT TO ENCODE</label>
                <input 
                    type="text" 
                    value={plainText} 
                    onChange={(e) => setPlainText(e.target.value)} 
                    className="w-full bg-slate-900 border border-slate-700 rounded p-2 text-sm text-white focus:outline-none focus:border-cyan-400"
                />
            </div>

            <div className="grid grid-cols-2 gap-4 mb-6">
                <div className="bg-slate-950 p-3 rounded-lg border border-slate-800">
                    <span className="text-[9px] text-slate-500 font-bold block">SHIFT CYCLES</span>
                    <input 
                        type="number" 
                        min="0" 
                        max="25"
                        value={rotValue} 
                        onChange={(e) => setRotValue(parseInt(e.target.value) || 0)} 
                        className="w-16 bg-transparent text-center text-xl font-bold text-white focus:outline-none"
                    />
                </div>
                <div className="bg-slate-950 p-3 rounded-lg border border-slate-800 flex flex-col justify-center items-center">
                    <span className="text-[9px] text-slate-500 font-bold block">INTERACT CLICKS</span>
                    <button 
                        onClick={() => setClicks(clicks + 1)} 
                        className="bg-cyan-500 hover:bg-cyan-600 px-3 py-1 rounded text-xs font-bold text-slate-950 transition-all mt-1"
                    >
                        Active: {clicks}
                    </button>
                </div>
            </div>

            <div className="bg-cyan-950/40 p-4 rounded-xl border border-cyan-800 text-left">
                <span className="text-[10px] font-bold tracking-wider text-cyan-400 block mb-1">CIPHER RE-ROUTEED TRANSMISSION</span>
                <p className="font-mono text-cyan-300 font-extrabold break-all">{cipherText || "..."}</p>
            </div>
        </div>
    );
}

const root = ReactDOM.createRoot(document.getElementById('react-root'));
root.render(<ReactDashboard />);"""
                    ),
                    WorkspaceFile(
                        name = "style.css",
                        path = "style.css",
                        isFolder = false,
                        content = """/* Custom ambient styles for the React Sandbox */
body {
  background: radial-gradient(circle at center, #1e1b4b 0%, #0f172a 100%);
  min-height: 100vh;
  padding: 20px;
}"""
                    ),
                    WorkspaceFile(
                        name = "README.md",
                        path = "README.md",
                        isFolder = false,
                        content = """# ⚛️ React 18 CDN Live Compiler

This static website template uses an in-browser Babel client-side transformer to compile and execute modern React components on page load. Perfect for rapid sandboxed UI prototyping.

- **Fast Rendering**: Full React state synchronization.
- **Micro UI Components**: Real-time Interactive Caesar Shifter component built using standard hooks (`useState`, `useEffect`).
- **Styling**: Pre-loaded tailwindcss framework from unpkg official library.
"""
                    )
                )

                "Static Website Portfolio" -> listOf(
                    WorkspaceFile(
                        name = "index.html",
                        path = "index.html",
                        isFolder = false,
                        content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Ceezix Static Web Portfolio</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <header class="hero-header">
        <div class="logo">✦ Ceezix Creative</div>
        <h1>Deploying Static Websites on Edge Networks</h1>
        <p>A beautifully optimized HTML5 & CSS Grid template built with modern aesthetic values.</p>
        <button id="cta-btn" class="glow-btn">Interact with Matrix</button>
    </header>

    <main class="grid-container">
        <div class="card bg-matte">
            <span class="badge">Performance</span>
            <h3>Static Site Architecture</h3>
            <p>Pre-rendered markup caches instantly on global CDN edges, minimizing time-to-first-byte.</p>
        </div>
        <div class="card bg-gradient">
            <span class="badge">Security</span>
            <h3>No Database Dependencies</h3>
            <p>A pure static design lacks databases, reducing typical web injection attack surfaces to absolute zero.</p>
        </div>
        <div class="card bg-matte">
            <span class="badge">Toolchain</span>
            <h3>NPM Build Integrations</h3>
            <p>Easily package using Webpack, Rollup, or Vite compiler systems into streamlined assets.</p>
        </div>
    </main>

    <footer class="text-center">
        <p>© 2026 Ceezix Official Templates. Live Interactive Canvas active.</p>
    </footer>

    <script src="script.js"></script>
</body>
</html>"""
                    ),
                    WorkspaceFile(
                        name = "style.css",
                        path = "style.css",
                        isFolder = false,
                        content = """/* Responsive CSS Grid & Layout variables for Static Site */
:root {
  --primary-glow: #3b82f6;
  --secondary-glow: #22c55e;
  --bg-dark: #090d16;
  --card-bg: #151b2d;
  --text-white: #f8fafc;
  --text-gray: #94a3b8;
}

body {
  background-color: var(--bg-dark);
  color: var(--text-white);
  font-family: system-ui, -apple-system, sans-serif;
  margin: 0;
  padding: 0;
  min-height: 100vh;
}

.hero-header {
  text-align: center;
  padding: 80px 20px;
  background: radial-gradient(circle at 50% 120%, rgba(59, 130, 246, 0.15), transparent);
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.logo {
  font-size: 14px;
  text-transform: uppercase;
  letter-spacing: 2px;
  color: var(--primary-glow);
  font-weight: 800;
  margin-bottom: 20px;
}

h1 {
  font-size: 32px;
  font-weight: 900;
  letter-spacing: -1px;
  margin: 0 0 16px 0;
  background: linear-gradient(135deg, #ffffff, #64748b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

p {
  color: var(--text-gray);
  font-size: 15px;
  max-width: 500px;
  margin: 0 auto;
  line-height: 1.6;
}

.glow-btn {
  background: linear-gradient(135deg, var(--primary-glow), #1d4ed8);
  color: white;
  border: none;
  padding: 12px 28px;
  font-size: 13px;
  font-weight: 700;
  border-radius: 50px;
  margin-top: 30px;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(59, 130, 246, 0.35);
  transition: all 0.3s ease;
}

.glow-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(59, 130, 246, 0.5);
}

.grid-container {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 20px;
  max-width: 1000px;
  margin: 40px auto;
  padding: 0 20px;
}

.card {
  background-color: var(--card-bg);
  border: 1px solid rgba(255, 255, 255, 0.03);
  border-radius: 16px;
  padding: 24px;
  transition: all 0.25s ease;
}

.card:hover {
  transform: translateY(-4px);
  border-color: rgba(59, 130, 246, 0.2);
}

.badge {
  font-size: 9px;
  font-weight: 800;
  text-transform: uppercase;
  letter-spacing: 1px;
  color: var(--primary-glow);
  background: rgba(59, 130, 246, 0.1);
  padding: 4px 8px;
  border-radius: 4px;
}

h3 {
  font-size: 18px;
  font-weight: 700;
  margin: 16px 0 8px 0;
}

.bg-gradient {
  background: linear-gradient(135deg, #1e293b, #0f172a, #152238);
}

footer {
  padding: 40px 20px;
  color: var(--text-gray);
  font-size: 11px;
  border-top: 1px solid rgba(255, 255, 255, 0.03);
}"""
                    ),
                    WorkspaceFile(
                        name = "script.js",
                        path = "script.js",
                        isFolder = false,
                        content = """// Interactive Web Portfolio Script
const ctaBtn = document.getElementById('cta-btn');

ctaBtn.addEventListener('click', () => {
    console.log("Interactive Matrix triggered!");
    
    // Add flashing matrix logs
    const lines = [
        "Initializing core layout build...",
        "Resolving static route dependencies...",
        "Pre-rendering DOM elements with strict typography...",
        "Compile success! Site fully loaded offline on Ceezix Edge client."
    ];
    
    lines.forEach((line, index) => {
        setTimeout(() => {
            console.log("⚡ [STAGE " + (index+1) + "]: " + line);
        }, index * 400);
    });

    ctaBtn.textContent = "Matrix Online ✅";
    ctaBtn.style.background = "linear-gradient(135deg, #22c55e, #15803d)";
    ctaBtn.style.boxShadow = "0 4px 14px rgba(34, 197, 94, 0.35)";
});"""
                    )
                )

                "NPM Module library" -> listOf(
                    WorkspaceFile(
                        name = "package.json",
                        path = "package.json",
                        isFolder = false,
                        content = """{
  "name": "ceezix-cryptokit",
  "version": "1.0.4",
  "description": "An official lightweight cryptographic utility library module for Caesar shifts and validation.",
  "main": "index.js",
  "scripts": {
    "test": "node test.js",
    "build": "node build.js"
  },
  "keywords": [
    "caesar",
    "cipher",
    "cryptography",
    "node-module"
  ],
  "author": "Ceezix core team",
  "license": "MIT",
  "dependencies": {
    "lodash": "^4.17.21"
  }
}"""
                    ),
                    WorkspaceFile(
                        name = "index.js",
                        path = "index.js",
                        isFolder = false,
                        content = """// Ceezix Cryptographic Library Core Code Export
const _ = require('lodash');

/**
 * Perform a Caesar Shift of alphabetical text characters
 * @param {string} text - Plaintext to process 
 * @param {number} shift - Distance indices to shift (0-25)
 * @returns {string} - Output ciphertext
 */
function cipherShift(text, shift) {
    if (!text) return "";
    const cleanShift = ((shift % 26) + 26) % 26;
    
    return text.split('').map(char => {
        const code = char.charCodeAt(0);
        if (code >= 65 && code <= 90) {
            return String.fromCharCode(((code - 65 + cleanShift) % 26) + 65);
        } else if (code >= 97 && code <= 122) {
            return String.fromCharCode(((code - 97 + cleanShift) % 26) + 97);
        }
        return char;
    }).join('');
}

module.exports = {
    cipherShift,
    version: "1.0.4"
};"""
                    ),
                    WorkspaceFile(
                        name = "test.js",
                        path = "test.js",
                        isFolder = false,
                        content = """// Modular Package Test Script
const cryptokit = require('./index');

console.log("--- RUNNING UNIT TESTS FOR CEEZIX CRYPTOKIT ---");

const testCases = [
    { input: "HELLO WORLD", shift: 3, expected: "KHOOR ZRUOG" },
    { input: "caesar", shift: 1, expected: "dbftbs" },
    { input: "ceezix-test-suite", shift: 0, expected: "ceezix-test-suite" }
];

testCases.forEach((tc, idx) => {
    const result = cryptokit.cipherShift(tc.input, tc.shift);
    const passed = result === tc.expected;
    console.log(`Test #` + (idx + 1) + `: Input "` + tc.input + `" | Shift ` + tc.shift + ` -> Result "` + result + `" [` + (passed ? "PASS ✅" : "FAIL ❌") + `]`);
});"""
                    ),
                    WorkspaceFile(
                        name = "build.js",
                        path = "build.js",
                        isFolder = false,
                        content = """// Custom minimal build compiler template for official modules
console.log("Starting custom build build task runner pipeline...");
console.log("Bundling assets, reading package.json and verifying checksum standards...");

setTimeout(() => {
    console.log("Minifying index.js file... OK");
    console.log("Output bundle successfully created in /dist/cryptokit-min.js.");
    console.log("Build pipeline finished cleanly! ✅");
}, 700);"""
                    )
                )

                "Vite & Webpack Configs" -> listOf(
                    WorkspaceFile(
                        name = "vite.config.js",
                        path = "vite.config.js",
                        isFolder = false,
                        content = """// Vite Bundler and Module Optimizer Configuration
import { defineConfig } from 'vite';

export default defineConfig({
  root: './',
  base: '/',
  build: {
    outDir: 'dist',
    minify: 'terser',
    sourcemap: true,
    cssCodeSplit: true,
    rollupOptions: {
      input: {
        main: './index.html'
      },
      output: {
        entryFileNames: 'assets/[name].[hash].js',
        chunkFileNames: 'assets/[name].[hash].js',
        assetFileNames: 'assets/[name].[hash].[ext]'
      }
    }
  },
  server: {
    port: 5173,
    open: true,
    strictPort: true
  }
});"""
                    ),
                    WorkspaceFile(
                        name = "webpack.config.js",
                        path = "webpack.config.js",
                        isFolder = false,
                        content = """// Webpack Core Asset Optimization Pipeline
const path = require('path');

module.exports = {
  entry: './script.js',
  output: {
    filename: 'bundle.js',
    path: path.resolve(__dirname, 'dist'),
    clean: true
  },
  mode: 'production',
  module: {
    rules: [
      {
        test: /\.css$/i,
        use: ['style-loader', 'css-loader'],
      },
    ],
  },
};"""
                    ),
                    WorkspaceFile(
                        name = "README.md",
                        path = "README.md",
                        isFolder = false,
                        content = """# ⚡ Bundlers Build Automation Templates

This workbench holds standard compiler settings configurations for **Vite** and **Webpack**.

- `vite.config.js`: Modern lightning-fast build using dynamic ESM.
- `webpack.config.js`: Production-ready bundling featuring tree shaking and style sheet chunk splitting.
"""
                    )
                )

                "Express Backend Server" -> listOf(
                    WorkspaceFile(
                        name = "server.js",
                        path = "server.js",
                        isFolder = false,
                        content = """// Express.js official Backend Router & APIs Template
const express = require('express');
const app = express();
const PORT = 8080;

app.use(express.json());

// Mock local key database persistence
let activeTransmissions = [
    { id: 1, text: "KHOOR", shift: 3, timestamp: "2026-06-05T02:00:00Z" },
    { id: 2, text: "Uifsf", shift: 1, timestamp: "2026-06-05T02:05:00Z" }
];

// 1. Health status routing
app.get('/api/health', (req, res) => {
    res.json({
        status: "Running",
        environment: "Ceezix Sandbox Server",
        timestamp: new Date().toISOString()
    });
});

// 2. Fetch list of recent cryptographic transmissions
app.get('/api/transmissions', (req, res) => {
    res.json(activeTransmissions);
});

// 3. Store a new encrypted message transmission
app.post('/api/transmissions', (req, res) => {
    const { text, shift } = req.body;
    if (!text || shift === undefined) {
        return res.status(400).json({ error: "Missing required parameters: text, shift" });
    }
    const newRecord = {
        id: activeTransmissions.length + 1,
        text,
        shift: parseInt(shift),
        timestamp: new Date().toISOString()
    };
    activeTransmissions.push(newRecord);
    res.status(201).json({
        success: true,
        message: "Key transmission recorded securely",
        record: newRecord
    });
});

app.listen(PORT, () => {
    console.log("==================================================");
    console.log(`🚀 CEEZIX EXPRESS SERVER ON PORT ` + PORT);
    console.log(`Explore live services at: http://localhost:` + PORT + `/api/health`);
    console.log("==================================================");
});"""
                    ),
                    WorkspaceFile(
                        name = "package.json",
                        path = "package.json",
                        isFolder = false,
                        content = """{
  "name": "ceezix-backend",
  "version": "1.0.0",
  "description": "Express backend routes simulation",
  "main": "server.js",
  "dependencies": {
    "express": "^4.18.2"
  }
}"""
                    ),
                    WorkspaceFile(
                        name = "README.md",
                        path = "README.md",
                        isFolder = false,
                        content = """# 🌐 Express API Server Template

An elegant REST api server. Contains JSON body parsers, mock resource routing in memory, and visual log outputs when requests arrive.
"""
                    )
                )

                "Caesar Decoder Wheel" -> getCaesarWheelTemplateMap().map { (name, content) ->
                    WorkspaceFile(name = name, path = name, isFolder = false, content = content)
                }
                "Matrix Digital Rain" -> getMatrixRainTemplateMap().map { (name, content) ->
                    WorkspaceFile(name = name, path = name, isFolder = false, content = content)
                }
                "Vigenère Shifting Grid" -> getVigenereTemplateMap().map { (name, content) ->
                    WorkspaceFile(name = name, path = name, isFolder = false, content = content)
                }
                "Hex & Binary Console" -> getBinaryConsoleTemplateMap().map { (name, content) ->
                    WorkspaceFile(name = name, path = name, isFolder = false, content = content)
                }
                else -> emptyList()
            }
            
            if (filesToCreate.isNotEmpty()) {
                workspaceRepository.insertFiles(filesToCreate)
                
                val targetPath = when (templateName) {
                    "React CDN Sandbox" -> "index.html"
                    "Static Website Portfolio" -> "index.html"
                    "NPM Module library" -> "index.js"
                    "Vite & Webpack Configs" -> "vite.config.js"
                    "Express Backend Server" -> "server.js"
                    else -> "index.html"
                }
                
                val dbFile = workspaceRepository.getFileByPath(targetPath)
                dbFile?.let { openFileInTab(it) }
                
                recompilePreviewData()
                appendConsoleLog("system", "Project Template '$templateName' successfully initialized in Workspace! 🚀")
            }
        }
    }

    fun injectReactComponent(componentName: String, code: String) {
        viewModelScope.launch {
            val files = workspaceFiles.value
            val scriptFile = files.find { it.name == "script.js" }
            if (scriptFile != null) {
                workspaceRepository.updateFileContent(scriptFile.id, code)
                if (_selectedFileId.value == scriptFile.id) {
                    editorContent.value = code
                }
                appendConsoleLog("system", "React Component '$componentName' successfully injected into script.js! ⚛️")
            } else {
                val newHtml = WorkspaceFile(
                    name = "index.html",
                    path = "index.html",
                    isFolder = false,
                    content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>React Component Sandbox</title>
    <!-- CDN-based React, ReactDOM, Babel, and TailwindCSS for live browser compiles -->
    <script src="https://unpkg.com/react@18/umd/react.development.js" crossorigin></script>
    <script src="https://unpkg.com/react-dom@18/umd/react-dom.development.js" crossorigin></script>
    <script src="https://unpkg.com/@babel/standalone/babel.min.js"></script>
    <script src="https://cdn.tailwindcss.com"></script>
    <style>
        body {
            background-color: #0f172a;
            color: #f1f5f9;
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            min-height: 100vh;
            padding: 20px;
        }
    </style>
</head>
<body>
    <div id="react-root"></div>
    <script type="text/babel" src="script.js"></script>
</body>
</html>"""
                )
                val newScript = WorkspaceFile(
                    name = "script.js",
                    path = "script.js",
                    isFolder = false,
                    content = code
                )
                val newCss = WorkspaceFile(
                    name = "style.css",
                    path = "style.css",
                    isFolder = false,
                    content = """/* Custom ambient styles for the React Sandbox */
body {
  background: radial-gradient(circle at center, #1e1b4b 0%, #0f172a 100%);
  min-height: 100vh;
  padding: 20px;
}"""
                )
                workspaceRepository.insertFiles(listOf(newHtml, newScript, newCss))
                
                // Fetch the newly created file & set tab active
                val dbFile = workspaceRepository.getFileByPath("script.js")
                dbFile?.let { openFileInTab(it) }
                
                appendConsoleLog("system", "React environment initialized & component '$componentName' injected into script.js! ⚛️")
            }
            recompilePreviewData()
        }
    }

    fun quickResetProgress() {
        viewModelScope.launch {
            dataStoreManager.clearAllData()
            _solvedChallengeIds.value = emptySet()
            _totalSolves.value = 0
            _currentStreak.value = 0
            _userXp.value = 0
            _savedCodes.value = emptyList()
            workspaceRepository.clearWorkspace()
            seedDefaultWorkspace()
        }
    }

    private suspend fun upgradeExistingSeededWorkspace(files: List<WorkspaceFile>) {
        val indexFile = files.find { it.path == "index.html" || it.name == "index.html" }
        val styleFile = files.find { it.path == "style.css" || it.name == "style.css" }
        val scriptFile = files.find { it.path == "script.js" || it.name == "script.js" }
        
        indexFile?.let {
            workspaceRepository.updateFileContent(it.id, getUpgradedHtml())
        }
        styleFile?.let {
            workspaceRepository.updateFileContent(it.id, getUpgradedCss())
        }
        scriptFile?.let {
            workspaceRepository.updateFileContent(it.id, getUpgradedJs())
        }
        Log.d("WorkspaceUpgrade", "Legacy Caesar sandbox upgraded with themes and tactile easing!")
    }

    private fun getUpgradedHtml(): String {
        return """<!DOCTYPE html>
<html lang="en">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cezix Cyber Decoder Wheel</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="app-container">
        <header>
            <span class="cyber-badge">CEZIX DECRYPTOR v1.8</span>
            <h1>🌌 CAESAR DECODER WHEEL</h1>
            <p class="subtitle">Concentric Rotary Shift Visualization & Interactive Sandbox</p>
        </header>

        <main class="main-layout">
            <!-- Left Side: Interactive Circular Decoder Wheel -->
            <div class="wheel-panel">
                <!-- Theme selection control for aesthetic wheel vibes -->
                <div class="theme-selector-container">
                    <span class="control-label">AESTHETIC SCHEME</span>
                    <div class="theme-options">
                        <button class="theme-btn active" data-theme="default">CYBER NEON</button>
                        <button class="theme-btn" data-theme="ubuntu">UBUNTU</button>
                        <button class="theme-btn" data-theme="vscode">VS CODE</button>
                        <button class="theme-btn" data-theme="github">GITHUB</button>
                        <button class="theme-btn" data-theme="minimalist">MINIMALIST</button>
                        <button class="theme-btn" data-theme="antique">ANTIQUE</button>
                    </div>
                </div>

                <div class="wheel-container">
                    <!-- SVG Interactive concentric wheel system -->
                    <svg id="decoder-svg" viewBox="0 0 320 320" width="100%" height="100%">
                        <!-- Outer Static Ambient Grid Glow -->
                        <circle cx="160" cy="160" r="148" class="grid-ring outer-glow-ring" />
                        <!-- Outer static boundary scale -->
                        <circle cx="160" cy="160" r="145" class="grid-ring border-ring" />
                        <!-- Inner rotating boundary circle -->
                        <circle cx="160" cy="160" r="105" class="grid-ring mid-ring" />
                        <!-- Core background disk -->
                        <circle cx="160" cy="160" r="65" class="core-disk" />
                        <circle cx="160" cy="160" r="62" class="core-ring" />

                        <!-- Dynamic Outer Ring Plaintext Letters Group (A-Z) -->
                        <g id="outer-wheel-letters" class="letters-group outer-letters"></g>

                        <!-- Dynamic Inner Rotating Cipher Letters Group -->
                        <g id="inner-wheel" class="letters-group inner-letters">
                            <g id="inner-wheel-letters"></g>
                        </g>

                        <!-- Pointer Arrow at the top indicating 0/key alignment -->
                        <polygon points="160,32 153,46 167,46" class="top-pointer-arrow" />
                        
                        <!-- Core Status Indicator -->
                        <text x="160" y="153" class="core-title-text">SHIFT KEY</text>
                        <text x="160" y="180" id="core-shift-val" class="core-value-text">0</text>
                    </svg>
                </div>
                
                <div class="wheel-controls">
                    <p class="control-label">ROTATE WHEEL OR CHOOSE SHIFT SHIFT</p>
                    <div class="slider-row">
                        <button id="btn-shift-down" class="btn-step">&#9664;</button>
                        <input id="shift-slider" type="range" min="0" max="25" value="0" class="neon-slider" />
                        <button id="btn-shift-up" class="btn-step">&#9654;</button>
                    </div>
                </div>
            </div>

            <!-- Right Side: Live Cipher Translator Sandbox -->
            <div class="translator-panel">
                <div class="toggle-mode-container">
                    <button id="mode-encrypt" class="btn-mode active">ENCRYPT (PLAINTEXT &rarr; CIPHER)</button>
                    <button id="mode-decrypt" class="btn-mode">DECRYPT (CIPHER &rarr; PLAINTEXT)</button>
                </div>

                <div class="text-boxes-container">
                    <div class="input-container">
                        <label for="text-input">INPUT STRING TEXT:</label>
                        <textarea id="text-input" placeholder="Type here to translate in real-time..."></textarea>
                    </div>

                    <div class="output-container">
                        <label>CONVERTED SECURE STREAM:</label>
                        <div id="text-output" class="result-box-stream">Waiting for transmission...</div>
                    </div>
                </div>

                <!-- Letter Highlight Visualization Track -->
                <div id="character-trail" class="visual-trail-track">
                    <span class="trail-heading">REAL-TIME ROTARY SHIFT PATHWAY:</span>
                    <div id="trail-mapping" class="trail-mapping-grid">
                        <div class="empty-trail">Introduce input text to trace shift paths</div>
                    </div>
                </div>
            </div>
        </main>

        <footer>
            <p>Interactive Cryptography Sandbox &copy; Cezix Core. Swipe, slide, and rotate segments dynamically.</p>
        </footer>
    </div>
    
    <script src="script.js"></script>
</body>
</html>"""
    }

    private fun getUpgradedCss(): String {
        return """/* Cezix Editor Cyber-Dark Stylesheet with Theme Engine & Tactile Easing */
:root {
    --bg-dark: #0a0e17;
    --panel-bg: #111625;
    --neon-blue: #45f3ff;
    --neon-teal: #66fcf1;
    --neon-purple: #bd5eff;
    --white: #ffffff;
    --text-muted: #8c92ac;
    --border-color: rgba(69, 243, 255, 0.25);
    --border-glow: 0 0 10px rgba(69, 243, 255, 0.15);
    --panel-border: 1.5px solid var(--border-color);
    --font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "JetBrains Mono", sans-serif;
    --wheel-bg: #0f1322;
    --wheel-core: #0c0f1b;
}

body {
    margin: 0;
    padding: 0;
    background-color: var(--bg-dark);
    color: var(--white);
    font-family: var(--font-family);
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100vh;
    transition: background-color 0.4s ease;
}

.app-container {
    max-width: 950px;
    width: 100%;
    margin: 16px;
    padding: 24px;
    background: var(--panel-bg);
    border: var(--panel-border);
    border-radius: 20px;
    box-shadow: 0 10px 40px rgba(0, 0, 0, 0.6), var(--border-glow);
    box-sizing: border-box;
    transition: background 0.4s ease, border-color 0.4s ease, box-shadow 0.4s ease;
}

header {
    text-align: center;
    margin-bottom: 24px;
    border-bottom: 2px dashed var(--border-color);
    padding-bottom: 16px;
}

.cyber-badge {
    background-color: rgba(69, 243, 255, 0.1);
    color: var(--neon-blue);
    border: 1px solid var(--neon-blue);
    padding: 4px 10px;
    font-size: 10px;
    font-weight: bold;
    border-radius: 4px;
    letter-spacing: 2px;
}

h1 {
    color: var(--neon-blue);
    font-size: 26px;
    margin: 12px 0 6px 0;
    letter-spacing: 1.5px;
    text-shadow: 0 0 10px rgba(69, 243, 255, 0.3);
}

.subtitle {
    font-size: 12px;
    color: var(--neon-teal);
    margin: 0;
    text-transform: uppercase;
    letter-spacing: 1px;
}

.main-layout {
    display: grid;
    grid-template-columns: 1.2fr 1.5fr;
    gap: 24px;
    margin-bottom: 24px;
}

@media (max-width: 768px) {
    .main-layout {
        grid-template-columns: 1fr;
    }
}

/* Theme Controller Styles */
.theme-selector-container {
    width: 100%;
    margin-bottom: 18px;
    display: flex;
    flex-direction: column;
    align-items: center;
    border-bottom: 1.5px dashed var(--border-color);
    padding-bottom: 14px;
}

.theme-options {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 6px;
    margin-top: 6px;
}

.theme-btn {
    background: rgba(255, 255, 255, 0.05);
    border: 1px solid rgba(255, 255, 255, 0.15);
    color: var(--text-muted);
    font-size: 9.5px;
    font-weight: bold;
    padding: 6px 12px;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
    letter-spacing: 0.5px;
}

.theme-btn:hover {
    color: var(--white);
    background: rgba(255, 255, 255, 0.12);
}

.theme-btn.active {
    background: var(--neon-blue);
    border-color: var(--neon-blue);
    color: #0c0f1b;
    box-shadow: 0 0 12px var(--neon-blue);
}

/* Wheel interactive section */
.wheel-panel {
    background: var(--wheel-bg);
    border-radius: 16px;
    padding: 20px;
    border: var(--panel-border);
    display: flex;
    flex-direction: column;
    align-items: center;
    position: relative;
    box-shadow: var(--border-glow);
    transition: background 0.4s ease, border-color 0.4s ease, box-shadow 0.4s ease;
}

.wheel-container {
    width: 280px;
    height: 280px;
    position: relative;
    touch-action: none;
    cursor: grab;
}

.wheel-container:active {
    cursor: grabbing;
}

/* SVG Rendering & Colors */
#decoder-svg {
    user-select: none;
    -webkit-user-select: none;
}

.grid-ring {
    fill: none;
    stroke-width: 1.5px;
}

.outer-glow-ring {
    stroke: var(--neon-blue);
    opacity: 0.1;
}

.border-ring {
    stroke: var(--neon-teal);
    opacity: 0.25;
}

.mid-ring {
    stroke: var(--neon-purple);
    stroke-dasharray: 4, 4;
}

.core-disk {
    fill: var(--wheel-core);
    transition: fill 0.4s ease;
}

.core-ring {
    fill: none;
    stroke: var(--neon-blue);
    stroke-width: 1.5px;
    opacity: 0.5;
}

.top-pointer-arrow {
    fill: var(--neon-blue);
}

.letters-group text {
    font-family: var(--font-family);
    font-weight: bold;
    user-select: none;
}

.outer-letters text {
    fill: var(--white);
    font-size: 13.5px;
    text-anchor: middle;
    cursor: pointer;
    transition: fill 0.2s;
}

.outer-letters text:hover {
    fill: var(--neon-blue);
}

.inner-letters text {
    fill: var(--neon-teal);
    font-size: 13px;
    text-anchor: middle;
    font-weight: bold;
}

.core-title-text {
    fill: var(--text-muted);
    font-size: 9px;
    text-anchor: middle;
    letter-spacing: 1px;
    font-weight: 500;
}

.core-value-text {
    fill: var(--neon-teal);
    font-size: 26px;
    transform-origin: 160px 172px;
    font-weight: 900;
    text-anchor: middle;
    transition: fill 0.2s ease, text-shadow 0.2s ease;
    text-shadow: 0 0 10px var(--neon-teal);
}

/* Wheel slider controls */
.wheel-controls {
    width: 100%;
    margin-top: 16px;
}

.control-label {
    font-size: 10px;
    color: var(--text-muted);
    letter-spacing: 1px;
    margin-bottom: 8px;
    text-align: center;
}

.slider-row {
    display: flex;
    align-items: center;
    gap: 12px;
}

.btn-step {
    background-color: rgba(255, 255, 255, 0.05);
    color: var(--neon-blue);
    border: 1px solid var(--border-color);
    border-radius: 8px;
    width: 36px;
    height: 36px;
    display: flex;
    justify-content: center;
    align-items: center;
    cursor: pointer;
    font-weight: bold;
    font-size: 16px;
    transition: all 0.2s ease;
}

.btn-step:hover {
    background-color: var(--neon-blue);
    color: #0c0f1b;
    box-shadow: 0 0 8px var(--neon-blue);
}

.neon-slider {
    flex: 1;
    -webkit-appearance: none;
    background: rgba(255, 255, 255, 0.05);
    height: 8px;
    border-radius: 4px;
    outline: none;
    border: 1px solid var(--border-color);
}

.neon-slider::-webkit-slider-thumb {
    -webkit-appearance: none;
    width: 18px;
    height: 18px;
    border-radius: 50%;
    background: var(--neon-teal);
    cursor: pointer;
    box-shadow: 0 0 10px var(--neon-teal);
    transition: transform 0.1s;
}

.neon-slider::-webkit-slider-thumb:hover {
    transform: scale(1.15);
}

/* Translator section style */
.translator-panel {
    background: var(--wheel-bg);
    border-radius: 16px;
    padding: 20px;
    border: var(--panel-border);
    box-shadow: var(--border-glow);
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    transition: background 0.4s ease, border-color 0.4s ease, box-shadow 0.4s ease;
}

.toggle-mode-container {
    display: flex;
    gap: 10px;
    margin-bottom: 20px;
}

.btn-mode {
    flex: 1;
    background-color: rgba(255, 255, 255, 0.05);
    border: 1px solid var(--border-color);
    color: var(--text-muted);
    padding: 10px 4px;
    border-radius: 10px;
    font-size: 10.5px;
    font-weight: bold;
    cursor: pointer;
    transition: all 0.2s ease;
}

.btn-mode.active {
    background-color: var(--neon-purple);
    border-color: var(--neon-purple);
    color: var(--white);
    box-shadow: 0 0 15px var(--neon-purple);
}

.text-boxes-container {
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.input-container label, .output-container label {
    display: block;
    font-size: 10px;
    color: var(--text-muted);
    margin-bottom: 6px;
    font-weight: bold;
    letter-spacing: 0.5px;
}

textarea {
    width: 100%;
    height: 80px;
    background-color: rgba(0, 0, 0, 0.25);
    border: 1px solid var(--border-color);
    border-radius: 10px;
    padding: 12px;
    color: var(--white);
    font-family: inherit;
    font-size: 14px;
    resize: none;
    box-sizing: border-box;
    outline: none;
    transition: border-color 0.2s, box-shadow 0.2s;
}

textarea:focus {
    border-color: var(--neon-blue);
    box-shadow: 0 0 8px var(--neon-blue);
}

.result-box-stream {
    background-color: rgba(0, 0, 0, 0.25);
    border: 1.5px solid var(--border-color);
    border-radius: 10px;
    padding: 12px;
    min-height: 50px;
    max-height: 80px;
    overflow-y: auto;
    font-family: "Courier New", Courier, monospace, sans-serif;
    color: var(--neon-teal);
    font-size: 14.5px;
    font-weight: bold;
    box-sizing: border-box;
    white-space: pre-wrap;
    word-break: break-all;
}

/* Character pathways mapping grid */
.visual-trail-track {
    margin-top: 18px;
    border-top: 1px dashed var(--border-color);
    padding-top: 14px;
}

.trail-heading {
    display: block;
    font-size: 9.5px;
    color: var(--neon-blue);
    font-weight: bold;
    margin-bottom: 8px;
    letter-spacing: 0.7px;
}

.trail-mapping-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 6px;
    max-height: 80px;
    overflow-y: auto;
    padding: 4px;
}

.empty-trail {
    font-size: 11px;
    color: var(--text-muted);
    font-style: italic;
}

.trail-node {
    background: rgba(255, 255, 255, 0.05);
    color: var(--white);
    border: 1px solid var(--border-color);
    border-radius: 4px;
    padding: 2px 6px;
    font-size: 11px;
    font-family: monospace;
    display: inline-flex;
    align-items: center;
    gap: 4px;
}

.trail-node .arrow {
    color: var(--neon-teal);
    font-size: 9px;
}

.trail-node .cipher {
    color: var(--neon-blue);
    font-weight: bold;
}

#inner-wheel {
    /* Magnificent snapping tactile ease out back curves */
    transition: transform 0.35s cubic-bezier(0.175, 0.885, 0.32, 1.25);
    transform-origin: 160px 160px;
}

#inner-wheel.dragging {
    transition: none !important;
}

footer {
    text-align: center;
    margin-top: 24px;
    border-top: 1px dashed var(--border-color);
    padding-top: 12px;
}

footer p {
    font-size: 10px;
    color: var(--text-muted);
    margin: 0;
}

/* ================= THEME DEFINITIONS ================= */

/* UBUNTU THEME */
.theme-ubuntu {
    --bg-dark: #2c001e;
    --panel-bg: #300a24;
    --neon-blue: #df4814;
    --neon-teal: #f99157;
    --neon-purple: #ae3f7b;
    --white: #ffffff;
    --text-muted: #aea79f;
    --border-color: rgba(223, 72, 20, 0.35);
    --border-glow: 0 0 12px rgba(223, 72, 20, 0.25);
    --panel-border: 1.5px solid #df4814;
    --font-family: "Ubuntu", Consolas, Monaco, monospace;
    --wheel-bg: #1c0012;
    --wheel-core: #13000a;
}
.theme-ubuntu .theme-btn.active {
    background: var(--neon-blue);
    border-color: var(--neon-blue);
    color: #ffffff;
    box-shadow: 0 0 10px rgba(223, 72, 20, 0.5);
}

/* VS CODE THEME */
.theme-vscode {
    --bg-dark: #1e1e1e;
    --panel-bg: #252526;
    --neon-blue: #007acc;
    --neon-teal: #4ec9b0;
    --neon-purple: #c586c0;
    --white: #d4d4d4;
    --text-muted: #858585;
    --border-color: rgba(0, 122, 204, 0.35);
    --border-glow: 0 0 8px rgba(0, 122, 204, 0.15);
    --panel-border: 1px solid #3c3c3c;
    --font-family: Consolas, "Courier New", monospace;
    --wheel-bg: #1e1e1e;
    --wheel-core: #090909;
}
.theme-vscode .theme-btn.active {
    background: var(--neon-blue);
    border-color: var(--neon-blue);
    color: #ffffff;
    box-shadow: 0 0 10px rgba(0, 122, 204, 0.5);
}

/* GITHUB THEME (Light Developer Style) */
.theme-github {
    --bg-dark: #f6f8fa;
    --panel-bg: #ffffff;
    --neon-blue: #0969da;
    --neon-teal: #1a7f37;
    --neon-purple: #8250df;
    --white: #24292f;
    --text-muted: #57606a;
    --border-color: #d0d7de;
    --border-glow: none;
    --panel-border: 1px solid #d0d7de;
    --font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Helvetica, Arial, sans-serif;
    --wheel-bg: #f6f8fa;
    --wheel-core: #eaeef2;
}
.theme-github .theme-btn {
    background: #f3f4f6;
    border: 1px solid #d1d5db;
    color: #374151;
}
.theme-github .theme-btn:hover {
    background: #e5e7eb;
}
.theme-github .theme-btn.active {
    background: var(--neon-blue);
    border-color: var(--neon-blue);
    color: #ffffff;
    box-shadow: 0 0 8px rgba(9, 105, 218, 0.3);
}

/* MINIMALIST THEME (High Contrast B&W Modernism) */
.theme-minimalist {
    --bg-dark: #ffffff;
    --panel-bg: #ffffff;
    --neon-blue: #000000;
    --neon-teal: #000000;
    --neon-purple: #555555;
    --white: #000000;
    --text-muted: #777777;
    --border-color: #000000;
    --border-glow: none;
    --panel-border: 2px solid #000000;
    --font-family: "Helvetica Neue", Helvetica, Arial, sans-serif;
    --wheel-bg: #ffffff;
    --wheel-core: #f5f5f5;
}
.theme-minimalist .theme-btn {
    background: #ffffff;
    border: 1px solid #000000;
    color: #000000;
    border-radius: 0;
}
.theme-minimalist .theme-btn.active {
    background: #000000;
    color: #ffffff;
    box-shadow: none;
}

/* ANTIQUE THEME (Parchment & Warm Brass Codebreaker) */
.theme-antique {
    --bg-dark: #120e0a;
    --panel-bg: #221a12;
    --neon-blue: #cba374;
    --neon-teal: #eecba5;
    --neon-purple: #82684b;
    --white: #eae0c8;
    --text-muted: #9f8d75;
    --border-color: #82684b;
    --border-glow: 0 0 15px rgba(130, 104, 75, 0.2);
    --panel-border: 2px solid #82684b;
    --font-family: Georgia, Garamond, serif;
    --wheel-bg: #2d2319;
    --wheel-core: #1b150f;
}
.theme-antique .theme-btn {
    background: rgba(130, 104, 75, 0.1);
    border: 1px solid #82684b;
    color: #9f8d75;
    font-family: inherit;
}
.theme-antique .theme-btn.active {
    background: var(--neon-blue);
    color: #120e0a;
    border-color: #cba374;
}

/* Snapping pulse animations */
@keyframes core-pulse {
    0% { transform: scale(1); }
    50% { transform: scale(1.15); }
    100% { transform: scale(1); }
}

.core-pulse-active {
    animation: core-pulse 0.2s cubic-bezier(0.175, 0.885, 0.32, 1.275);
}"""
    }

    private fun getUpgradedJs(): String {
        return """const outerWheelLetters = document.getElementById('outer-wheel-letters');
const innerWheelLetters = document.getElementById('inner-wheel-letters');
const alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
const R_outer = 124;
const R_inner = 84;

// Generate A-Z letters around circles
for (let i = 0; i < 26; i++) {
    const char = alphabet[i];
    const angleDeg = i * (360 / 26);
    const angleRad = (angleDeg - 90) * Math.PI / 180;

    // Outer ring Plaintext
    const xO = 160 + R_outer * Math.cos(angleRad);
    const yO = 160 + R_outer * Math.sin(angleRad);
    
    const outerText = document.createElementNS("http://www.w3.org/2000/svg", "text");
    outerText.setAttribute("x", xO);
    outerText.setAttribute("y", yO + 4.5);
    outerText.setAttribute("transform", "rotate(" + angleDeg + ", " + xO + ", " + yO + ")");
    outerText.textContent = char;
    outerText.dataset.index = i;
    outerWheelLetters.appendChild(outerText);

    // Inner ring Cipher
    const xI = 160 + R_inner * Math.cos(angleRad);
    const yI = 160 + R_inner * Math.sin(angleRad);
    
    const innerText = document.createElementNS("http://www.w3.org/2000/svg", "text");
    innerText.setAttribute("x", xI);
    innerText.setAttribute("y", yI + 4.5);
    innerText.setAttribute("transform", "rotate(" + angleDeg + ", " + xI + ", " + yI + ")");
    innerText.textContent = char;
    innerText.dataset.index = i;
    innerWheelLetters.appendChild(innerText);
}

// Controller elements
const innerWheel = document.getElementById('inner-wheel');
const shiftSlider = document.getElementById('shift-slider');
const coreShiftVal = document.getElementById('core-shift-val');
const btnShiftDown = document.getElementById('btn-shift-down');
const btnShiftUp = document.getElementById('btn-shift-up');
const textInput = document.getElementById('text-input');
const textOutput = document.getElementById('text-output');
const trailMapping = document.getElementById('trail-mapping');
const modeEncrypt = document.getElementById('mode-encrypt');
const modeDecrypt = document.getElementById('mode-decrypt');

let currentShift = 0;
let isEncryptMode = true;

// Drag rotation metrics
const decoderSvg = document.getElementById('decoder-svg');
let isDragging = false;
let startAngle = 0;
let baseAngle = 0;

function getMouseAngle(e) {
    const rect = decoderSvg.getBoundingClientRect();
    const centerX = rect.left + rect.width / 2;
    const centerY = rect.top + rect.height / 2;
    
    let clientX = e.clientX;
    let clientY = e.clientY;
    
    if (e.touches && e.touches.length > 0) {
        clientX = e.touches[0].clientX;
        clientY = e.touches[0].clientY;
    }
    
    const dx = clientX - centerX;
    const dy = clientY - centerY;
    return Math.atan2(dy, dx) * 180 / Math.PI;
}

decoderSvg.addEventListener('pointerdown', (e) => {
    isDragging = true;
    innerWheel.classList.add('dragging');
    const angle = getMouseAngle(e);
    startAngle = angle;
    baseAngle = -currentShift * (360 / 26);
    decoderSvg.setPointerCapture(e.pointerId);
    console.log("Interactive Rotor Drag Started");
});

decoderSvg.addEventListener('pointermove', (e) => {
    if (!isDragging) return;
    const angle = getMouseAngle(e);
    const deltaAngle = angle - startAngle;
    const targetRot = baseAngle + deltaAngle;
    
    // Rotate wheel instantly (without transitions) while dragging
    innerWheel.style.transform = "rotate(" + targetRot + "deg)";
    
    // Convert current targetRot to positive integer shift [0 - 25]
    const anglePerLetter = 360 / 26;
    let computedShift = Math.round(-targetRot / anglePerLetter) % 26;
    if (computedShift < 0) computedShift += 26;
    
    if (computedShift !== currentShift) {
        currentShift = computedShift;
        shiftSlider.value = currentShift;
        coreShiftVal.textContent = currentShift;
        triggerCorePulse();
        translateText();
    }
});

decoderSvg.addEventListener('pointerup', (e) => {
    if (!isDragging) return;
    isDragging = false;
    innerWheel.classList.remove('dragging');
    decoderSvg.releasePointerCapture(e.pointerId);
    
    // Snap rot to perfect alignment with easing
    const targetRot = -currentShift * (360 / 26);
    innerWheel.style.transform = "rotate(" + targetRot + "deg)";
    triggerCorePulse();
    console.log("Rotor alignment snapped to key: " + currentShift);
});

// Slider & Steps triggers
shiftSlider.addEventListener('input', (e) => {
    updateShift(parseInt(e.target.value));
});

btnShiftDown.addEventListener('click', () => {
    updateShift((currentShift - 1 + 26) % 26);
});

btnShiftUp.addEventListener('click', () => {
    updateShift((currentShift + 1) % 26);
});

function updateShift(val) {
    if (val === currentShift) return;
    currentShift = val;
    shiftSlider.value = currentShift;
    coreShiftVal.textContent = currentShift;
    
    // Rotate wheel with spring easing
    innerWheel.classList.remove('dragging');
    const targetRot = -currentShift * (360 / 26);
    innerWheel.style.transform = "rotate(" + targetRot + "deg)";
    
    triggerCorePulse();
    translateText();
}

function triggerCorePulse() {
    coreShiftVal.classList.remove('core-pulse-active');
    void coreShiftVal.offsetWidth; // Force layout recalculation
    coreShiftVal.classList.add('core-pulse-active');
}

// Mode Selection Toggle
modeEncrypt.addEventListener('click', () => {
    isEncryptMode = true;
    modeEncrypt.classList.add('active');
    modeDecrypt.classList.remove('active');
    translateText();
    console.log("Toggled mode: ENCRYPT");
});

modeDecrypt.addEventListener('click', () => {
    isEncryptMode = false;
    modeDecrypt.classList.add('active');
    modeEncrypt.classList.remove('active');
    translateText();
    console.log("Toggled mode: DECRYPT");
});

// Theme Selector Support
const themeButtons = document.querySelectorAll('.theme-btn');
const appContainer = document.querySelector('.app-container');

themeButtons.forEach(btn => {
    btn.addEventListener('click', () => {
        themeButtons.forEach(b => b.classList.remove('active'));
        btn.classList.add('active');

        // Reset and apply theme class
        appContainer.className = 'app-container';
        const selectedTheme = btn.dataset.theme;
        if (selectedTheme !== 'default') {
            appContainer.classList.add('theme-' + selectedTheme);
        }
        console.log("Aesthetic theme loaded: " + selectedTheme);
    });
});

// Input tracker and Shift translation
textInput.addEventListener('input', () => {
    translateText();
});

function translateText() {
    const text = textInput.value;
    if (!text) {
        textOutput.textContent = "Waiting for transmission...";
        textOutput.style.color = "rgba(102, 252, 241, 0.4)";
        trailMapping.innerHTML = '<div class="empty-trail">Introduce input text to trace shift paths</div>';
        return;
    }
    
    textOutput.style.color = "var(--neon-teal)";
    
    const shiftVal = isEncryptMode ? currentShift : (26 - currentShift);
    let output = "";
    let nodesHtml = "";
    const maxTrails = 18;
    let trailCount = 0;
    
    for (let i = 0; i < text.length; i++) {
        const char = text[i];
        const upper = char.toUpperCase();
        const plainIndex = alphabet.indexOf(upper);
        
        if (plainIndex !== -1) {
            const cipherIndex = (plainIndex + shiftVal) % 26;
            const cipherChar = alphabet[cipherIndex];
            const resultChar = (char === upper) ? cipherChar : cipherChar.toLowerCase();
            output += resultChar;
            
            // Build visual trail mapping flow nodes
            if (trailCount < maxTrails) {
                nodesHtml += '<div class="trail-node">' +
                    '<span>' + upper + '</span>' +
                    '<span class="arrow">&gt;</span>' +
                    '<span class="cipher">' + cipherChar + '</span>' +
                    '</div>';
                trailCount++;
            }
        } else {
            output += char;
        }
    }
    
    textOutput.textContent = output;
    
    if (nodesHtml) {
        trailMapping.innerHTML = nodesHtml;
    } else {
        trailMapping.innerHTML = '<div class="empty-trail">Symbols unchanged</div>';
    }
}

// Initialize
updateShift(0);
console.log("🚀 Custom Caesar Decoder Wheel Console active and live!");
"""
    }

    // --- CEEZIX SAMPLE TEMPLATE MAP ROUTERS ---

    private fun getCaesarWheelTemplateMap(): Map<String, String> {
        return mapOf(
            "README.md" to """# 🌌 Interactive Caesar Concentric Decoder Wheel

A fully interactive circular CSS-based decoder wheel prototype. This digital sandbox serves as a playground for Caesar cipher encoding and decoding.

### ✨ Highlights:
- 🎡 **Concentric Rotors**: Swipe / drag or slider-controlled circular alphabet shifting.
- 🔗 **Real-time Log traces**: Traces character shifts step-by-step so you can inspect how alphabetical mapping functions.
- 🎨 **Theme Options**: Dynamic aesthetic schemes including Cyber Neon and Vintage Parchment.
""",
            "index.html" to getUpgradedHtml(),
            "style.css" to getUpgradedCss(),
            "script.js" to getUpgradedJs(),
            "data.json" to """{
  "template": "Caesar Wheel Simulator",
  "difficulty": "Beginner",
  "category": "Substitution Ciphers",
  "version": "1.8.0"
}"""
        )
    }

    private fun getMatrixRainTemplateMap(): Map<String, String> {
        return mapOf(
            "README.md" to """# ⚡ Cyber Matrix Digital Rain Cascade

An interactive code canvas displaying the classic green rain matrix codestream but upgraded with hidden cipher text injections!

### ✨ Features:
- 🛸 **High-performance Canvas Rendering**: Optimized for fluid rendering.
- 🎨 **Interactive HUD overlay**: Real-time control panels for adjustings Speed FPS, Theme hue colors, and density.
- 🕵️‍♂️ **Cipher Injections**: Type secret keys below in the controller to inject custom purple cryptographic highlight characters into the rain lines!
""",
            "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cyber Matrix Digital Rain</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="hud-overlay">
        <header>
            <span class="hud-badge">MATRIX CORE ACTIVE</span>
            <h1>⚡ MATRIX DIGITAL RAIN</h1>
            <p>Interactive Binary & Cipher Stream Sandbox</p>
        </header>

        <section class="controls">
            <div class="control-row">
                <label>Stream Aesthetic</label>
                <select id="colorSelect">
                    <option value="matrix" selected>Matrix Green</option>
                    <option value="neon-blue">Nebula Blue</option>
                    <option value="amber">Retro Amber</option>
                    <option value="orchid">Orchid Pink</option>
                </select>
            </div>
            
            <div class="control-row">
                <label>Cascade Speed</label>
                <input type="range" id="speedSlider" min="10" max="100" value="33">
            </div>

            <div class="control-row">
                <label>Cipher Injector</label>
                <input type="text" id="cipherInject" placeholder="Type secret text...">
            </div>
        </section>
        
        <div class="log-panel">
            <span class="log-title">CONSOLE WORKSPACE</span>
            <div id="outputLog">Matrix active and socket running...</div>
        </div>
    </div>
    <canvas id="matrixCanvas"></canvas>
    <script src="script.js"></script>
</body>
</html>""",
            "style.css" to """:root {
    --matrix-green: #00FF00;
    --accent-glow: rgba(0, 255, 0, 0.45);
    --bg-dark: #000000;
}

body {
    margin: 0;
    padding: 0;
    width: 100vw;
    height: 100vh;
    overflow: hidden;
    background-color: var(--bg-dark);
    font-family: 'Courier New', Courier, monospace;
    color: #ffffff;
}

#matrixCanvas {
    position: absolute;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    z-index: 1;
}

.hud-overlay {
    position: absolute;
    top: 20px;
    left: 20px;
    z-index: 10;
    pointer-events: auto;
    background: rgba(0, 0, 0, 0.85);
    border: 1px solid var(--matrix-green);
    box-shadow: 0 0 20px var(--accent-glow);
    border-radius: 12px;
    padding: 20px;
    max-width: 320px;
    display: flex;
    flex-direction: column;
    gap: 15px;
}

h1 {
    font-size: 16px;
    margin: 8px 0 2px 0;
    color: var(--matrix-green);
    text-shadow: 0 0 10px var(--matrix-green);
}

p {
    font-size: 11px;
    margin: 0;
    color: #888;
}

.hud-badge {
    background: #003300;
    color: var(--matrix-green);
    font-size: 8px;
    padding: 2px 6px;
    border-radius: 4px;
    align-self: flex-start;
    border: 1px solid var(--matrix-green);
    font-weight: bold;
}

.controls {
    display: flex;
    flex-direction: column;
    gap: 10px;
}

.control-row {
    display: flex;
    flex-direction: column;
    gap: 4px;
    font-size: 11px;
}

.control-row label {
    color: var(--matrix-green);
    font-size: 10px;
    text-transform: uppercase;
    letter-spacing: 1px;
}

select, input[type="text"], input[type="range"] {
    background: #111;
    border: 1px solid var(--matrix-green);
    color: #fff;
    padding: 6px;
    border-radius: 4px;
    font-size: 11px;
    font-family: inherit;
    outline: none;
}

select:focus, input[type="text"]:focus {
    box-shadow: 0 0 8px var(--matrix-green);
}

.log-panel {
    border-top: 1px solid rgba(0, 255, 0, 0.2);
    padding-top: 10px;
}

.log-title {
    font-size: 9px;
    color: #555;
    display: block;
    margin-bottom: 4px;
}

#outputLog {
    font-size: 10px;
    color: var(--matrix-green);
    height: 36px;
    overflow-y: auto;
}""",
            "script.js" to """const canvas = document.getElementById("matrixCanvas");
const ctx = canvas.getContext("2d");

const resizeCanvas = () => {
    canvas.width = window.innerWidth;
    canvas.height = window.innerHeight;
};
resizeCanvas();
window.addEventListener("resize", resizeCanvas);

const alphabet = "0101XYZΩΨΦ01ABCDEF0123456789";
const fontSize = 16;
let columns = Math.floor(canvas.width / fontSize);

let rainDrops = Array(columns).fill(1);

const speedSlider = document.getElementById("speedSlider");
const colorSelect = document.getElementById("colorSelect");
const cipherInject = document.getElementById("cipherInject");
const outputLog = document.getElementById("outputLog");

let colorsList = {
    "matrix": { main: "#0F0", head: "#FFF", bg: "rgba(0, 0, 0, 0.05)" },
    "neon-blue": { main: "#00E5FF", head: "#FFF", bg: "rgba(0, 0, 0, 0.07)" },
    "amber": { main: "#FFB266", head: "#FFE5CC", bg: "rgba(0, 0, 0, 0.06)" },
    "orchid": { main: "#FF79C6", head: "#FFF", bg: "rgba(0, 0, 0, 0.05)" }
};

let activeColor = "matrix";
colorSelect.addEventListener("change", (e) => {
    activeColor = e.target.value;
    const style = document.documentElement.style;
    const hex = colorsList[activeColor].main;
    style.setProperty('--matrix-green', hex);
    style.setProperty('--accent-glow', hex + "77");
    outputLog.textContent = `Vibe set to: ` + activeColor.toUpperCase();
});

let injectedList = [];
cipherInject.addEventListener("change", (e) => {
    if (e.target.value.trim()) {
        injectedList = e.target.value.toUpperCase().split("");
        outputLog.textContent = "Injected: '" + e.target.value + "' into cascade!";
    } else {
        injectedList = [];
    }
});

const draw = () => {
    const config = colorsList[activeColor];
    ctx.fillStyle = config.bg;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.font = fontSize + "px monospace";

    for (let i = 0; i < rainDrops.length; i++) {
        let char = alphabet[Math.floor(Math.random() * alphabet.length)];
        
        if (injectedList.length > 0 && Math.random() < 0.1) {
            char = injectedList[Math.floor(Math.random() * injectedList.length)];
            ctx.fillStyle = "#BD5EFF";
        } else {
            ctx.fillStyle = config.main;
        }

        const y = rainDrops[i] * fontSize;
        const x = i * fontSize;

        ctx.fillText(char, x, y);

        if (y > canvas.height && Math.random() > 0.975) {
            rainDrops[i] = 0;
        }
        rainDrops[i]++;
    }
};

let intervalId = setInterval(draw, 1000 / 33);
speedSlider.addEventListener("input", (e) => {
    clearInterval(intervalId);
    const speed = e.target.value;
    intervalId = setInterval(draw, 1000 / speed);
    outputLog.textContent = `Refresh rate optimized: ` + speed + ` FPS`;
});

console.log("Matrix codestream active!");""",
            "data.json" to """{
  "template": "Binary rain",
  "difficulty": "Medium",
  "category": "Canvas Graphics"
}"""
        )
    }

    private fun getVigenereTemplateMap(): Map<String, String> {
        return mapOf(
            "README.md" to """# 🗝️ Vigenère Shifting Slate Grid

The classical polyalphabetic code device with tabular calculations.

### ✨ Highlights:
- 📊 **Dynamic Tabular Visualizer**: Shows the complete alphabet grid.
- 🛡️ **Secret Key Shifting**: Uses dynamic custom length passkeys.
- 🚀 **Step-by-step Decoder list**: Displays trace patterns showing intersection indexes matching each cipher token.
""",
            "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Vigenere cipher sandbox</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="vigenere-container">
        <header>
            <span class="badge">POLYALPHABETIC ENGINE</span>
            <h1>🗝️ VIGENÈRE CIPHER GRID</h1>
            <p>Calculate shifting intersection codes in real-time</p>
        </header>

        <section class="workspace">
            <div class="panel text-entry">
                <div class="control-box">
                    <label>Key Phrase (Repeat cycle)</label>
                    <input type="text" id="vigenereKey" value="SOLAR" placeholder="Type key...">
                </div>

                <div class="control-box">
                    <label>Plaintext payload</label>
                    <textarea id="plainText" placeholder="Enter plaintext message...">HELLO WORLD FROM THE CEZIX SANDBOX</textarea>
                </div>

                <div class="control-box">
                    <label>Encrypted Code</label>
                    <textarea id="cipherText" readonly></textarea>
                </div>
            </div>

            <div class="panel grid-panel">
                <h3>VIGENERE COORDINATES MATRIX</h3>
                <div id="matrixTable" class="table-scroll"></div>
            </div>
        </section>
        
        <section class="trace-logs">
            <h3>🔏 CHARACTER MATRIX CALCULATOR TRACES</h3>
            <div id="calcTraces" class="trace-box"></div>
        </section>
    </div>
    <script src="script.js"></script>
</body>
</html>""",
            "style.css" to """:root {
    --vp-color: #BD5EFF;
    --cyan: #45F3FF;
    --navy: #0F121C;
    --border-lines: rgba(69, 243, 255, 0.15);
}

body {
    margin: 0;
    padding: 16px;
    background: var(--navy);
    color: #E2E8F0;
    font-family: system-ui, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
    line-height: 1.5;
}

.vigenere-container {
    max-width: 900px;
    margin: 0 auto;
    background: rgba(27, 32, 48, 0.8);
    background-image: radial-gradient(circle at top right, rgba(189, 94, 255, 0.08) 0%, transparent 400px);
    border: 1px solid var(--border-lines);
    box-shadow: 0 10px 30px rgba(0,0,0,0.5);
    border-radius: 16px;
    padding: 24px;
}

header {
    border-bottom: 1px solid var(--border-lines);
    padding-bottom: 20px;
    margin-bottom: 24px;
}

h1 {
    font-size: 22px;
    color: var(--cyan);
    margin: 8px 0;
}

p {
    margin: 0;
    font-size: 13px;
    color: #94A3B8;
}

.badge {
    background: rgba(189, 94, 255, 0.15);
    color: #C084FC;
    font-size: 9px;
    padding: 3px 8px;
    border-radius: 6px;
    border: 1px solid rgba(189, 94, 255, 0.3);
    font-weight: bold;
}

.workspace {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20px;
}

@media(max-width: 768px) {
    .workspace {
        grid-template-columns: 1fr;
    }
}

.panel {
    background: rgba(12, 15, 27, 0.5);
    border: 1px solid var(--border-lines);
    border-radius: 12px;
    padding: 16px;
}

.control-box {
    margin-bottom: 16px;
    display: flex;
    flex-direction: column;
}

.control-box label {
    font-size: 11px;
    color: var(--cyan);
    text-transform: uppercase;
    font-weight: bold;
    margin-bottom: 6px;
}

input[type="text"], textarea {
    background: rgba(15, 18, 28, 0.8);
    border: 1px solid var(--border-lines);
    border-radius: 8px;
    padding: 10px;
    color: #fff;
    font-family: monospace;
    font-size: 13px;
    outline: none;
    resize: none;
}

input[type="text"]:focus, textarea:focus {
    border-color: var(--cyan);
    box-shadow: 0 0 10px rgba(69, 243, 255, 0.2);
}

textarea {
    height: 80px;
}

.table-scroll {
    overflow-x: auto;
    max-height: 280px;
}

table {
    border-collapse: collapse;
    width: 100%;
    font-size: 10px;
    font-family: monospace;
}

td, th {
    border: 1px solid rgba(255,255,255,0.05);
    width: 20px;
    height: 20px;
    text-align: center;
}

th {
    background: rgba(69, 243, 255, 0.1);
    color: var(--cyan);
}

.highlight-cell {
    background: var(--vp-color);
    color: #fff;
    font-weight: bold;
}

.trace-logs {
    margin-top: 24px;
    border-top: 1px solid var(--border-lines);
    padding-top: 20px;
}

.trace-logs h3 {
    margin: 0 0 12px 0;
    font-size: 12px;
    color: var(--cyan);
}

.trace-box {
    background: rgba(15, 18, 28, 0.8);
    border: 1px solid var(--border-lines);
    border-radius: 8px;
    font-family: monospace;
    padding: 12px;
    height: 120px;
    overflow-y: auto;
    font-size: 12px;
}

.trace-line {
    border-bottom: 1px solid rgba(255,255,255,0.02);
    padding: 3px 0;
}

.trace-line span {
    color: var(--cyan);
}""",
            "script.js" to """const plainText = document.getElementById("plainText");
const vigenereKey = document.getElementById("vigenereKey");
const cipherText = document.getElementById("cipherText");
const matrixTable = document.getElementById("matrixTable");
const calcTraces = document.getElementById("calcTraces");

const buildMatrix = () => {
    let html = "<table><thead><tr><th></th>";
    for (let c = 0; c < 26; c++) {
        html += "<th>" + String.fromCharCode(65 + c) + "</th>";
    }
    html += "</tr></thead><tbody>";

    for (let r = 0; r < 26; r++) {
        html += "<tr><th>" + String.fromCharCode(65 + r) + "</th>";
        for (let c = 0; c < 26; c++) {
            const index = (r + c) % 26;
            html += "<td id='cell-" + r + "-" + c + "'>" + String.fromCharCode(65 + index) + "</td>";
        }
        html += "</tr>";
    }
    html += "</tbody></table>";
    matrixTable.innerHTML = html;
};

const vigenereEncode = () => {
    const text = plainText.value.toUpperCase();
    const key = vigenereKey.value.toUpperCase().replace(/[^A-Z]/g, "");
    
    if (!key) {
        cipherText.value = text;
        return;
    }

    let result = "";
    let keyIdx = 0;
    let tracesHtml = "";
    
    document.querySelectorAll(".highlight-cell").forEach(td => td.classList.remove("highlight-cell"));

    for (let i = 0; i < text.length; i++) {
        const char = text[i];
        if (char >= 'A' && char <= 'Z') {
            const textCode = char.charCodeAt(0) - 65;
            const currentKeyChar = key[keyIdx % key.length];
            const keyCode = currentKeyChar.charCodeAt(0) - 65;
            const cipherVal = (textCode + keyCode) % 26;
            const finalChar = String.fromCharCode(65 + cipherVal);

            result += finalChar;

            const targetTd = document.getElementById("cell-" + keyCode + "-" + textCode);
            if (targetTd) targetTd.classList.add("highlight-cell");

            tracesHtml += "<div class='trace-line'>Char: <span>" + char + "</span> + Key: <span>" + currentKeyChar + "</span> (shift +" + keyCode + ") => <span>" + finalChar + "</span></div>";

            keyIdx++;
        } else {
            result += char;
        }
    }
    
    cipherText.value = result;
    calcTraces.innerHTML = tracesHtml;
};

plainText.addEventListener("input", vigenereEncode);
vigenereKey.addEventListener("input", vigenereEncode);

buildMatrix();
vigenereEncode();
console.log("Vigenere calculator grid ready!");""",
            "data.json" to """{
  "template": "Vigenere Polyalphabetic",
  "difficulty": "Hard",
  "category": "Classic Cryptography"
}"""
        )
    }

    private fun getBinaryConsoleTemplateMap(): Map<String, String> {
        return mapOf(
            "README.md" to """# 💻 Hex & Binary Crypto Console

An immersive interactive command-line style decoder dashboard.

### ✨ Highlights:
- 👾 **Cyberpunk CLI Skin theme**: Clean retro green dashboard style with cursor anims.
- ⚙️ **Conversions matrix**: Formats plain input blocks instantaneously to Binary, Decimal arrays, Hex, Base64 values, and Rot13 strings.
- 📂 **JSON output logs**: Compiles converted payload outputs automatically in formatted nested payloads.
""",
            "index.html" to """<!DOCTYPE html>
<html lang="en">
<head>
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Cryptographic Terminal Console</title>
    <link rel="stylesheet" href="style.css">
</head>
<body>
    <div class="terminal-shell">
        <header class="terminal-bar">
            <div class="dots"><span class="red"></span><span class="yellow"></span><span class="green"></span></div>
            <span class="title">aistudio@ceezix-crypto-node: ~</span>
        </header>

        <section class="terminal-body">
            <div class="welcome-banner">
                <p>=== CEEZIX CODESPACE INTERACTIVE CODING TERMINAL ===</p>
                <p>Status: ONLINE | Target Sandbox: WEB RAW STREAM CODES</p>
                <p>Type payload below to see instantaneous binary compilation.</p>
            </div>

            <div class="input-line">
                <span class="prompt">guest@ceezix:~$</span>
                <input type="text" id="terminalInput" value="CEEZIX MASTER DECRYPTON" autofocus>
            </div>
            
            <div class="terminal-outputs">
                <div class="output-block">
                    <span class="label">[RAW STRING]:</span>
                    <span id="outRaw" class="val"></span>
                </div>
                <div class="output-block">
                    <span class="label">[ASCII BINARY]:</span>
                    <span id="outBin" class="val val-green"></span>
                </div>
                <div class="output-block">
                    <span class="label">[HEXADECIMAL]:</span>
                    <span id="outHex" class="val val-blue"></span>
                </div>
                <div class="output-block">
                    <span class="label">[BASE64 ENCODED]:</span>
                    <span id="outB64" class="val val-yellow"></span>
                </div>
                <div class="output-block">
                    <span class="label">[ROT13 SHIFT]:</span>
                    <span id="outRot" class="val val-purple"></span>
                </div>
            </div>
        </section>
    </div>
    <script src="script.js"></script>
</body>
</html>""",
            "style.css" to """body {
    margin: 0;
    padding: 16px;
    background: #05070a;
    color: #e2e8f0;
    font-family: 'Consolas', 'Courier New', monospace;
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 90vh;
}

.terminal-shell {
    width: 100%;
    max-width: 750px;
    background: #0b0f14;
    border: 1px solid #1f2937;
    border-radius: 8px;
    box-shadow: 0 10px 40px rgba(0,0,0,0.6);
    overflow: hidden;
}

.terminal-bar {
    background: #121820;
    padding: 10px 14px;
    display: flex;
    align-items: center;
    border-bottom: 1px solid #1f2937;
    position: relative;
}

.dots {
    display: flex;
    gap: 6px;
}

.dots span {
    width: 10px;
    height: 10px;
    border-radius: 50%;
    display: inline-block;
}

.red { background: #ff5f56; }
.yellow { background: #ffbd2e; }
.green { background: #27c93f; }

.title {
    font-size: 11px;
    color: #94a3b8;
    position: absolute;
    left: 50%;
    transform: translateX(-50%);
}

.terminal-body {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.welcome-banner {
    font-size: 11px;
    color: #64748b;
    line-height: 1.6;
}

.input-line {
    display: flex;
    align-items: center;
    gap: 10px;
}

.prompt {
    color: #27c93f;
    font-weight: bold;
    font-size: 13px;
    white-space: nowrap;
}

#terminalInput {
    background: transparent;
    border: none;
    outline: none;
    color: #fff;
    font-family: inherit;
    font-size: 13px;
    flex: 1;
}

.terminal-outputs {
    display: flex;
    flex-direction: column;
    gap: 12px;
    border-top: 1px solid #1f2937;
    padding-top: 16px;
}

.output-block {
    display: flex;
    flex-direction: column;
    gap: 4px;
}

.label {
    font-size: 11px;
    color: #64748b;
    text-transform: uppercase;
}

.val {
    font-size: 12px;
    word-break: break-all;
    background: rgba(255,255,255,0.02);
    padding: 8px;
    border-radius: 4px;
    border: 1px solid rgba(255,255,255,0.04);
}

.val-green { color: #34d399; border-color: rgba(52, 211, 153, 0.1); }
.val-blue { color: #60a5fa; border-color: rgba(96, 165, 250, 0.1); }
.val-yellow { color: #fbbf24; border-color: rgba(251, 191, 36, 0.1); }
.val-purple { color: #c084fc; border-color: rgba(192, 132, 252, 0.1); }""",
            "script.js" to """const terminalInput = document.getElementById("terminalInput");
const outRaw = document.getElementById("outRaw");
const outBin = document.getElementById("outBin");
const outHex = document.getElementById("outHex");
const outB64 = document.getElementById("outB64");
const outRot = document.getElementById("outRot");

const convertAll = () => {
    const val = terminalInput.value;
    
    outRaw.textContent = val || "(empty)";

    let binary = "";
    for (let i = 0; i < val.length; i++) {
        const bin = val.charCodeAt(i).toString(2);
        binary += "0".repeat(8 - bin.length) + bin + " ";
    }
    outBin.textContent = binary.trim() || "00000000";

    let hex = "";
    for (let i = 0; i < val.length; i++) {
        hex += val.charCodeAt(i).toString(16).toUpperCase() + " ";
    }
    outHex.textContent = hex.trim() || "00";

    try {
        outB64.textContent = btoa(val) || "";
    } catch(e) {
        outB64.textContent = "(non-valid ascii characters)";
    }

    let rot = "";
    for (let i = 0; i < val.length; i++) {
        const code = val.charCodeAt(i);
        if (code >= 65 && code <= 90) {
            rot += String.fromCharCode(((code - 65 + 13) % 26) + 65);
        } else if (code >= 97 && code <= 122) {
            rot += String.fromCharCode(((code - 97 + 13) % 26) + 97);
        } else {
            rot += val[i];
        }
    }
    outRot.textContent = rot || "";
};

terminalInput.addEventListener("input", convertAll);
convertAll();
console.log("Terminal interface ready!");""",
            "data.json" to """{
  "template": "CLI Binary Converter",
  "difficulty": "Easy",
  "category": "Utility tool"
}"""
        )
    }

    // ==========================================
    // GIT VERSION CONTROL ACTIONS
    // ==========================================
    fun gitInit() {
        viewModelScope.launch {
            dataStoreManager.setGitInitialized(true)
            appendConsoleLog("info", "Initialized empty Git repository in virtual workspace.")
            
            // Auto stage all existing workspace files
            val paths = workspaceFiles.value.map { it.path }.toSet()
            dataStoreManager.setGitStagedPaths(paths)
        }
    }

    fun gitToggleStageFile(path: String) {
        viewModelScope.launch {
            val current = _gitStagedPaths.value.toMutableSet()
            if (current.contains(path)) {
                current.remove(path)
                appendConsoleLog("info", "Unstaged file: $path")
            } else {
                current.add(path)
                appendConsoleLog("info", "Staged file: $path")
            }
            dataStoreManager.setGitStagedPaths(current)
        }
    }

    fun gitStageAllFiles() {
        viewModelScope.launch {
            val paths = workspaceFiles.value.map { it.path }.toSet()
            dataStoreManager.setGitStagedPaths(paths)
            appendConsoleLog("info", "Staged all workspace files (${paths.size} items)")
        }
    }

    fun gitUnstageAllFiles() {
        viewModelScope.launch {
            dataStoreManager.setGitStagedPaths(emptySet())
            appendConsoleLog("info", "Unstaged all workspace files")
        }
    }

    fun gitCommit(message: String, author: String = "Ceezix Developer") {
        if (message.isBlank()) return
        viewModelScope.launch {
            saveActiveFileContent()
            
            val files = workspaceFiles.value
            val staged = _gitStagedPaths.value
            
            if (staged.isEmpty()) {
                appendConsoleLog("error", "Nothing staged to commit (use git add).")
                return@launch
            }

            // Create a file snapshot list
            val snapshots = files.map { file ->
                WorkspaceFileSnapshot(
                    name = file.name,
                    path = file.path,
                    isFolder = file.isFolder,
                    content = file.content
                )
            }
            
            val json = try {
                snapshotAdapter.toJson(snapshots)
            } catch (e: Exception) {
                ""
            }
            
            val hash = java.util.UUID.randomUUID().toString().replace("-", "").take(7)
            
            val newCommit = GitCommitEntity(
                hash = hash,
                message = message,
                author = author,
                timestamp = System.currentTimeMillis(),
                filesJson = json
            )
            
            workspaceRepository.insertCommit(newCommit)
            dataStoreManager.setGitStagedPaths(emptySet())
            
            appendConsoleLog("info", "Commit successful: [$hash] $message")
        }
    }

    fun gitRemoteAdd(url: String) {
        viewModelScope.launch {
            dataStoreManager.setGitRemoteUrl(url)
            appendConsoleLog("info", "Remote origin configured: $url")
        }
    }

    fun gitPush() {
        val remote = _gitRemoteUrl.value
        if (remote.isBlank()) {
            appendConsoleLog("error", "No remote configured. Add a remote URL first.")
            return
        }
        viewModelScope.launch {
            appendConsoleLog("info", "Connecting to remote $remote...")
            appendConsoleLog("info", "Pushing local commits to branch 'main'...")
            
            val localCommits = _gitCommits.value
            if (localCommits.isEmpty()) {
                appendConsoleLog("warn", "No commits found to push.")
                return@launch
            }
            
            localCommits.forEach { commit ->
                dataStoreManager.addGitPushedCommit(commit.hash)
            }
            appendConsoleLog("info", "Pushed ${localCommits.size} commits. Tracking branch 'origin/main' updated.")
        }
    }

    fun gitPull() {
        val remote = _gitRemoteUrl.value
        if (remote.isBlank()) {
            appendConsoleLog("error", "No remote configured. Add a remote URL first.")
            return
        }
        viewModelScope.launch {
            appendConsoleLog("info", "Fetching origin branch updates from $remote...")
            appendConsoleLog("info", "Pulling latest updates...")
            
            // To make pull action visual, we append a README line if files are available
            val files = workspaceFiles.value
            val readme = files.find { it.name.lowercase() == "readme.md" }
            if (readme != null) {
                val updatedContent = readme.content + "\n\n---\n*Fetched and pulled from remote origin at ${java.text.SimpleDateFormat.getDateTimeInstance().format(java.util.Date())}*"
                workspaceRepository.updateFileContent(readme.id, updatedContent)
                
                // Refresh list
                val updated = workspaceRepository.getFileById(readme.id)
                updated?.let {
                    val currentList = workspaceFiles.value.toMutableList()
                    val idx = currentList.indexOfFirst { item -> item.id == readme.id }
                    if (idx >= 0) {
                        currentList[idx] = it
                        workspaceFiles.value = currentList
                    }
                }
            }
            
            appendConsoleLog("info", "Successfully pulled remote changes. Local files up-to-date with branch 'origin/main'.")
        }
    }

    fun gitCheckout(hash: String) {
        viewModelScope.launch {
            val commit = workspaceRepository.getCommitByHash(hash) ?: return@launch
            val snapshots = try {
                snapshotAdapter.fromJson(commit.filesJson)
            } catch (e: Exception) {
                null
            } ?: return@launch
            
            workspaceRepository.clearWorkspace()
            val newWorkspaceFiles = snapshots.map { snap ->
                WorkspaceFile(
                    name = snap.name,
                    path = snap.path,
                    isFolder = snap.isFolder,
                    content = snap.content
                )
            }
            workspaceRepository.insertFiles(newWorkspaceFiles)
            
            // Clear current tabs
            _openTabIds.value = emptyList()
            _selectedFileId.value = null
            
            // Wait for file updates to reload the list
            workspaceRepository.allFiles.collect { files ->
                if (files.isNotEmpty()) {
                    val firstFile = files.find { !it.isFolder } ?: files.first()
                    openFileInTab(firstFile)
                }
            }
            
            recompilePreviewData()
            appendConsoleLog("info", "Checked out commit $hash. Workspace state restored successfully.")
        }
    }

    fun gitResetHard() {
        viewModelScope.launch {
            val lastCommit = _gitCommits.value.firstOrNull()
            if (lastCommit == null) {
                appendConsoleLog("error", "No commits found to reset to.")
                return@launch
            }
            gitCheckout(lastCommit.hash)
            appendConsoleLog("info", "Hard reset complete. Reverted all unstaged changes to commit ${lastCommit.hash}.")
        }
    }
}
