package io.ceezix.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.viewinterop.AndroidView
import io.ceezix.cryptography.CezixViewModel
import io.ceezix.cryptography.ConsoleLogEntry
import io.ceezix.data.WorkspaceFile
import io.ceezix.models.ChallengeDatabase
import io.ceezix.ui.components.CipherWheel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.regex.Pattern

// --- High Performance Visual Syntax Highlighter with Multiple Theme Palettes and Universal Multi-Language Support ---
class CoreSyntaxHighlighter(val extension: String, val theme: String) : VisualTransformation {
    
    companion object {
        // Pre-compiled regex patterns to avoid recompiling on every keystroke/draw pass
        private val HTML_TAG_NAME_PATTERN = Pattern.compile("</?([a-zA-Z0-9:-]+)")
        private val HTML_TAG_BRACKET_PATTERN = Pattern.compile("(<|/>|>|/|&lt;|&gt;)")
        private val HTML_ATTR_PATTERN = Pattern.compile("\\b([a-zA-Z0-9:-]+)\\s*=")
        private val HTML_VAR_PATTERN = Pattern.compile("\\{\\{\\s*([a-zA-Z0-9_$.-]+)\\s*\\}\\}")
        private val HTML_STR_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'")
        private val HTML_COMMENT_PATTERN = Pattern.compile("<!--[\\s\\S]*?-->")

        private val CSS_PROP_PATTERN = Pattern.compile("([\\w-]+)\\s*:")
        private val CSS_KEYWORDS_PATTERN = Pattern.compile("(@\\w+|!important|inherit|initial|unset|none)\\b")
        private val CSS_NUM_PATTERN = Pattern.compile("\\b(\\d+)(px|%|em|rem|vh|vw|deg|ms|s)?\\b")
        private val CSS_VAR_PATTERN = Pattern.compile("(--[\\w-]+)\\b")
        private val CSS_STR_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'")
        private val CSS_COMMENT_PATTERN = Pattern.compile("/\\*[\\s\\S]*?\\*/")

        private val JS_KEYWORDS_PATTERN = Pattern.compile("\\b(const|let|var|function|return|if|else|for|while|class|import|export|true|false|this|new|null|try|catch|finally|throw|break|continue|switch|case|default|async|await|typeof|instanceof|extends|super|interface|type|from|package|public|private|protected|static|readonly|any)\\b")
        private val JS_VAR_DECL_PATTERN = Pattern.compile("\\b(?:const|let|var)\\s+([a-zA-Z_\$][\\w\$]*)")
        private val JS_VAR_ASSIGN_PATTERN = Pattern.compile("\\b([a-zA-Z_\$][\\w\$]*)\\s*=(?!=)")
        private val JS_STR_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'|`([^`]*)`")
        private val JS_NUM_PATTERN = Pattern.compile("\\b(\\d+)\\b")
        private val JS_COMMENT_PATTERN = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")

        private val KT_KEYWORDS_PATTERN = Pattern.compile("\\b(package|import|class|interface|fun|val|var|return|if|else|for|while|when|is|as|null|true|false|this|super|override|private|public|protected|internal|companion|object|typealias|constructor|init|get|set|by|throw|try|catch|finally|break|continue|in|out|where|data)\\b")
        private val KT_ANN_PATTERN = Pattern.compile("@[\\w]+")
        private val KT_TYPE_PATTERN = Pattern.compile("\\b(Int|String|Boolean|Double|Float|Long|Char|Byte|Short|Any|Unit|List|Map|Set|StateFlow|MutableStateFlow|State|MutableState|ViewModel|Composable|Modifier|Color|Column|Row|Box|Text|Button|Card|Activity)\\b")
        private val KT_STR_PATTERN = Pattern.compile("\"\"\"[\\s\\S]*?\"\"\"|\"([^\"]*)\"|'([^']*)'")
        private val KT_NUM_PATTERN = Pattern.compile("\\b(\\d+L|\\d+f|\\d+\\.\\d+|\\d+)\\b")
        private val KT_COMMENT_PATTERN = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")

        private val PY_KEYWORDS_PATTERN = Pattern.compile("\\b(def|class|import|from|as|return|if|elif|else|while|for|in|try|except|finally|raise|None|True|False|and|or|not|pass|with|yield|lambda|is|global|assert|break|continue|del|nonlocal)\\b")
        private val PY_FUNC_PATTERN = Pattern.compile("\\b(print|len|range|str|int|float|list|dict|set|tuple|open|type|enumerate|zip)\\s*(?=[(])")
        private val PY_VAR_ASSIGN_PATTERN = Pattern.compile("\\b([a-zA-Z_][\\w]*)\\s*=(?!=)")
        private val PY_SELF_VAR_PATTERN = Pattern.compile("\\bself\\.([a-zA-Z_][\\w]*)")
        private val PY_STR_PATTERN = Pattern.compile("\"\"\"[\\s\\S]*?\"\"\"|'''[\\s\\S]*?'''|\"([^\"]*)\"|'([^']*)'")
        private val PY_NUM_PATTERN = Pattern.compile("\\b(\\d+\\.\\d+|\\d+)\\b")
        private val PY_COMMENT_PATTERN = Pattern.compile("#.*")

        private val GO_KEYWORDS_PATTERN = Pattern.compile("\\b(package|import|func|var|const|type|struct|interface|return|if|else|for|range|switch|case|default|go|chan|select|defer|fallthrough|map|nil|true|false|make|new|len|cap|append|panic|recover)\\b")
        private val GO_STR_PATTERN = Pattern.compile("\"([^\"]*)\"|`([^`]*)`|'([^']*)'")
        private val GO_NUM_PATTERN = Pattern.compile("\\b(\\d+)\\b")
        private val GO_COMMENT_PATTERN = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")

        private val RS_KEYWORDS_PATTERN = Pattern.compile("\\b(fn|let|mut|pub|use|mod|struct|enum|impl|trait|match|if|else|for|while|loop|return|true|false|self|Self|type|as|unsafe|extern|crate|in|ref|static|const|where|dyn|impl|move|macro_rules)\\b")
        private val RS_ANN_PATTERN = Pattern.compile("#!?\\[.*?\\]")
        private val RS_STR_PATTERN = Pattern.compile("\"([^\"]*)\"|'(.?)'")
        private val RS_NUM_PATTERN = Pattern.compile("\\b(\\d+(?:_\\d+)*(?:u8|u16|u32|u64|u128|i8|i16|i32|i64|i128|f32|f64)?)\\b")
        private val RS_COMMENT_PATTERN = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")

        private val CPP_KEYWORDS_PATTERN = Pattern.compile("\\b(int|float|double|char|void|bool|class|struct|public|private|protected|return|if|else|for|while|do|switch|case|default|static|const|volatile|virtual|override|new|delete|namespace|using|std|throw|try|catch|template|typename|explicit|friend|inline|operator)\\b")
        private val CPP_MACROS_PATTERN = Pattern.compile("(#include\\s*(?:<.*?>|\"[^\"]*\")|#define\\b|#undef\\b|#ifdef\\b|#ifndef\\b|#endif\\b|#if\\b|#else\\b)")
        private val CPP_STR_PATTERN = Pattern.compile("\"([^\"]*)\"|'([^']*)'")
        private val CPP_COMMENT_PATTERN = Pattern.compile("//.*|/\\*[\\s\\S]*?\\*/")

        private val SQL_KEYWORDS_PATTERN = Pattern.compile("(?i)\\b(SELECT|FROM|WHERE|INSERT|INTO|VALUES|UPDATE|SET|DELETE|CREATE|TABLE|DROP|ALTER|INDEX|JOIN|ON|LEFT|RIGHT|INNER|OUTER|GROUP|BY|ORDER|LIMIT|AS|AND|OR|NOT|IN|IS|NULL|HAVING|UNION|ALL|DISTINCT|PRIMARY|KEY|FOREIGN|REFERENCES|DEFAULT|AUTO_INCREMENT|VARCHAR|INT|BOOLEAN|TEXT|DATE)\\b")
        private val SQL_FUNC_PATTERN = Pattern.compile("(?i)\\b(COUNT|SUM|AVG|MIN|MAX|CONCAT|COALESCE|NOW|DATE_SUB|DATE_ADD)\\s*(?=[(])")
        private val SQL_STR_PATTERN = Pattern.compile("'([^']*)'|\"([^\"]*)\"")
        private val SQL_COMMENT_PATTERN = Pattern.compile("--.*|/\\*[\\s\\S]*?\\*/")

        private val MD_HEADER_PATTERN = Pattern.compile("(?m)^#+.*$")
        private val MD_BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*")
        private val MD_ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*|_(.*?)_")
        private val MD_BLOCK_PATTERN = Pattern.compile("```[\\s\\S]*?```")
        private val MD_INLINE_PATTERN = Pattern.compile("`([^`]+)`")

        private const val MAX_SYNTAX_HIGHLIGHT_LENGTH = 15000
    }

    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        
        // Fast-path performance ceiling: skip heavy regex processing of massive files
        // to prevent UI thread ANRs and InputDispatcher breakdown crashes.
        if (originalText.length > MAX_SYNTAX_HIGHLIGHT_LENGTH) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(originalText)
        
        // Dynamic theme customization styling palettes
        val colorsList = when (theme) {
            "Monokai" -> listOf(
                Color(0xFFF92672),      // Vivid Neon Pink
                Color(0xFF66D9EF),      // Aqua Blue
                Color(0xFFE6DB74),      // Warm Buttercup Yellow
                Color(0xFFAE81FF),      // Celestial Lavender Purple
                Color(0xFF75715E),      // Charcoal Sage Gray
                Color(0xFFF92672),      // Vivid Crimson Tag Outline
                Color(0xFFA6E22E),      // Fresh Lime Green
                Color(0xFFFD971F),       // Soft Apricot Gold
                Color(0xFFFF9742)       // Rich Sunset Peach-Orange for Variables
            )
            "Code Green" -> listOf(
                Color(0xFF00FF00),      // Crisp Matrix Green
                Color(0xFF32CD32),      // Lime Green Accent
                Color(0xFFADFF2F),      // Light Neon Green-Yellow
                Color(0xFF00FF7F),      // High-Contrast Spring Green
                Color(0xFF228B22),      // Deep Cyber Forest Gray-Green
                Color(0xFF00FF00),      // Pure Green Borders
                Color(0xFF7FFF00),      // Bright Citron
                Color(0xFF98FB98),       // Soft Mint Accent
                Color(0xFFE0F7FA)       // Ice Menthol Blue-Green for Variables
            )
            "Retro Amber" -> listOf(
                Color(0xFFFFB266),      // Bright Amber Phosphor
                Color(0xFFFFCC99),      // Soft Cream Peach
                Color(0xFFFFE5CC),      // Light Candlelight Tint
                Color(0xFFFF9933),      // Dark Golden Yellow
                Color(0xFF804000),      // Muted Copper Walnut Brown
                Color(0xFFFF8000),      // Neon Orange Accent
                Color(0xFFFFB266),      // Classic Muted Gold
                Color(0xFFFFD9B3),       // Soft Sunset Amber Glow
                Color(0xFFFFCC00)       // Bright Chrome Amber for Variables
            )
            "Nordic Frost" -> listOf(
                Color(0xFF81A1C1),      // Crisp Polar Ice Blue
                Color(0xFF88C0D0),      // Deep Ocean Turquoise
                Color(0xFFA3BE8C),      // Mossy Forest Sage
                Color(0xFFB48EAD),      // Aurora Orchid Purple
                Color(0xFF4C566A),      // Ice-bound Granite Gray
                Color(0xFFBF616A),      // Warm Autumn Coral
                Color(0xFFEBCB8B),      // Amber Glow Gold
                Color(0xFFD8DEE9),       // Snowdrift White
                Color(0xFF8FBCBB)       // Frostbite Light Green-Teal for Variables
            )
            "Solarized Light" -> listOf(
                Color(0xFF268BD2),      // Premium Electric Blue
                Color(0xFF2AA198),      // Mediterranean Teal
                Color(0xFF859900),      // Olive-Grass Green
                Color(0xFFD33682),      // Radiant Magenta Purple
                Color(0xFF93A1A1),      // Cool Shadow Gray
                Color(0xFFDC322F),      // Crimson Solar Red
                Color(0xFFCB4B16),      // Bold Clay Orange
                Color(0xFFB58900),       // Deep Amber Ochre
                Color(0xFF268BD2)       // Electric Blue for Variables
            )
            "Github Light" -> listOf(
                Color(0xFFCF222E),      // Crisp Crimson
                Color(0xFF953800),      // Wood Brown
                Color(0xFF0A3069),      // Royal Indigo Blue
                Color(0xFF0550AE),      // Navy Sea Blue
                Color(0xFF6E7781),      // Cool Fog Gray
                Color(0xFF116329),      // Deep Shamrock Green
                Color(0xFF805A00),      // Harvest Ochre
                Color(0xFF0550AE),       // Pure Royal Blue
                Color(0xFF24292F)       // Deep Charcoal Black for Variables
            )
            else -> listOf( // "Dracula Dark" as default comfortable night theme
                Color(0xFFFF79C6),      // High-Contrast Orchid Pink
                Color(0xFF8BE9FD),      // Sky Celestial Cyan
                Color(0xFFF1FA8C),      // Pale Buttercup Yellow
                Color(0xFFBD93F9),      // Electric Nebula Purple
                Color(0xFF6272A4),      // Cool Slate Lavender
                Color(0xFFFF5555),      // Radiant Coral Red
                Color(0xFF50FA7B),      // Lucid Spring Green
                Color(0xFFFFB86C),       // Soft Gold Orange
                Color(0xFFFFB86C)       // Soft Apricot Orange for Variables
            )
        }

        val keywordColor = colorsList[0]
        val typeColor = colorsList[1]
        val stringColor = colorsList[2]
        val numberColor = colorsList[3]
        val commentColor = colorsList[4]
        val tagColor = colorsList[5]
        val attrColor = colorsList[6]
        val annotationColor = colorsList[7]
        val variableColor = colorsList[8]
 
        try {
            val ext = extension.lowercase()
            when (ext) {
                "html", "xml", "xhtml" -> {
                    // HTML tag names highlighted as keywords (e.g. main tag terms)
                    val tagNameMatcher = HTML_TAG_NAME_PATTERN.matcher(originalText)
                    while (tagNameMatcher.find()) {
                        if (tagNameMatcher.groupCount() >= 1) {
                            builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), tagNameMatcher.start(1), tagNameMatcher.end(1))
                        }
                    }

                    // HTML general tag syntax <, >, </, />
                    val bracketMatcher = HTML_TAG_BRACKET_PATTERN.matcher(originalText)
                    while (bracketMatcher.find()) {
                        builder.addStyle(SpanStyle(color = tagColor), bracketMatcher.start(), bracketMatcher.end())
                    }

                    // HTML Attributes mapped as visual variables
                    val attrMatcher = HTML_ATTR_PATTERN.matcher(originalText)
                    while (attrMatcher.find()) {
                        if (attrMatcher.groupCount() >= 1) {
                            builder.addStyle(SpanStyle(color = variableColor), attrMatcher.start(1), attrMatcher.end(1))
                        }
                    }

                    // Handle Template evaluation variables e.g. {{ variable }}
                    val tVarMatcher = HTML_VAR_PATTERN.matcher(originalText)
                    while (tVarMatcher.find()) {
                        builder.addStyle(SpanStyle(color = variableColor, fontWeight = FontWeight.SemiBold), tVarMatcher.start(), tVarMatcher.end())
                    }

                    // HTML Attribute values double and single-quoted strings
                    val strMatcher = HTML_STR_PATTERN.matcher(originalText)
                    while (strMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
                    }

                    // HTML commenting block
                    val commentMatcher = HTML_COMMENT_PATTERN.matcher(originalText)
                    while (commentMatcher.find()) {
                        builder.addStyle(SpanStyle(color = commentColor), commentMatcher.start(), commentMatcher.end())
                    }
                }
                "css" -> {
                    // CSS properties (e.g. background-color:, margin-top:)
                    val propMatcher = CSS_PROP_PATTERN.matcher(originalText)
                    while (propMatcher.find()) {
                        if (propMatcher.groupCount() >= 1) {
                            builder.addStyle(SpanStyle(color = typeColor), propMatcher.start(1), propMatcher.end(1))
                        }
                    }

                    // CSS at-rules / media keywords
                    val keyMatcher = CSS_KEYWORDS_PATTERN.matcher(originalText)
                    while (keyMatcher.find()) {
                        builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold), keyMatcher.start(), keyMatcher.end())
                    }
 
                    // CSS numeric measurement values
                    val numMatcher = CSS_NUM_PATTERN.matcher(originalText)
                    while (numMatcher.find()) {
                        builder.addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
                    }

                    // CSS custom variables (--custom-variable-color)
                    val varMatcher = CSS_VAR_PATTERN.matcher(originalText)
                    while (varMatcher.find()) {
                        builder.addStyle(SpanStyle(color = variableColor, fontWeight = FontWeight.SemiBold), varMatcher.start(), varMatcher.end())
                    }
 
                    // CSS string values
                    val strMatcher = CSS_STR_PATTERN.matcher(originalText)
                    while (strMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
                    }
 
                    // CSS comment block /* ... */
                    val commentMatcher = CSS_COMMENT_PATTERN.matcher(originalText)
                    while (commentMatcher.find()) {
                        builder.addStyle(SpanStyle(color = commentColor), commentMatcher.start(), commentMatcher.end())
                    }
                }
                "js", "javascript", "json", "ts", "typescript" -> {
                    // Standard Javascript/Typescript keywords
                    val matcher = JS_KEYWORDS_PATTERN.matcher(originalText)
                    while (matcher.find()) {
                        builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold), matcher.start(), matcher.end())
                    }

                    // Variable Declarations (const name, let name, var name)
                    val varDeclMatcher = JS_VAR_DECL_PATTERN.matcher(originalText)
                    while (varDeclMatcher.find()) {
                        if (varDeclMatcher.groupCount() >= 1) {
                            builder.addStyle(SpanStyle(color = variableColor, fontWeight = FontWeight.Normal), varDeclMatcher.start(1), varDeclMatcher.end(1))
                        }
                    }

                    // Variable Assignments (name = value)
                    val varAssignMatcher = JS_VAR_ASSIGN_PATTERN.matcher(originalText)
                    while (varAssignMatcher.find()) {
                        if (varAssignMatcher.groupCount() >= 1) {
                            builder.addStyle(SpanStyle(color = variableColor, fontWeight = FontWeight.Normal), varAssignMatcher.start(1), varAssignMatcher.end(1))
                        }
                    }
 
                    // String literals including backtick strings
                    val strMatcher = JS_STR_PATTERN.matcher(originalText)
                    while (strMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
                    }
 
                    // Numbers
                    val numMatcher = JS_NUM_PATTERN.matcher(originalText)
                    while (numMatcher.find()) {
                        builder.addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
                    }
 
                    // Developer remarks comments
                    val commMatcher = JS_COMMENT_PATTERN.matcher(originalText)
                    while (commMatcher.find()) {
                        builder.addStyle(SpanStyle(color = commentColor), commMatcher.start(), commMatcher.end())
                    }
                }
                "kt", "kotlin", "java" -> {
                    // Native JVM-centric programming statement keys
                    val matcher = KT_KEYWORDS_PATTERN.matcher(originalText)
                    while (matcher.find()) {
                        builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold), matcher.start(), matcher.end())
                    }
 
                    // Annotations like @Composable, @OptIn, @Override etc.
                    val annMatcher = KT_ANN_PATTERN.matcher(originalText)
                    while (annMatcher.find()) {
                        builder.addStyle(SpanStyle(color = annotationColor, fontWeight = FontWeight.Bold), annMatcher.start(), annMatcher.end())
                    }
 
                    // Standard visual API types
                    val typeMatcher = KT_TYPE_PATTERN.matcher(originalText)
                    while (typeMatcher.find()) {
                        builder.addStyle(SpanStyle(color = typeColor), typeMatcher.start(), typeMatcher.end())
                    }
 
                    // Strings (supports triple quotes)
                    val strMatcher = KT_STR_PATTERN.matcher(originalText)
                    while (strMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
                    }
 
                    // Multi-suffixed numeric constants
                    val numMatcher = KT_NUM_PATTERN.matcher(originalText)
                    while (numMatcher.find()) {
                        builder.addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
                    }
 
                    // Comment entries
                    val commMatcher = KT_COMMENT_PATTERN.matcher(originalText)
                    while (commMatcher.find()) {
                        builder.addStyle(SpanStyle(color = commentColor), commMatcher.start(), commMatcher.end())
                    }
                }
                "py", "python" -> {
                    // Python language structure statements
                    val matcher = PY_KEYWORDS_PATTERN.matcher(originalText)
                    while (matcher.find()) {
                        builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), matcher.start(), matcher.end())
                    }
 
                    // Python basic functions highlighting
                    val funcMatcher = PY_FUNC_PATTERN.matcher(originalText)
                    while (funcMatcher.find()) {
                        if (funcMatcher.groupCount() >= 1) {
                            builder.addStyle(SpanStyle(color = typeColor), funcMatcher.start(1), funcMatcher.end(1))
                        }
                    }

                    // Variable Assignments
                    val varAssignMatcher = PY_VAR_ASSIGN_PATTERN.matcher(originalText)
                    while (varAssignMatcher.find()) {
                        if (varAssignMatcher.groupCount() >= 1) {
                            builder.addStyle(SpanStyle(color = variableColor, fontWeight = FontWeight.Normal), varAssignMatcher.start(1), varAssignMatcher.end(1))
                        }
                    }

                    // Self Properties / Variables
                    val selfVarMatcher = PY_SELF_VAR_PATTERN.matcher(originalText)
                    while (selfVarMatcher.find()) {
                        if (selfVarMatcher.groupCount() >= 1) {
                            builder.addStyle(SpanStyle(color = variableColor, fontWeight = FontWeight.Normal), selfVarMatcher.start(1), selfVarMatcher.end(1))
                        }
                    }
 
                    // Triple/Single/Double quotes python strings
                    val strMatcher = PY_STR_PATTERN.matcher(originalText)
                    while (strMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
                    }
 
                    // Standalone numeric variables
                    val numMatcher = PY_NUM_PATTERN.matcher(originalText)
                    while (numMatcher.find()) {
                        builder.addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
                    }
 
                    // Python comments with '#' symbol
                    val commentMatcher = PY_COMMENT_PATTERN.matcher(originalText)
                    while (commentMatcher.find()) {
                        builder.addStyle(SpanStyle(color = commentColor), commentMatcher.start(), commentMatcher.end())
                    }
                }
                "go", "golang" -> {
                    // Go core structural keywords
                    val matcher = GO_KEYWORDS_PATTERN.matcher(originalText)
                    while (matcher.find()) {
                        builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold), matcher.start(), matcher.end())
                    }
 
                    // Go string types and character literals
                    val strMatcher = GO_STR_PATTERN.matcher(originalText)
                    while (strMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
                    }
 
                    // Numbers
                    val numMatcher = GO_NUM_PATTERN.matcher(originalText)
                    while (numMatcher.find()) {
                        builder.addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
                    }
 
                    // Go comments
                    val commentMatcher = GO_COMMENT_PATTERN.matcher(originalText)
                    while (commentMatcher.find()) {
                        builder.addStyle(SpanStyle(color = commentColor), commentMatcher.start(), commentMatcher.end())
                    }
                }
                "rs", "rust" -> {
                    // Rust safety and systems keywords
                    val matcher = RS_KEYWORDS_PATTERN.matcher(originalText)
                    while (matcher.find()) {
                        builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold), matcher.start(), matcher.end())
                    }
 
                    // Rust procedural macro annotations
                    val annMatcher = RS_ANN_PATTERN.matcher(originalText)
                    while (annMatcher.find()) {
                        builder.addStyle(SpanStyle(color = annotationColor, fontWeight = FontWeight.Bold), annMatcher.start(), annMatcher.end())
                    }
 
                    // Strings and characters
                    val strMatcher = RS_STR_PATTERN.matcher(originalText)
                    while (strMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
                    }
 
                    // Numbers
                    val numMatcher = RS_NUM_PATTERN.matcher(originalText)
                    while (numMatcher.find()) {
                        builder.addStyle(SpanStyle(color = numberColor), numMatcher.start(), numMatcher.end())
                    }
 
                    // Comments
                    val commentMatcher = RS_COMMENT_PATTERN.matcher(originalText)
                    while (commentMatcher.find()) {
                        builder.addStyle(SpanStyle(color = commentColor), commentMatcher.start(), commentMatcher.end())
                    }
                }
                "cpp", "c", "h", "hpp", "cc" -> {
                    // C++ native elements
                    val matcher = CPP_KEYWORDS_PATTERN.matcher(originalText)
                    while (matcher.find()) {
                        builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold), matcher.start(), matcher.end())
                    }
 
                    // Preprocessor macros and library inclusions
                    val macrosMatcher = CPP_MACROS_PATTERN.matcher(originalText)
                    while (macrosMatcher.find()) {
                        builder.addStyle(SpanStyle(color = annotationColor), macrosMatcher.start(), macrosMatcher.end())
                    }
 
                    // Strings
                    val strMatcher = CPP_STR_PATTERN.matcher(originalText)
                    while (strMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
                    }
 
                    // Comments
                    val commentMatcher = CPP_COMMENT_PATTERN.matcher(originalText)
                    while (commentMatcher.find()) {
                        builder.addStyle(SpanStyle(color = commentColor), commentMatcher.start(), commentMatcher.end())
                    }
                }
                "sql" -> {
                    // SQL statements
                    val matcher = SQL_KEYWORDS_PATTERN.matcher(originalText)
                    while (matcher.find()) {
                        builder.addStyle(SpanStyle(color = keywordColor, fontWeight = FontWeight.Bold), matcher.start(), matcher.end())
                    }
 
                    // SQL custom functions
                    val funcMatcher = SQL_FUNC_PATTERN.matcher(originalText)
                    while (funcMatcher.find()) {
                        builder.addStyle(SpanStyle(color = typeColor), funcMatcher.start(1), funcMatcher.end(1))
                    }
 
                    // SQL text strings
                    val strMatcher = SQL_STR_PATTERN.matcher(originalText)
                    while (strMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor), strMatcher.start(), strMatcher.end())
                    }
 
                    // SQL comments
                    val commentMatcher = SQL_COMMENT_PATTERN.matcher(originalText)
                    while (commentMatcher.find()) {
                        builder.addStyle(SpanStyle(color = commentColor), commentMatcher.start(), commentMatcher.end())
                    }
                }
                "md", "markdown" -> {
                    // Headers
                    val headerMatcher = MD_HEADER_PATTERN.matcher(originalText)
                    while (headerMatcher.find()) {
                        builder.addStyle(SpanStyle(color = typeColor, fontWeight = FontWeight.Bold), headerMatcher.start(), headerMatcher.end())
                    }
 
                    // Bold tags **bold**
                    val boldMatcher = MD_BOLD_PATTERN.matcher(originalText)
                    while (boldMatcher.find()) {
                        builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = attrColor), boldMatcher.start(), boldMatcher.end())
                    }
 
                    // Italic tags *italic* or _italic_
                    val italicMatcher = MD_ITALIC_PATTERN.matcher(originalText)
                    while (italicMatcher.find()) {
                        builder.addStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic), italicMatcher.start(), italicMatcher.end())
                    }
 
                    // Codeblocks ``` ... ```
                    val codeBlockMatcher = MD_BLOCK_PATTERN.matcher(originalText)
                    while (codeBlockMatcher.find()) {
                        builder.addStyle(SpanStyle(color = numberColor, fontFamily = FontFamily.Monospace), codeBlockMatcher.start(), codeBlockMatcher.end())
                    }
 
                    // Inline code blocks
                    val codeMatcher = MD_INLINE_PATTERN.matcher(originalText)
                    while (codeMatcher.find()) {
                        builder.addStyle(SpanStyle(color = stringColor, fontFamily = FontFamily.Monospace), codeMatcher.start(), codeMatcher.end())
                    }
                }
            }
        } catch (e: Exception) {
            // Graceful render fallback
        }
 
        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: CezixViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    // Observe core Workspace States
    val activeTab by viewModel.activeTab.collectAsState()
    val workspaceFiles by viewModel.workspaceFiles.collectAsState()
    val openTabIds by viewModel.openTabIds.collectAsState()
    val selectedFileId by viewModel.selectedFileId.collectAsState()
    val editorContent by viewModel.editorContent.collectAsState()
    val editorTheme by viewModel.editorTheme.collectAsState()
    val editorFontSize by viewModel.editorFontSize.collectAsState()

    // Observe Profile/Progression states
    val userXp by viewModel.userXp.collectAsState()
    val totalSolves by viewModel.totalSolves.collectAsState()
    val currentStreak by viewModel.currentStreak.collectAsState()
    val savedCodes by viewModel.savedCodes.collectAsState()

    // Observe Live Preview + Console
    val compositeHtmlOutput by viewModel.compositeHtmlOutput.collectAsState()
    val consoleLogs by viewModel.consoleLogs.collectAsState()

    // Observe AI Pairing
    val aiOutput by viewModel.aiOutput.collectAsState()
    val isAiLoading by viewModel.isAiLoading.collectAsState()

    // Local UI states
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isNewFileDialogOpen by remember { mutableStateOf(false) }
    var newFileNameInput by remember { mutableStateOf("") }
    var newFileTypeFolder by remember { mutableStateOf(false) }

    var aiCustomPromptInput by remember { mutableStateOf("") }
    var showCipherSandboxModal by remember { mutableStateOf(false) }
    var showDependencyManagerModal by remember { mutableStateOf(false) }
    var showTermuxTerminalModal by remember { mutableStateOf(false) }
    var showGitModal by remember { mutableStateOf(false) }

    val activeSelectedFile = workspaceFiles.find { file -> file.id == selectedFileId }
    val activeFileExtension = activeSelectedFile?.name?.substringAfterLast(".", "") ?: "html"

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { viewModel.selectActiveTab(0) },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Developer Workspace") },
                    label = { Text("Workspace", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_workspace")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { viewModel.selectActiveTab(1) },
                    icon = { Icon(Icons.Default.PlayArrow, contentDescription = "Live Preview Frame") },
                    label = { Text("Live Run", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_liverun")
                )
                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { viewModel.selectActiveTab(2) },
                    icon = { Icon(Icons.Default.Book, contentDescription = "Saved Notebook") },
                    label = { Text("Codebook", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_codebook")
                )
                NavigationBarItem(
                    selected = activeTab == 3,
                    onClick = { viewModel.selectActiveTab(3) },
                    icon = { Icon(Icons.Default.SupportAgent, contentDescription = "AI Assistant Copilot") },
                    label = { Text("AI Copilot", fontSize = 11.sp) },
                    modifier = Modifier.testTag("nav_tab_ai_copilot")
                )
            }
        },
        floatingActionButton = {
            // Preserving precise prompt target requirement for FAB in Codebook Tab!
            if (activeTab == 2 && savedCodes.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = {
                        val formattedDoc = buildString {
                            appendLine("==============================================")
                            appendLine("       CEEZIX - SAVED CODEBOOK LOG            ")
                            appendLine("==============================================")
                            appendLine("Total Entries : ${savedCodes.size}")
                            appendLine()
                            savedCodes.forEachIndexed { index, entry ->
                                val segments = entry.split("|:|:")
                                if (segments.size >= 3) {
                                    val shift = segments[0]
                                    val raw = segments[1]
                                    val dec = segments[2]
                                    appendLine("[LOG ENTRY #${index + 1}]")
                                    appendLine("KEY/SHIFT : $shift")
                                    appendLine("RAW SIGNAL: $raw")
                                    appendLine("DECRYPTED : $dec")
                                    appendLine("----------------------------------------------")
                                    appendLine()
                                } else {
                                    // Raw snippet logging support
                                    appendLine("[SNIPPET ENTRY #${index + 1}]")
                                    appendLine("CONTENT:")
                                    appendLine(entry)
                                    appendLine("----------------------------------------------")
                                    appendLine()
                                }
                            }
                            appendLine("==============================================")
                        }
                        clipboardManager.setText(AnnotatedString(formattedDoc))
                        Toast.makeText(context, "Logbook exported to clip drawer!", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.testTag("export_clipboard_fab"),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Export notes"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Export Logs",
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (activeTab) {
                0 -> {
                    // --- DEVELOPER WORKSPACE TAB ---
                    WorkspaceEditorTab(
                        viewModel = viewModel,
                        workspaceFiles = workspaceFiles,
                        openTabIds = openTabIds,
                        selectedFileId = selectedFileId,
                        editorContent = editorContent,
                        editorTheme = editorTheme,
                        editorFontSize = editorFontSize,
                        activeFileExtension = activeFileExtension,
                        activeSelectedFile = activeSelectedFile,
                        isDrawerOpen = isDrawerOpen,
                        onToggleDrawer = { isDrawerOpen = !isDrawerOpen },
                        onOpenNewFileCreator = { isNewFileDialogOpen = true },
                        showCipherSandboxClick = { showCipherSandboxModal = true },
                        onManageDependenciesClick = { showDependencyManagerModal = true },
                        onTermuxTerminalClick = { showTermuxTerminalModal = true },
                        isTerminalActive = showTermuxTerminalModal,
                        onGitVersionControlClick = { showGitModal = true }
                    )
                }
                1 -> {
                    // --- WEB PREVIEW LIVE RUN TAB ---
                    LivePreviewTab(
                        compositeHtml = compositeHtmlOutput,
                        consoleLogs = consoleLogs,
                        onSendConsoleLog = { type, msg -> viewModel.appendConsoleLog(type, msg) },
                        onClearConsole = { viewModel.clearCompleteConsole() }
                    )
                }
                2 -> {
                    // --- CODEBOOK LOGS TAB ---
                    CodebookSavedTab(
                        savedCodes = savedCodes,
                        userXp = userXp,
                        totalSolves = totalSolves,
                        currentStreak = currentStreak,
                        onDeleteCode = { ent -> viewModel.deleteFromNotebook(ent) },
                        onQuickReset = { viewModel.quickResetProgress() },
                        showCipherSandboxClick = { showCipherSandboxModal = true }
                    )
                }
                3 -> {
                    // --- GEMINI POWERED AI COPILOT TAB ---
                    AiCopilotTab(
                        viewModel = viewModel,
                        aiOutput = aiOutput,
                        isAiLoading = isAiLoading,
                        activeSelectedFile = activeSelectedFile,
                        aiCustomPromptInput = aiCustomPromptInput,
                        onPromptChange = { aiCustomPromptInput = it },
                        onSendPrompt = { 
                            viewModel.askAiAboutCurrentCode(aiCustomPromptInput)
                            aiCustomPromptInput = "" // Clear after submission
                        }
                    )
                }
            }

            // --- Collapsible Lateral File System Drawer Overlay ---
            AnimatedVisibility(
                visible = isDrawerOpen,
                enter = slideInHorizontally { width -> -width } + fadeIn(),
                exit = slideOutHorizontally { width -> -width } + fadeOut(),
                modifier = Modifier.fillMaxHeight().width(280.dp).align(Alignment.TopStart)
            ) {
                FilesSystemSidebar(
                    workspaceFiles = workspaceFiles,
                    selectedFileId = selectedFileId,
                    onSelectFile = { file ->
                        viewModel.openFileInTab(file)
                        isDrawerOpen = false
                    },
                    onDeleteFile = { file -> viewModel.deleteFileFromWorkspace(file) },
                    onAddFileClick = { isNewFileDialogOpen = true },
                    onCloseSidebar = { isDrawerOpen = false },
                    onManageDependenciesClick = { showDependencyManagerModal = true }
                )
            }

            // --- Create New File/Folder Modal Dialog ---
            if (isNewFileDialogOpen) {
                AlertDialog(
                    shape = RoundedCornerShape(20.dp),
                    onDismissRequest = { isNewFileDialogOpen = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CreateNewFolder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Assemble New File Block", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = newFileNameInput,
                                onValueChange = { newFileNameInput = it },
                                label = { Text("File Name & Extension") },
                                placeholder = { Text("e.g. index.html, style.css, script.js", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Description,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                ),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth().testTag("new_file_name_input")
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                    .clickable { newFileTypeFolder = !newFileTypeFolder }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = newFileTypeFolder,
                                    onCheckedChange = { newFileTypeFolder = it },
                                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                                )
                                Text("Virtual Folder structure", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                if (newFileNameInput.isNotBlank()) {
                                    viewModel.createNewFileInWorkspace(newFileNameInput, newFileTypeFolder)
                                    newFileNameInput = ""
                                    newFileTypeFolder = false
                                    isNewFileDialogOpen = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Create File", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { isNewFileDialogOpen = false }
                        ) {
                            Text("Cancel", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                )
            }

            // --- Floating Backwards-Compatible Cipher Sandbox Modal ---
            if (showCipherSandboxModal) {
                CipherSandboxDialog(
                    viewModel = viewModel,
                    onDismiss = { showCipherSandboxModal = false }
                )
            }

            // --- Floating Dependency and Template Library Manager Modal ---
            if (showDependencyManagerModal) {
                DependencyManagerDialog(
                    viewModel = viewModel,
                    onDismiss = { showDependencyManagerModal = false }
                )
            }

            // --- Floating Termux Terminal Shell Modal ---
            if (showTermuxTerminalModal) {
                TermuxTerminalDialog(
                    viewModel = viewModel,
                    onDismiss = { showTermuxTerminalModal = false }
                )
            }

            // --- Floating Git Version Control System Modal ---
            if (showGitModal) {
                GitVersionControlDialog(
                    viewModel = viewModel,
                    onDismiss = { showGitModal = false }
                )
            }
        }
    }
}

// ==========================================
// 1. WORKSPACE EDITOR VIEW COMPONENT
// ==========================================
data class ThemeColors(
    val editorBgColor: Color,
    val gutterBgColor: Color,
    val gutterTextColor: Color,
    val editorTextColor: Color,
    val cursorColor: Color
)

@Composable
fun WorkspaceEditorTab(
    viewModel: CezixViewModel,
    workspaceFiles: List<WorkspaceFile>,
    openTabIds: List<Int>,
    selectedFileId: Int?,
    editorContent: String,
    editorTheme: String,
    editorFontSize: Int,
    activeFileExtension: String,
    activeSelectedFile: WorkspaceFile?,
    isDrawerOpen: Boolean,
    onToggleDrawer: () -> Unit,
    onOpenNewFileCreator: () -> Unit,
    showCipherSandboxClick: () -> Unit,
    onManageDependenciesClick: () -> Unit,
    onTermuxTerminalClick: () -> Unit,
    isTerminalActive: Boolean = false,
    onGitVersionControlClick: () -> Unit
) {
    val context = LocalContext.current
    var showSettingsShelf by remember { mutableStateOf(false) }
    var manualLanguageOverride by remember { mutableStateOf<String?>(null) }
    val currentRenderLanguage = manualLanguageOverride ?: activeFileExtension

    var textFieldValue by remember {
        mutableStateOf(TextFieldValue(text = editorContent))
    }

    LaunchedEffect(editorContent) {
        if (textFieldValue.text != editorContent) {
            textFieldValue = textFieldValue.copy(
                text = editorContent,
                selection = if (textFieldValue.selection.start <= editorContent.length) {
                    textFieldValue.selection
                } else {
                    androidx.compose.ui.text.TextRange(editorContent.length)
                }
            )
        }
    }

    val coercedSelectionStart = textFieldValue.selection.start.coerceIn(0, textFieldValue.text.length)
    val textBeforeCursor = textFieldValue.text.take(coercedSelectionStart)
    val currentLine = textBeforeCursor.count { it == '\n' } + 1
    val lastNewlineIndex = textBeforeCursor.lastIndexOf('\n')
    val currentColumn = coercedSelectionStart - lastNewlineIndex

    val themeConfig = when (editorTheme) {
        "Monokai" -> ThemeColors(
            editorBgColor = Color(0xFF272822),
            gutterBgColor = Color(0xFF1E1F1C),
            gutterTextColor = Color(0xFF75715E),
            editorTextColor = Color(0xFFF8F8F2),
            cursorColor = Color(0xFFF92672)
        )
        "Code Green" -> ThemeColors(
            editorBgColor = Color(0xFF000000),
            gutterBgColor = Color(0xFF0A0A0A),
            gutterTextColor = Color(0xFF00AA00),
            editorTextColor = Color(0xFF00FF00),
            cursorColor = Color(0xFF32CD32)
        )
        "Retro Amber" -> ThemeColors(
            editorBgColor = Color(0xFF1A1005),
            gutterBgColor = Color(0xFF130B03),
            gutterTextColor = Color(0xFF805000),
            editorTextColor = Color(0xFFFFB266),
            cursorColor = Color(0xFFFF9933)
        )
        "Nordic Frost" -> ThemeColors(
            editorBgColor = Color(0xFF2E3440),
            gutterBgColor = Color(0xFF242933),
            gutterTextColor = Color(0xFF4C566A),
            editorTextColor = Color(0xFFD8DEE9),
            cursorColor = Color(0xFF88C0D0)
        )
        "Solarized Light" -> ThemeColors(
            editorBgColor = Color(0xFFFDF6E3),
            gutterBgColor = Color(0xFFEEE8D5),
            gutterTextColor = Color(0xFF93A1A1),
            editorTextColor = Color(0xFF657B83),
            cursorColor = Color(0xFF268BD2)
        )
        "Github Light" -> ThemeColors(
            editorBgColor = Color(0xFFFFFFFF),
            gutterBgColor = Color(0xFFF6F8FA),
            gutterTextColor = Color(0xFF57606A),
            editorTextColor = Color(0xFF24292F),
            cursorColor = Color(0xFF0969DA)
        )
        else -> ThemeColors( // "Dracula Dark"
            editorBgColor = Color(0xFF282A36),
            gutterBgColor = Color(0xFF21222C),
            gutterTextColor = Color(0xFF6272A4),
            editorTextColor = Color(0xFFF8F8F2),
            cursorColor = Color(0xFFFF79C6)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Workspace Status / Actions Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp),
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                        )
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(
                onClick = onToggleDrawer,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .testTag("workspace_drawer_toggle")
            ) {
                Icon(
                    imageVector = if (isDrawerOpen) Icons.Default.MenuOpen else Icons.Default.Menu,
                    contentDescription = "Project Workspace Explorer",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Terminal,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "CEZIX IDE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 0.5.sp
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto Format Button
                IconButton(
                    onClick = {
                        viewModel.formatActiveFile()
                        Toast.makeText(context, "Code beautifully formatted!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .testTag("format_code_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatAlignLeft,
                        contentDescription = "Auto format editor code",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Git Version Control Button
                IconButton(
                    onClick = onGitVersionControlClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .testTag("git_vcs_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.MergeType,
                        contentDescription = "Open Git version control",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Termux Terminal Button
                IconButton(
                    onClick = onTermuxTerminalClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF102A12).copy(alpha = 0.9f))
                        .border(1.dp, Color(0xFF00FF00).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .testTag("launch_termux_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = "Launch terminal simulation",
                        tint = Color(0xFF00FF00),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Save active file content Button
                IconButton(
                    onClick = { 
                        viewModel.saveActiveFileContent()
                        Toast.makeText(context, "Draft changes saved to Workspace!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .testTag("save_draft_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Save file Draft",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // New entry file button
                IconButton(
                    onClick = onOpenNewFileCreator,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .testTag("create_new_file_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Append virtual file",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Decoder Wheel Sandbox Button
                IconButton(
                    onClick = showCipherSandboxClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .testTag("cipher_sandbox_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = "Cipher wheel engine sandbox",
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Dependency Library Packager Button
                IconButton(
                    onClick = onManageDependenciesClick,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .testTag("manage_dependencies_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Extension,
                        contentDescription = "Configure dependency modules",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Horizontal Open Tabs Bar (VS Code style switching tabs)
        if (openTabIds.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                openTabIds.forEach { id ->
                    val file = workspaceFiles.find { f -> f.id == id }
                    if (file != null) {
                        val isActive = file.id == selectedFileId
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .clickable { viewModel.openFileInTab(file) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = when (file.name.substringAfterLast(".", "")) {
                                    "html" -> Icons.Default.Html
                                    "css" -> Icons.Default.Css
                                    "js" -> Icons.Default.Javascript
                                    "md" -> Icons.Default.Description
                                    else -> Icons.Default.Article
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = file.name,
                                fontSize = 12.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close tab",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .clickable { viewModel.closeTab(file.id) },
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }

        // Active File Status banner
        if (activeSelectedFile != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Editing: ${activeSelectedFile.path}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    
                    // Display Badge indicating current loaded syntax language parsing rules
                    val activeLangBadge = currentRenderLanguage.ifEmpty { "txt" }.uppercase()
                    Surface(
                        modifier = Modifier.padding(start = 8.dp),
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Text(
                            text = activeLangBadge,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Lines: ${editorContent.count { char -> char == '\n' } + 1}",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "Size: ${editorContent.length} B",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    // Toggles settings customizer row
                    IconButton(
                        onClick = { showSettingsShelf = !showSettingsShelf },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Sprk Settings & Palette Theme customizer",
                            tint = if (showSettingsShelf) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Expanded Sprk Editor Setting & Themes Palette Customizer Shelf
        if (activeSelectedFile != null && showSettingsShelf) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Sprk Customization Theme Palette",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val availableThemes = listOf(
                            "Dracula Dark", "Monokai", "Code Green", 
                            "Retro Amber", "Nordic Frost", "Solarized Light", "Github Light"
                        )
                        availableThemes.forEach { t ->
                            val isSelected = (editorTheme == t)
                            FilterChip(
                                selected = isSelected,
                                onClick = { viewModel.editorTheme.value = t },
                                label = { Text(text = t, fontSize = 11.sp) }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Scale Typography step controller
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Font Scale: ", 
                                style = MaterialTheme.typography.bodySmall, 
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            FilledIconButton(
                                onClick = { viewModel.editorFontSize.value = (editorFontSize - 1).coerceAtLeast(10) },
                                modifier = Modifier.size(24.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", modifier = Modifier.size(10.dp))
                            }
                            Text(
                                text = "${editorFontSize}sp",
                                modifier = Modifier.padding(horizontal = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            FilledIconButton(
                                onClick = { viewModel.editorFontSize.value = (editorFontSize + 1).coerceIn(10, 24) },
                                modifier = Modifier.size(24.dp),
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", modifier = Modifier.size(10.dp))
                            }
                        }
                        
                        // Active Language Mode marker
                        Text(
                            text = "Mode: ${currentRenderLanguage.uppercase()}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Text(
                        text = "Syntax Engine Highlight Override",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val languageItems = listOf(
                            "Auto Detect" to null,
                            "Kotlin/Java" to "kt",
                            "Python" to "py",
                            "Go" to "go",
                            "Rust" to "rs",
                            "C++" to "cpp",
                            "SQL" to "sql",
                            "HTML/XML" to "html",
                            "CSS" to "css",
                            "JS/TS" to "js",
                            "Markdown" to "md"
                        )
                        languageItems.forEach { (displayName, associatedVal) ->
                            val isSelected = (manualLanguageOverride == associatedVal)
                            FilterChip(
                                selected = isSelected,
                                onClick = { manualLanguageOverride = associatedVal },
                                label = { Text(text = displayName, fontSize = 10.sp) }
                            )
                        }
                    }
                }
            }
        }

        // Central Code Typing Area with Line Numbers & Custom Highlighting
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(themeConfig.editorBgColor) // Dynamic theme background canvas
        ) {
            if (selectedFileId == null || activeSelectedFile == null) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CodeOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Files Active",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap the lateral folder menu or expand templates on side panel to begin typing.",
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onToggleDrawer) {
                        Text("Open Project File Hierarchy")
                    }
                }
            } else {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Leftside line number margin
                    val lineNumbersCount = textFieldValue.text.count { char -> char == '\n' } + 1
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(36.dp)
                            .background(themeConfig.gutterBgColor) // Dynamic line numbers column background
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        for (i in 1..lineNumbersCount) {
                            Text(
                                text = "$i",
                                fontSize = editorFontSize.sp,
                                fontFamily = FontFamily.Monospace,
                                color = themeConfig.gutterTextColor, // Dynamic theme-specific count colors
                                modifier = Modifier.padding(end = 8.dp)
                            )
                        }
                    }

                    // Seamless dynamic editing text area with syntax styling
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { s -> 
                            textFieldValue = s
                            if (s.text != editorContent) {
                                viewModel.editorContent.value = s.text
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState())
                            .testTag("code_editor_field"),
                        textStyle = TextStyle(
                            color = themeConfig.editorTextColor, // Dynamic theme base color
                            fontSize = editorFontSize.sp,
                            fontFamily = FontFamily.Monospace,
                            lineHeight = (editorFontSize + 4).sp
                        ),
                        cursorBrush = SolidColor(themeConfig.cursorColor), // Dynamic theme pulsing cursor focus bar
                        visualTransformation = CoreSyntaxHighlighter(currentRenderLanguage, editorTheme)
                    )
                }
            }
        }

        // --- Persistent Code Editor Status Bar ---
        androidx.compose.material3.Divider(color = Color(0xFF30363D), thickness = 1.dp)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF161B22))
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .testTag("editor_status_bar"),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left portion: Line, Column & active count
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatAlignLeft,
                        contentDescription = "Position indicator",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (selectedFileId != null) "Ln $currentLine, Col $currentColumn" else "Ln 1, Col 1",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    )
                }
                
                Text(
                    text = if (selectedFileId != null) "${textFieldValue.text.length} chars" else "0 chars",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            // Right portion: Syntax and Termux terminal connection indicator
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Syntax format label
                val activeSyntaxLabel = if (selectedFileId != null) {
                    currentRenderLanguage.ifEmpty { "TXT" }.uppercase()
                } else {
                    "PLAIN"
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = "Syntax",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = activeSyntaxLabel,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }

                Text(
                    text = "|",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )

                // High fidelity interactive segmented control for user-selectable theme toggle
                Row(
                    modifier = Modifier
                        .background(Color(0xFF0D1117), RoundedCornerShape(6.dp))
                        .border(1.dp, Color(0xFF30363D), RoundedCornerShape(6.dp))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val themeModes = listOf(
                        Triple("Light", "☀️", "Github Light"),
                        Triple("Dark", "🌙", "Dracula Dark"),
                        Triple("High", "⚡", "Code Green")
                    )
                    
                    themeModes.forEach { (mode, emoji, targetTheme) ->
                        val isSelected = when (mode) {
                            "Light" -> editorTheme == "Github Light" || editorTheme == "Solarized Light"
                            "High" -> editorTheme == "Code Green" || editorTheme == "Retro Amber"
                            else -> editorTheme != "Github Light" && editorTheme != "Solarized Light" && editorTheme != "Code Green" && editorTheme != "Retro Amber"
                        }
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color(0xFF21262D) else Color.Transparent)
                                .clickable {
                                    viewModel.editorTheme.value = targetTheme
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .testTag("theme_mode_" + mode.lowercase())
                        ) {
                            Text(
                                text = "$emoji $mode",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.Gray
                            )
                        }
                    }
                }

                Text(
                    text = "|",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                )

                // Live dynamic Termux terminal socket indicator with click modifier to launch dialog
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onTermuxTerminalClick() }
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    val statusDotColor = if (isTerminalActive) Color(0xFF22C55E) else Color(0xFFEF4444)
                    val statusLabel = if (isTerminalActive) "Connected" else "Standby"
                    
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .background(statusDotColor, CircleShape)
                    )
                    Text(
                        text = "Termux: $statusLabel",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = statusDotColor
                    )
                }
            }
        }

        // Specialized Speed Coding shortcuts panel (Insert helper icons)
        if (selectedFileId != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF0D1117))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // List of shortcut keys
                val speedKeys = listOf(
                    "{", "}", "[", "]", "(", ")", "<", ">", "/", ";", "=", "\"", "'", "!", "$", "#", "+", "-", "*", ".", "_"
                )
                speedKeys.forEach { key ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF21262D))
                            .border(BorderStroke(1.dp, Color(0xFF30363D)), RoundedCornerShape(8.dp))
                            .clickable {
                                // Paste key character at current point
                                viewModel.editorContent.value = viewModel.editorContent.value + key
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

// Side file tree drawer overlay list
@Composable
fun FilesSystemSidebar(
    workspaceFiles: List<WorkspaceFile>,
    selectedFileId: Int?,
    onSelectFile: (WorkspaceFile) -> Unit,
    onDeleteFile: (WorkspaceFile) -> Unit,
    onAddFileClick: () -> Unit,
    onCloseSidebar: () -> Unit,
    onManageDependenciesClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "WORKSPACE",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onAddFileClick) {
                    Icon(Icons.Default.CreateNewFolder, contentDescription = "Add node", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onManageDependenciesClick) {
                    Icon(Icons.Default.Extension, contentDescription = "Manage modular dependencies", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onCloseSidebar) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Collapse sidebar")
                }
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val nonFolders = workspaceFiles.filter { file -> !file.isFolder }
            items(nonFolders) { file ->
                val isActive = file.id == selectedFileId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .clickable { onSelectFile(file) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = when (file.name.substringAfterLast(".", "")) {
                                "html" -> Icons.Default.Html
                                "css" -> Icons.Default.Css
                                "js" -> Icons.Default.Javascript
                                "md" -> Icons.Default.Description
                                else -> Icons.Default.Article
                            },
                            contentDescription = null,
                            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = file.name,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Keep standard files protected from instant accidents, but customizable custom files delete friendly
                    if (file.name != "index.html" && file.name != "style.css" && file.name != "script.js") {
                        IconButton(
                            onClick = { onDeleteFile(file) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Remove file",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Cezix local compiling is fully sandbox isolated and works entirely offline.",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp)
        )
    }
}

// ==========================================
// 2. LIVE VIEW PREVIEW BROWSER PREVIEW
// ==========================================
@Composable
fun LivePreviewTab(
    compositeHtml: String,
    consoleLogs: List<ConsoleLogEntry>,
    onSendConsoleLog: (String, String) -> Unit,
    onClearConsole: () -> Unit
) {
    var isConsoleExpanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // Preview control header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Language, contentDescription = "Active web preview", tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "VIRTUAL BROWSER FRAME",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
            }
            
            // Toggle debugging logbook bar
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isConsoleExpanded = !isConsoleExpanded },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isConsoleExpanded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isConsoleExpanded) "Hide Console (${consoleLogs.size})" else "Show Console (${consoleLogs.size})",
                        fontSize = 11.sp
                    )
                }
            }
        }

        // Render WebView Sandbox
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = android.webkit.WebViewClient()
                        
                        class VirtualConsoleBridge {
                            @android.webkit.JavascriptInterface
                            fun postMessage(jsonString: String) {
                                try {
                                    val obj = org.json.JSONObject(jsonString)
                                    val logType = obj.optString("type", "log")
                                    val logMsg = obj.optString("message", "")
                                    post {
                                        onSendConsoleLog(logType, logMsg)
                                    }
                                } catch (e: Exception) {
                                    // log error
                                }
                            }
                        }
                        addJavascriptInterface(VirtualConsoleBridge(), "CezixConsoleBridge")
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://ceezix.internal/", compositeHtml, "text/html", "UTF-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Real-Time Debug Console expansion bottom sheet panel
        AnimatedVisibility(
            visible = isConsoleExpanded,
            modifier = Modifier.fillMaxWidth().height(260.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF07090E))
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            ) {
                // Console title toolbar bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF11141E))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "💻 CONSOLE COMPILER DRAW",
                        color = Color.Green,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(onClick = onClearConsole, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Block, contentDescription = "Clear logs", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        }
                        IconButton(onClick = { isConsoleExpanded = false }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss console", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        }
                    }
                }

                // Logs list details
                if (consoleLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Console is empty. Unhandled exceptions or logs will print here.",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(consoleLogs) { log ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = when (log.type) {
                                            "error" -> Color(0xFFFF5252).copy(alpha = 0.3f)
                                            "warn" -> Color(0xFFFFC107).copy(alpha = 0.3f)
                                            else -> Color.Transparent
                                        },
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .background(
                                        when (log.type) {
                                            "error" -> Color(0xFFFF5252).copy(alpha = 0.05f)
                                            "warn" -> Color(0xFFFFC107).copy(alpha = 0.05f)
                                            else -> Color.Transparent
                                        }
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = when (log.type) {
                                        "error" -> "❌ [ERR]"
                                        "warn" -> "⚠️ [WRN]"
                                        "info" -> "ℹ️ [INF]"
                                        else -> "💬 [LOG]"
                                    },
                                    color = when (log.type) {
                                        "error" -> Color(0xFFFF5252)
                                        "warn" -> Color(0xFFFFC107)
                                        "info" -> Color(0xFF66FDFF)
                                        else -> Color.LightGray
                                    },
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(end = 6.dp)
                                )
                                Text(
                                    text = log.message,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. CODEBOOK LOGS & ACCUMULATOR TAB
// ==========================================
@Composable
fun CodebookSavedTab(
    savedCodes: List<String>,
    userXp: Int,
    totalSolves: Int,
    currentStreak: Int,
    onDeleteCode: (String) -> Unit,
    onQuickReset: () -> Unit,
    showCipherSandboxClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Banner Header Dashboard
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "DECODER ACADEMY STANDINGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "$userXp XP",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Level Profile Progression",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                            Text("$totalSolves Solves", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = Color(0xFFFF8A00), modifier = Modifier.size(24.dp))
                            Text("$currentStreak Days", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        }

        // Cipher Quick Decoder Trigger Shortcut
        Button(
            onClick = showCipherSandboxClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.SyncAlt, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Launch Cryptographic Signal wheel", fontWeight = FontWeight.Bold)
        }

        // List Header or Reset Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "SAVED INTERCEPT LOGS (${savedCodes.size})",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
                fontFamily = FontFamily.Monospace
            )
            
            TextButton(onClick = onQuickReset) {
                Text("Reset Account Progression", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (savedCodes.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Book,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Your logbook is clear",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Decode mock cipher signs inside Decoder Sandbox Wheel and persist results long-term.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                savedCodes.forEach { entry ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val segments = entry.split("|:|:")
                                val badgeText = if (segments.size >= 3) "Cipher Shift: ${segments[0]}" else "Code Fragment"
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(MaterialTheme.colorScheme.secondaryContainer)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                
                                IconButton(
                                    onClick = { onDeleteCode(entry) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Remove logs snippet",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val segments = entry.split("|:|:")
                            if (segments.size >= 3) {
                                val rawSignal = segments[1]
                                val decSignal = segments[2]
                                
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "SIGNAL: $rawSignal",
                                        fontSize = 12.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = "DECODED: $decSignal",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            } else {
                                Text(
                                    text = entry,
                                    fontSize = 13.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 4. GEMINI POWERED INTELLIGENT AI COPILOT
// ==========================================
@Composable
fun AiCopilotTab(
    viewModel: CezixViewModel,
    aiOutput: String?,
    isAiLoading: Boolean,
    activeSelectedFile: WorkspaceFile?,
    aiCustomPromptInput: String,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit
) {
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Context identifier header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = "ACTIVE PAIRING TARGET",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (activeSelectedFile != null) "'${activeSelectedFile.name}' Workspace file focus is on" else "No file selected. AI will advise globally.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Assistant discussion scrolling canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color(0xFF0F141C), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            if (isAiLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Cezix Assistant is querying logic patterns...", color = Color.LightGray, fontSize = 12.sp)
                }
            } else if (aiOutput == null) {
                Column(
                    modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("I am your integrated pairing compiler.", color = Color.LightGray, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Tap standard action cards below, or input tailored coding assistance prompts.", fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Quick Action Template Cards
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Card(
                            modifier = Modifier.weight(1f).clickable { viewModel.askAiAboutCurrentCode("Describe what this file does, step-by-step.") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Describe\nFocus Code", modifier = Modifier.padding(10.dp), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                        Card(
                            modifier = Modifier.weight(1f).clickable { viewModel.askAiAboutCurrentCode("Refactor this code to make it more optimized, clean, and follow the best modern practices.") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Refactor &\nOptimize", modifier = Modifier.padding(10.dp), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                        Card(
                            modifier = Modifier.weight(1f).clickable { viewModel.askAiAboutCurrentCode("Look closely at this file and find if there are any bugs, unclosed tags, syntax gaps or logic flaws.") },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Text("Audit Potential\nFile Errors", modifier = Modifier.padding(10.dp), fontSize = 11.sp, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Chat Output scrollable block
                    Box(modifier = Modifier.weight(1f).verticalScroll(scrollState)) {
                        Text(
                            text = aiOutput,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }

                    // Direct Code Merge injection trigger - super cool!
                    if (aiOutput.contains("```")) {
                        Button(
                            onClick = { 
                                viewModel.mergeAiProposedCode(aiOutput) 
                                Toast.makeText(context, "Suggested block combined into file draft!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Apply Proposed Code Merge", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Prompt user interactive messaging bar
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = aiCustomPromptInput,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        text = "Ask Copilot: e.g. 'Optimize code context'...",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Copilot spark icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (aiCustomPromptInput.isNotEmpty()) {
                        IconButton(onClick = { onPromptChange("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear input prompt",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                ),
                modifier = Modifier.weight(1f).testTag("ai_interactive_prompt_field")
            )
            IconButton(
                onClick = onSendPrompt,
                enabled = aiCustomPromptInput.isNotBlank() && !isAiLoading,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (aiCustomPromptInput.isNotBlank() && !isAiLoading) MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Query Copilot",
                    tint = if (aiCustomPromptInput.isNotBlank() && !isAiLoading) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

// ==========================================
// 5. BACKWARDS COMPATIBLE CIPHER WHEEL SANDBOX
// ==========================================
@Composable
fun CipherSandboxDialog(
    viewModel: CezixViewModel,
    onDismiss: () -> Unit
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val currentShift by viewModel.currentShift.collectAsState()
    val vigenereKey by viewModel.vigenereKey.collectAsState()
    val wheelTheme by viewModel.wheelTheme.collectAsState()
    val context = LocalContext.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Decoder Wheel Sandbox", fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary) },
        text = {
            Column(
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .focusable()
                    .onPreviewKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown) {
                            if ((keyEvent.isCtrlPressed || keyEvent.isAltPressed) && keyEvent.key == Key.R) {
                                viewModel.currentShift.value = 0
                                Toast.makeText(context, "Wheel reset via shortcut!", Toast.LENGTH_SHORT).show()
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    }
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "This backward-compatible cryptographic simulator allows you to shift letters. Perfect for logs!",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                // Wheel Composable representation
                Box(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CipherWheel(
                        currentShift = currentShift,
                        onShiftChanged = { viewModel.currentShift.value = it },
                        themeName = wheelTheme
                    )
                }

                // Dedicated Reset Trigger button next to helper label
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Shortcut: [Ctrl + R] or [Alt + R] to Reset",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    
                    OutlinedButton(
                        onClick = { 
                            viewModel.currentShift.value = 0
                            Toast.makeText(context, "Wheel reset!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.testTag("reset_wheel_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset shift to 0",
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Wheel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Aesthetic Theme Selectors for Decoder Wheel with Visual Preview Grid
                Text(
                    text = "Aesthetic Wheel Theme (Visual Preview)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    modifier = Modifier.padding(top = 6.dp)
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val availableThemes = listOf("Cyber Neon", "Ubuntu", "VS Code", "GitHub", "Minimalist", "Antique")
                    availableThemes.chunked(2).forEach { themePair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            themePair.forEach { themeName ->
                                val isSelected = wheelTheme == themeName
                                
                                // Resolve properties for visual preview swatches and drawings
                                val outerColor = when (themeName) {
                                    "Ubuntu" -> Color(0xFFDF4814)
                                    "VS Code" -> Color(0xFF007ACC)
                                    "GitHub" -> Color(0xFF0969DA)
                                    "Minimalist" -> Color(0xFF000000)
                                    "Antique" -> Color(0xFFCBA374)
                                    else -> Color(0xFF45F3FF) // Cyber Neon
                                }
                                val innerColor = when (themeName) {
                                    "Ubuntu" -> Color(0xFFF99157)
                                    "VS Code" -> Color(0xFF4EC9B0)
                                    "GitHub" -> Color(0xFF2DA44E)
                                    "Minimalist" -> Color(0xFF555555)
                                    "Antique" -> Color(0xFF82684B)
                                    else -> Color(0xFF66FCF1) // Cyber Neon
                                }
                                val accentColor = when (themeName) {
                                    "Ubuntu" -> Color(0xFFE95420)
                                    "VS Code" -> Color(0xFFC586C0)
                                    "GitHub" -> Color(0xFF8250DF)
                                    "Minimalist" -> Color(0xFF000000)
                                    "Antique" -> Color(0xFFA67744)
                                    else -> Color(0xFFBD5EFF) // Cyber Neon
                                }
                                val textColor = when (themeName) {
                                    "GitHub", "Minimalist" -> Color(0xFF24292F)
                                    "Antique" -> Color(0xFF82684B)
                                    else -> Color(0xFFFFFFFF)
                                }
                                val bgColor = when (themeName) {
                                    "Cyber Neon" -> Color(0xFF080B15)
                                    "Ubuntu" -> Color(0xFF1C0012)
                                    "VS Code" -> Color(0xFF1E1E1E)
                                    "GitHub" -> Color(0xFFF6F8FA)
                                    "Minimalist" -> Color(0xFFFFFFFF)
                                    "Antique" -> Color(0xFFFBF0D9)
                                    else -> MaterialTheme.colorScheme.surface
                                }
                                val themeFontLabel = when (themeName) {
                                    "Antique" -> "Serif"
                                    "GitHub", "Minimalist" -> "Sans"
                                    else -> "Mono"
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.wheelTheme.value = themeName }
                                        .testTag("theme_preview_card_${themeName.lowercase().replace(" ", "_")}"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) outerColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = bgColor
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = themeName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                fontFamily = when (themeName) {
                                                    "Antique" -> FontFamily.Serif
                                                    "GitHub", "Minimalist" -> FontFamily.SansSerif
                                                    else -> FontFamily.Monospace
                                                }
                                            )
                                            if (isSelected) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(accentColor)
                                                )
                                            }
                                        }

                                        // Mini circular preview drawing vector canvas representational concentric rings
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            androidx.compose.foundation.Canvas(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                            ) {
                                                val centerOffset = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
                                                val outerR = size.width * 0.45f
                                                val midR = size.width * 0.30f
                                                val innerR = size.width * 0.16f

                                                // Draw outer ring
                                                drawCircle(
                                                    color = outerColor,
                                                    radius = outerR,
                                                    center = centerOffset,
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx())
                                                )

                                                // Draw inner ring
                                                drawCircle(
                                                    color = innerColor,
                                                    radius = midR,
                                                    center = centerOffset,
                                                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 0.8.dp.toPx())
                                                )

                                                // Draw core dot
                                                drawCircle(
                                                    color = accentColor,
                                                    radius = innerR,
                                                    center = centerOffset,
                                                    style = androidx.compose.ui.graphics.drawscope.Fill
                                                )
                                            }

                                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                                Text(
                                                    text = themeFontLabel,
                                                    fontSize = 8.sp,
                                                    color = textColor.copy(alpha = 0.65f),
                                                    fontFamily = FontFamily.Monospace
                                                )
                                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                                    listOf(outerColor, innerColor, accentColor).forEach { c ->
                                                        Box(
                                                            modifier = Modifier
                                                                .size(6.dp)
                                                                .clip(CircleShape)
                                                                .background(c)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Input Box
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { viewModel.inputText.value = it },
                    label = { Text("Interception Ciphertext") },
                    placeholder = { Text("e.g. Uifsf jt b tfdsfu nfttbhf", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock indicator icon",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    },
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.inputText.value = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear ciphertext input",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.secondary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Select standard shifts
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Caesar", "ROT13", "Atbash", "Vigenere").forEach { mode ->
                        FilterChip(
                            selected = currentMode == mode,
                            onClick = { viewModel.currentMode.value = mode },
                            label = { Text(mode) }
                        )
                    }
                }

                if (currentMode == "Caesar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Shift Index: $currentShift", modifier = Modifier.weight(1.5f), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Slider(
                            value = currentShift.toFloat(),
                            onValueChange = { viewModel.currentShift.value = it.toInt() },
                            valueRange = 0f..25f,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.secondary,
                                activeTrackColor = MaterialTheme.colorScheme.secondary
                            ),
                            modifier = Modifier.weight(3.5f)
                        )
                    }
                } else if (currentMode == "Vigenere") {
                    OutlinedTextField(
                        value = vigenereKey,
                        onValueChange = { viewModel.vigenereKey.value = it },
                        label = { Text("Code Keyword") },
                        placeholder = { Text("e.g. SECRETSIGNAL", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = "Security key icon",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        },
                        trailingIcon = {
                            if (vigenereKey.isNotEmpty()) {
                                IconButton(onClick = { viewModel.vigenereKey.value = "" }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear keyword input",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.tertiary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Divider()

                // Decipher outcome results
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("INTERCEPT DECODED:", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = viewModel.getProcessedOutput().ifEmpty { "Waiting for transmission..." },
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.getProcessedOutput().isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                shape = RoundedCornerShape(12.dp),
                enabled = inputText.isNotEmpty(),
                onClick = {
                    viewModel.saveCurrentToNotebook()
                    Toast.makeText(context, "Added translation to logbook!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Logs", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("Close", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
        }
    )
}

// ==========================================================
// 📦 DEPENDENCY MODULES & BEAUTIFUL INTEGRATION TEMPLATES
// ==========================================================
data class DependencyItem(
    val name: String,
    val language: String, // "node" or "python"
    val version: String,
    val category: String,
    val description: String,
    val templateCode: String
)

val dependencyLibraryList = listOf(
    DependencyItem(
        name = "lodash",
        language = "node",
        version = "^4.17.21",
        category = "Utility Engine",
        description = "A modern JavaScript utility library delivering modularity, performance & extras.",
        templateCode = """// Grouping files by language
const _ = require('lodash');

const items = [
  { name: 'app.py', size: 120, lang: 'python' },
  { name: 'index.html', size: 340, lang: 'html' },
  { name: 'package.json', size: 250, lang: 'json' }
];

const grouped = _.groupBy(items, 'lang');
console.log("Lodash Grouped Output:", JSON.stringify(grouped, null, 2));"""
    ),
    DependencyItem(
        name = "axios",
        language = "node",
        version = "^1.6.0",
        category = "HTTP Client",
        description = "Promise based HTTP client for the browser and node.js to run REST calls.",
        templateCode = """// Axios HTTP client service call
const axios = require('axios');

console.log("Fetching live payload from dummy endpoint...");
axios.get('https://httpbin.org/get?ref=ceezix')
  .then(response => {
    console.log("Status Code: " + response.status);
    console.log("Response data headers: " + JSON.stringify(response.data.headers, null, 2));
  })
  .catch(err => {
    console.error("HTTP Error:", err.message);
  });"""
    ),
    DependencyItem(
        name = "uuid",
        language = "node",
        version = "^9.0.1",
        category = "Cryptography",
        description = "Rigorous, RFC4122 compliant UUID generation for session authentication keys.",
        templateCode = """// RFC4122 UUID generator block
const { v4: uuidv4 } = require('uuid');

const primarySessionId = uuidv4();
console.log("Generated fresh Cryptographic session identifier:");
console.log("UUID-v4: " + primarySessionId);"""
    ),
    DependencyItem(
        name = "moment",
        language = "node",
        version = "^2.29.4",
        category = "Data Formatting",
        description = "Parse, validate, manipulate, and display date/time calculations in JavaScript.",
        templateCode = """// Moment JS Dates & Calendars manipulation
const moment = require('moment');

console.log("--- MOMENT CALENDAR CALCULATOR ---");
console.log("Current system local time: " + moment().format('MMMM Do YYYY, h:mm:ss a'));
console.log("Time relative to 7 days ago: " + moment().subtract(7, 'days').calendar());
console.log("Time relative to 2 weeks from now: " + moment().add(2, 'weeks').fromNow());"""
    ),
    DependencyItem(
        name = "express",
        language = "node",
        version = "^4.18.2",
        category = "Web Servers",
        description = "Fast, unopinionated, minimalist web framework structure for Node.js backends.",
        templateCode = """// Express web server boiler-frame
const express = require('express');
const app = express();
const PORT = 3000;

app.get('/', (req, res) => {
  res.json({
    status: "Online",
    service: "Ceezix Sandbox API Module"
  });
});

console.log("Express virtual backend configured on port " + PORT);"""
    ),

    DependencyItem(
        name = "requests",
        language = "python",
        version = "==2.31.0",
        category = "HTTP Network",
        description = "Elegant and simple HTTP library for Python, built for human beings.",
        templateCode = """# Requests client integration
import requests

print("Initiating dynamic REST exchange...")
url = 'https://httpbin.org/get?source=ceezix_python'
res = requests.get(url)
if res.status_code == 200:
    print("Connection healthy! Metadata headers parsed:")
    print(res.json().get('headers'))"""
    ),
    DependencyItem(
        name = "numpy",
        language = "python",
        version = "==1.26.2",
        category = "Scientific Math",
        description = "The fundamental package for high-speed scientific computing with Python.",
        templateCode = """# Numpy Mathematical Array Operations
import numpy as np

print("Creating multi-dimensional vector array...")
matrix = np.array([[1, 2, 3], [4, 5, 6]])
print(f"Matrix rank count: {matrix.ndim}")
print(f"Matrix shape specs: {matrix.shape}")
print("Calculated Mean of columns:", np.mean(matrix, axis=0))"""
    ),
    DependencyItem(
        name = "pandas",
        language = "python",
        version = "==2.1.3",
        category = "Data Analysis",
        description = "Powerful data structures for parsing, time series, and analytics.",
        templateCode = """# Pandas Dataframe & Series analysis
import pandas as pd

raw_series = {
    'Module': ['Requests', 'Numpy', 'Pandas', 'Flask'],
    'Daily Downloads': [4500, 3100, 2400, 1920],
    'Category': ['Network', 'Math', 'Data', 'Web']
}

df = pd.DataFrame(raw_series)
print("--- MODULE METRICS ---")
print(df.to_string(index=False))
print("\nFilter Category: Math")
print(df[df['Category'] == 'Math'])"""
    ),
    DependencyItem(
        name = "flask",
        language = "python",
        version = "==3.0.0",
        category = "Micro-Services",
        description = "A lightweight WSGI web dynamic micro-framework in Python.",
        templateCode = """# Flask RESTful API server structure
from flask import Flask, jsonify
app = Flask(__name__)

@app.route('/api/status', methods=['GET'])
def status_endpoint():
    return jsonify({
        "engine": "Ceezix Sandbox Sandbox Server",
        "status": "Online",
        "python_libs": ["requests", "pandas", "numpy"]
    })

app.run(host='0.0.0.0', port=5000)"""
    ),
    DependencyItem(
        name = "matplotlib",
        language = "python",
        version = "==3.8.2",
        category = "Visual Charts",
        description = "Comprehensive library for creating beautiful graphic and chart diagrams.",
        templateCode = """# Matplotlib scientific display layout
import matplotlib.pyplot as plt

categories = ['Lodash', 'Axios', 'Requests', 'Numpy', 'Pandas']
usage_shares = [35, 25, 20, 12, 8]

plt.figure(figsize=(6, 4))
plt.bar(categories, usage_shares, color=['#45f3ff', '#66fcf1', '#1f2833', '#116329', '#cb4b16'])
plt.title('Ceezix Dependency Adoption distribution')
plt.ylabel('Share percentage')
plt.savefig('dependency_diagram.png')
print("Saved beautiful performance visual rendering as dependency_diagram.png")"""
    )
)

@Composable
fun DependencyManagerDialog(
    viewModel: CezixViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var activeLangTab by remember { mutableStateOf("node") } // "node" or "python"
    var searchText by remember { mutableStateOf("") }
    var expandedItemName by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Extension,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text("Ceezix Dependency Center", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Configure modular packages and inject beautiful instructions templates into your workspace dynamically.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (activeLangTab == "node" || activeLangTab == "python") {
                    // Search textfield
                    OutlinedTextField(
                        value = searchText,
                        onValueChange = { searchText = it },
                        placeholder = { Text("Search packages...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // List of filtered elements
                    val filteredList = dependencyLibraryList.filter {
                        it.language == activeLangTab &&
                        (it.name.contains(searchText, ignoreCase = true) || 
                         it.category.contains(searchText, ignoreCase = true) || 
                         it.description.contains(searchText, ignoreCase = true))
                    }

                    if (filteredList.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("No packages match your inquiry", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(filteredList) { item ->
                                val isExpanded = expandedItemName == item.name
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable { expandedItemName = if (isExpanded) null else item.name },
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                         else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    ),
                                    border = if (isExpanded) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)) else null
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(
                                                        text = item.name,
                                                        fontSize = 14.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = item.version,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                                    )
                                                }
                                                Text(
                                                    text = item.category,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    letterSpacing = 0.5.sp
                                                )
                                            }
                                            
                                            Icon(
                                                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = item.description,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 15.sp
                                        )

                                        if (isExpanded) {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            
                                            Text(
                                                text = "INTEGRATION TEMPLATE CODE:",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary,
                                                letterSpacing = 0.5.sp
                                            )
                                            
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            // Display code template with syntax highlighter
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFF1E1F1C)) // Dark terminal-style preview
                                                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(8.dp)
                                            ) {
                                                val highlighter = remember(item.language) {
                                                    CoreSyntaxHighlighter(
                                                        extension = if (item.language == "node") "js" else "py",
                                                        theme = "Monokai"
                                                    )
                                                }
                                                val transformed = highlighter.filter(androidx.compose.ui.text.AnnotatedString(item.templateCode))
                                                Text(
                                                    text = transformed.text,
                                                    fontSize = 10.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    lineHeight = 14.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            Button(
                                                onClick = {
                                                    viewModel.injectDependency(
                                                        language = item.language,
                                                        packageName = item.name,
                                                        version = item.version,
                                                        templateCode = item.templateCode
                                                    )
                                                    Toast.makeText(
                                                        context,
                                                        "Successfully configured and injected ${item.name}!",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                    onDismiss()
                                                },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Inject Dependency & Template", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (activeLangTab == "templates") {
                    // --- Gorgeous Project Templates Hub from Official Sources ---
                    var selectedTemplateName by remember { mutableStateOf<String?>(null) }
                    
                    val projectTemplates = listOf(
                        Triple(
                            "React CDN Sandbox",
                            "Official React 18 single-page app containing ReactDOM, Babel, Tailwind CSS and dynamic Caesar Encoder React component.",
                            listOf("index.html", "script.js", "style.css", "README.md")
                        ),
                        Triple(
                            "Static Website Portfolio",
                            "Clean HTML5 static landing page highlighting CSS margins, responsive CSS Grid layout, gradient styling, and an interactive Matrix launch button.",
                            listOf("index.html", "script.js", "style.css")
                        ),
                        Triple(
                            "Caesar Decoder Wheel",
                            "Concentric Rotary Shift Visualization with drag rotors, touch easing physics, dynamic character trail mapping, and themed aesthetics.",
                            listOf("index.html", "script.js", "style.css", "data.json", "README.md")
                        ),
                        Triple(
                            "Matrix Digital Rain",
                            "Classic green falling cascading codestream animation on HTML5 Canvas, featuring speed settings, color hues, and text cipher injectors.",
                            listOf("index.html", "script.js", "style.css", "data.json", "README.md")
                        ),
                        Triple(
                            "Vigenère Shifting Grid",
                            "Classic polyalphabetic cipher solver highlighting index coordinates, key-based shifting password loops, and trace logs formulas step calculations.",
                            listOf("index.html", "script.js", "style.css", "data.json", "README.md")
                        ),
                        Triple(
                            "Hex & Binary Console",
                            "Retro command terminal simulation displaying inputs mapped instantly to Raw, Binary, Hex, Base64 and ROT-13 configurations.",
                            listOf("index.html", "script.js", "style.css", "data.json", "README.md")
                        ),
                        Triple(
                            "NPM Module library",
                            "A structured NPM module template incorporating package.json specs, index.js module exports, a unit test runner suite, and custom build bundler tasks.",
                            listOf("package.json", "index.js", "test.js", "build.js")
                        ),
                        Triple(
                            "Vite & Webpack Configs",
                            "Official configurations for bundler systems, showcasing asset transpilations, entry points, code splits, and Webpack CSS rules loaders.",
                            listOf("vite.config.js", "webpack.config.js", "README.md")
                        ),
                        Triple(
                            "Express Backend Server",
                            "Backend Node server featuring Express router APIs, JSON parse support, in-memory DB syncing, and health status logging routes.",
                            listOf("server.js", "package.json", "README.md")
                        )
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(projectTemplates) { (name, desc, files) ->
                            val isSelected = selectedTemplateName == name
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedTemplateName = if (isSelected) null else name },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "OFFICIAL SOURCE TEMPLATE",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        lineHeight = 15.sp
                                    )
                                    
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Text(
                                            text = "FILES CREATED IN VIRTUAL WORKSPACE:",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            files.forEach { filename ->
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                                                    modifier = Modifier.padding(vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = filename,
                                                        fontSize = 9.sp,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontWeight = FontWeight.Medium,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                                    )
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = {
                                                viewModel.loadProjectTemplate(name)
                                                Toast.makeText(
                                                    context,
                                                    "Successfully configured and loaded '$name' in workspace! Run files in Compiler view.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onDismiss()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CreateNewFolder,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Initialize Project Template 🚀", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (activeLangTab == "react") {
                    // --- Gorgeous React Components gallery ---
                    var selectedReactCompName by remember { mutableStateOf<String?>(null) }
                    
                    val reactComponents = listOf(
                        Triple(
                            "Cyberpunk Caesar Wheel",
                            "Gorgeously styled Caesar encoder widget using pure state hook handlers, responsive input controls, neon decoration border accents, and snap-to-grid rotation dials.",
                            """// Cyberpunk Caesar Encoder - Stateful React Component
const { useState, useEffect } = React;

function CyberpunkCaesar() {
    const [plaintext, setPlaintext] = useState("SECURE INTEL");
    const [shift, setShift] = useState(5);
    const [cipher, setCipher] = useState("");

    useEffect(() => {
        let res = "";
        for (let i = 0; i < plaintext.length; i++) {
            let c = plaintext.charCodeAt(i);
            if (c >= 65 && c <= 90) res += String.fromCharCode(((c - 65 + shift) % 26) + 65);
            else if (c >= 97 && c <= 122) res += String.fromCharCode(((c - 97 + shift) % 26) + 97);
            else res += plaintext[i];
        }
        setCipher(res);
    }, [plaintext, shift]);

    return (
        <div className="max-w-md mx-auto p-6 bg-slate-900 border-2 border-emerald-500 rounded-2xl shadow-[0_0_20px_rgba(16,185,129,0.3)] mt-6 text-emerald-100 font-mono">
            <h2 className="text-xl font-black text-center text-emerald-400 mb-2 tracking-wider">⚡ CYBERPUNK ENCODER ⚡</h2>
            <p className="text-center text-[9px] text-slate-500 mb-6 uppercase">State-Sourced Shift Encryption sandbox</p>

            <div className="space-y-4 text-left">
                <div>
                    <label className="text-[10px] text-emerald-500 font-bold tracking-widest block mb-1">INPUT TRANS-PAYLOAD</label>
                    <input 
                        type="text" 
                        value={plaintext} 
                        onChange={e => setPlaintext(e.target.value)} 
                        className="w-full bg-slate-950 border border-emerald-500/30 rounded-xl p-3 text-sm font-mono text-emerald-300 focus:outline-none focus:border-emerald-400"
                    />
                </div>

                <div>
                    <div className="flex justify-between items-center mb-1">
                        <label className="text-[10px] text-emerald-500 font-bold tracking-widest">ROTATION OFFSET</label>
                        <span className="text-sm text-emerald-300 font-bold">{shift}</span>
                    </div>
                    <input 
                        type="range" min="0" max="25" 
                        value={shift} 
                        onChange={e => setShift(parseInt(e.target.value))} 
                        className="w-full h-1 bg-slate-950 rounded-lg appearance-none cursor-pointer accent-emerald-500"
                    />
                </div>

                <div className="bg-slate-950 p-4 rounded-xl border border-emerald-500/20 shadow-inner">
                    <span className="text-[10px] text-emerald-500 font-bold tracking-widest block mb-2">OUTPUT CIPHER CODE</span>
                    <p className="text-lg text-emerald-400 font-black tracking-widest break-all bg-emerald-950/10 p-2 rounded min-h-[48px]">{cipher || "..."}</p>
                </div>
            </div>
            
            <div className="mt-6 text-center">
                <button 
                    onClick={() => { setPlaintext("CEEZIX DECODER WORKSPACE"); setShift(13); }}
                    className="bg-emerald-500 hover:bg-emerald-600 active:scale-95 text-slate-950 font-bold px-4 py-2 rounded-xl text-xs tracking-wider uppercase transition-all"
                >
                    Hard Reset
                </button>
            </div>
        </div>
    );
}

const root = ReactDOM.createRoot(document.getElementById('react-root'));
root.render(<CyberpunkCaesar />);"""
                        ),
                        Triple(
                            "Mission Interval Cyber Timer",
                            "Stopwatch countdown loop widget with highly responsive play, pause, and interval clearing logic driven entirely by standard React hooks.",
                            """// Mission Interval Cyber Timer - React 18 sandbox
const { useState, useEffect } = React;

function CyberTimer() {
    const [seconds, setSeconds] = useState(60);
    const [isActive, setIsActive] = useState(false);

    useEffect(() => {
        let interval = null;
        if (isActive && seconds > 0) {
            interval = setInterval(() => {
                setSeconds(prev => prev - 1);
            }, 1000);
        } else if (seconds === 0) {
            setIsActive(false);
        }
        return () => clearInterval(interval);
    }, [isActive, seconds]);

    return (
        <div className="max-w-md mx-auto p-6 bg-slate-900 border-2 border-cyan-500 rounded-2xl shadow-[0_0_20px_rgba(6,182,212,0.3)] mt-6 text-center font-mono">
            <h2 className="text-xl font-black text-cyan-400 tracking-wider mb-1">⏱️ MISSION TIMER ⏱️</h2>
            <p className="text-slate-550 text-[9px] uppercase tracking-widest mb-6">React countdown timer module</p>

            <div className="text-5xl font-black text-cyan-300 drop-shadow-[0_0_10px_rgba(6,182,212,0.5)] my-6">
                00:{seconds < 10 ? '0' + seconds : seconds}
            </div>

            <div className="flex justify-center gap-4 mb-6">
                <button 
                    onClick={() => setIsActive(!isActive)}
                    className={`px-5 py-2 rounded-xl text-xs font-bold uppercase tracking-wider transition-all ` + 
                        (isActive ? 'bg-amber-500 text-slate-950 hover:bg-amber-600' : 'bg-cyan-500 text-slate-950 hover:bg-cyan-600')}
                >
                    {isActive ? "Pause Interval" : "Start Countdown"}
                </button>
                <button 
                    onClick={() => { setIsActive(false); setSeconds(60); }}
                    className="bg-slate-800 hover:bg-slate-700 text-slate-300 px-5 py-2 rounded-xl text-xs font-bold uppercase tracking-wider transition-all"
                >
                    Reset Timer
                </button>
            </div>

            <div className="text-left bg-slate-950 p-3 rounded-xl border border-cyan-500/10 text-[9px] text-cyan-400/80">
                INFO: Interval loops are dynamically registered, cleaned, and synced with local memory contexts instantly.
            </div>
        </div>
    );
}

const root = ReactDOM.createRoot(document.getElementById('react-root'));
root.render(<CyberTimer />);"""
                        ),
                        Triple(
                            "Quantum Metrics SVG Dashboard",
                            "Data grid visualizer rendering gorgeous responsive SVG bar charts, metric togglers, and layout transitions.",
                            """// Quantum Metrics SVG Dashboard - Stateful React Render
const { useState } = React;

function QuantumChart() {
    const [metricType, setMetricType] = useState("transmissions");
    const data = {
        transmissions: [45, 78, 56, 120, 89, 142],
        decodes: [30, 48, 70, 95, 110, 155],
        efficiency: [92, 94, 88, 96, 95, 99]
    };

    const maxValue = Math.max(...data[metricType]);

    return (
        <div className="max-w-md mx-auto p-6 bg-slate-900 border-2 border-pink-500 rounded-2xl shadow-[0_0_20px_rgba(244,63,94,0.3)] mt-6 font-mono text-center">
            <h2 className="text-xl font-black text-pink-400 tracking-wider mb-1">📊 QUANTUM METRICS 📊</h2>
            <p className="text-slate-550 text-[9px] uppercase tracking-widest mb-6 text-slate-550">Responsive React SVG Data Grid</p>

            <div className="flex justify-center gap-2 mb-6">
                {["transmissions", "decodes", "efficiency"].map(t => (
                    <button 
                        key={t}
                        onClick={() => setMetricType(t)}
                        className={`px-3 py-1.5 rounded-lg text-[9px] font-bold uppercase tracking-wider transition-all ` + 
                            (metricType === t ? 'bg-pink-500 text-slate-950' : 'bg-slate-800 text-slate-400 hover:text-white')}
                    >
                        {t}
                    </button>
                ))}
            </div>

            {/* SVG Interactive Chart */}
            <div className="bg-slate-950 p-4 rounded-xl border border-pink-500/10 mb-4">
                <div className="h-32 flex items-end justify-between gap-1 pt-4 px-2">
                    {data[metricType].map((val, idx) => {
                        const barHeight = (val / maxValue) * 100;
                        return (
                            <div key={idx} className="flex-1 flex flex-col items-center group">
                                <span className="text-[8px] font-mono text-pink-400 mb-1">{val}</span>
                                <div 
                                    style={{ height: barHeight + "px" }}
                                    className="w-full bg-gradient-to-t from-pink-600 to-pink-400 rounded-t-md shadow-[0_0_10px_rgba(244,63,94,0.3)] transition-all duration-300"
                                ></div>
                                <span className="text-[8px] font-mono text-slate-500 mt-2">M{idx+1}</span>
                            </div>
                        );
                    })}
                </div>
            </div>

            <div className="text-center text-[9px] text-slate-500">
                Switch parameters above to transition the fully synchronized vectors instantly.
            </div>
        </div>
    );
}

const root = ReactDOM.createRoot(document.getElementById('react-root'));
root.render(<QuantumChart />);"""
                        ),
                        Triple(
                            "Physical Wave Synthesizer Widget",
                            "Mathematical sine wave physics animation rendering frequency values dynamically depending on state scale slide selections.",
                            """// Interactive Physical Wave Synthesizer - React View
const { useState } = React;

function SoundSynthesizer() {
    const [frequency, setFrequency] = useState(440);
    const [waveType, setWaveType] = useState("sine");

    return (
        <div className="max-w-md mx-auto p-6 bg-slate-900 border-2 border-indigo-500 rounded-2xl shadow-[0_0_20px_rgba(99,102,241,0.3)] mt-6 font-mono text-center">
            <h2 className="text-xl font-black text-indigo-400 tracking-wider mb-1">🌊 QUANTUM WAVE 🌊</h2>
            <p className="text-slate-550 text-[9px] uppercase tracking-widest mb-6 text-slate-500">Interactive Physical Waveform Canvas</p>

            <div className="bg-slate-950 h-24 rounded-2xl border border-indigo-500/20 relative overflow-hidden mb-6 flex items-center justify-center">
                <svg className="w-full h-full absolute top-0 left-0" viewBox="0 0 100 40">
                    <path 
                        d={"M 0 20 Q 25 " + (20 - frequency/40) + ", 50 20 T 100 20"}
                        fill="none" 
                        stroke="#6366f1" 
                        strokeWidth="1.5"
                        strokeDasharray="4 2"
                    />
                    <path 
                        d={"M 0 20 Q 25 " + (20 + frequency/40) + ", 50 20 T 100 20"}
                        fill="none" 
                        stroke="#818cf8" 
                        strokeWidth="1"
                    />
                </svg>
                <span className="text-xs text-indigo-300 font-bold z-10 bg-slate-900/80 px-3 py-1 rounded-full border border-indigo-500/20">
                    WAVELENGTH: {frequency}Hz
                </span>
            </div>

            <div className="space-y-4 text-left">
                <div>
                    <label className="text-[10px] text-indigo-500 font-bold tracking-widest block mb-1">ADJUST SCALE FREQUENCY</label>
                    <input 
                        type="range" min="100" max="1000" 
                        value={frequency} 
                        onChange={e => setFrequency(parseInt(e.target.value))} 
                        className="w-full h-1 bg-slate-950 rounded-lg appearance-none cursor-pointer accent-indigo-500"
                    />
                </div>

                <div className="flex gap-2">
                    {["sine", "square", "triangle", "sawtooth"].map(type => (
                        <button 
                            key={type}
                            onClick={() => setWaveType(type)}
                            className={`flex-1 py-1.5 rounded-lg text-[9px] font-bold uppercase tracking-wider transition-all border ` + 
                                (waveType === type ? 'bg-indigo-500 text-slate-950 border-indigo-500' : 'bg-slate-800 text-slate-400 border-indigo-500/20 hover:text-white')}
                        >
                            {type}
                        </button>
                    ))}
                </div>
            </div>
        </div>
    );
}

const root = ReactDOM.createRoot(document.getElementById('react-root'));
root.render(<SoundSynthesizer />);"""
                        ),
                        Triple(
                            "Neural Matrix Terminal Console",
                            "Interactive terminal command parser logging custom command outcomes state-buffered inside the custom react framework.",
                            """// Neural Matrix Terminal Console Emulator
const { useState, useEffect, useRef } = React;

function MatrixConsole() {
    const [logs, setLogs] = useState([
        "SYSTEM: Starting system interface container...",
        "SYSTEM: Connection virtual Cezix Web preview server...",
        "SYSTEM: Enter 'help' to review instruction streams."
    ]);
    const [command, setCommand] = useState("");
    const bottomRef = useRef(null);

    useEffect(() => {
        bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
    }, [logs]);

    const handleSend = (e) => {
        e.preventDefault();
        if (!command.trim()) return;
        
        let reply = `USER: ` + command;
        let systemReply = "";
        
        const cmd = command.toLowerCase().trim();
        if (cmd === "help") {
            systemReply = "SYSTEM: Directives: 'help', 'status', 'decrypt', 'clear'";
        } else if (cmd === "status") {
            systemReply = "SYSTEM: Core Sandbox active. Framework stream: React 18.";
        } else if (cmd === "decrypt") {
            systemReply = "SYSTEM: Executing quantum code compilation... DECODED SUCCESSFULLY.";
        } else if (cmd === "clear") {
            setLogs([]);
            setCommand("");
            return;
        } else {
            systemReply = `SYSTEM: Process not matching core command: "` + command + `"`;
        }

        setLogs(prev => [...prev, reply, systemReply]);
        setCommand("");
    };

    return (
        <div className="max-w-md mx-auto p-6 bg-slate-900 border-2 border-green-500 rounded-2xl shadow-[0_0_20px_rgba(34,197,94,0.3)] mt-6 text-green-400 font-mono text-left">
            <h2 className="text-lg font-black text-center text-green-300 mb-1">🤖 NEURAL TERMINAL 🤖</h2>
            <p className="text-slate-550 text-[8px] text-center mb-4 uppercase">React in-browser emulator console</p>

            <div className="bg-slate-950 p-4 rounded-xl border border-green-550/20 h-40 overflow-y-auto space-y-1 text-[11px] mb-4">
                {logs.map((log, idx) => (
                    <div key={idx} className={log.startsWith("USER:") ? "text-cyan-400" : "text-green-400"}>
                        {log}
                    </div>
                ))}
                <div ref={bottomRef} />
            </div>

            <form onSubmit={handleSend} className="flex gap-2">
                <span className="text-green-500 self-center">&gt;</span>
                <input 
                    type="text" 
                    value={command} 
                    onChange={e => setCommand(e.target.value)} 
                    placeholder="Type (status, decrypt, help)..."
                    className="flex-1 bg-slate-950 border border-green-500/30 rounded-lg p-2 text-xs text-green-300 focus:outline-none focus:border-green-400"
                />
                <button type="submit" className="bg-green-550 hover:bg-green-600 text-slate-950 px-3 py-1.5 rounded-lg text-xs font-bold uppercase transition-all">
                    SEND
                </button>
            </form>
        </div>
    );
}

const root = ReactDOM.createRoot(document.getElementById('react-root'));
root.render(<MatrixConsole />);"""
                        )
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(reactComponents) { (name, desc, code) ->
                            val isSelected = selectedReactCompName == name
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { selectedReactCompName = if (isSelected) null else name },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                ),
                                border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)) else null
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                text = name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = "REACT 18 INTEGRATION",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.8f),
                                                letterSpacing = 0.5.sp
                                            )
                                        }
                                        Icon(
                                            imageVector = if (isSelected) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = desc,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                                        lineHeight = 15.sp
                                    )
                                    
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        androidx.compose.material3.Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                        Spacer(modifier = Modifier.height(10.dp))
                                        
                                        Button(
                                            onClick = {
                                                viewModel.injectReactComponent(name, code)
                                                Toast.makeText(
                                                    context,
                                                    "Successfully injected $name into script.js! Open Live Preview to run.",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onDismiss()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Extension,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Inject React Component ⚛️", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // --- Gorgeous Custom Package Injection Form ---
                    var customType by remember { mutableStateOf("node") }
                    var customName by remember { mutableStateOf("") }
                    var customVersion by remember { mutableStateOf("") }
                    var customTemplate by remember { mutableStateOf("") }
                    
                    var lastCustomName by remember { mutableStateOf("") }
                    var lastCustomType by remember { mutableStateOf("") }
                    
                    LaunchedEffect(customName, customType) {
                        if (customName != lastCustomName || customType != lastCustomType) {
                            if (customType == "node") {
                                customVersion = "^1.0.0"
                                customTemplate = "const ${if (customName.isEmpty()) "customModule" else customName} = require('${if (customName.isEmpty()) "custom-package" else customName}');\n\nconsole.log('Successfully called virtual custom package: ${if (customName.isEmpty()) "custom-package" else customName}');"
                            } else {
                                customVersion = "==1.0.0"
                                customTemplate = "import ${if (customName.isEmpty()) "custom_module" else customName}\n\nprint('Successfully called virtual custom package: ${if (customName.isEmpty()) "custom-package" else customName}')"
                            }
                            lastCustomName = customName
                            lastCustomType = customType
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "TARGET COMPILER FRAMEWORK",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            letterSpacing = 0.5.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = customType == "node",
                                onClick = { customType = "node" },
                                label = { Text("Node.js (package.json)") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = customType == "python",
                                onClick = { customType = "python" },
                                label = { Text("Python (requirements.txt)") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        OutlinedTextField(
                            value = customName,
                            onValueChange = { customName = it },
                            label = { Text("Package ID Name") },
                            placeholder = { Text("e.g. chalk, scipy, socket.io") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customVersion,
                            onValueChange = { customVersion = it },
                            label = { Text("Version Identifier constraint") },
                            placeholder = { Text("e.g. ^5.0.0, ==3.4.1, latest") },
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = customTemplate,
                            onValueChange = { customTemplate = it },
                            label = { Text("Integration Usage Script Snippet") },
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 200.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (customName.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter a valid package identifier", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.injectDependency(
                                        language = customType,
                                        packageName = customName.trim(),
                                        version = customVersion.trim(),
                                        templateCode = customTemplate
                                    )
                                    Toast.makeText(
                                        context,
                                        "Successfully injected custom module ${customName.trim()}!",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    onDismiss()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inject Custom Dependency Configuration", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }

                // Dynamic Platforms Button Cards on the bottom of the Dialog
                Spacer(modifier = Modifier.height(4.dp))
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "SELECT TARGET DEPLOYMENT PLATFORM",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.85f),
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val platforms = listOf(
                        Triple("templates", "Templates", Icons.Default.AutoAwesome),
                        Triple("react", "React Components", Icons.Default.Widgets),
                        Triple("node", "Node.js (NPM)", Icons.Default.LibraryAdd),
                        Triple("python", "Python (PIP)", Icons.Default.Description),
                        Triple("custom", "Custom Script", Icons.Default.Add)
                    )

                    platforms.forEach { (id, label, icon) ->
                        val isSelected = activeLangTab == id
                        Card(
                            modifier = Modifier
                                .width(140.dp)
                                .height(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable {
                                    activeLangTab = id
                                    expandedItemName = null
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                                 else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(verticalArrangement = Arrangement.Center) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = id.uppercase(),
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        letterSpacing = 0.4.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TermuxTerminalDialog(
    viewModel: CezixViewModel,
    onDismiss: () -> Unit
) {
    var history by remember {
        mutableStateOf(
            listOf(
                "Welcome to Termux Android Terminal Emulator (v0.122-ceezix)",
                "Type 'help' to identify available compiled binary packages and utilities.",
                "Target host directory: /data/data/com.termux/files/ceezix_workspace",
                ""
            )
        )
    }

    var currentInput by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Auto scroll terminal to the bottom when history changes
    LaunchedEffect(history.size) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    fun executeTerminalCommand(cmdText: String) {
        if (cmdText.isBlank()) return
        val trimmedCmd = cmdText.trim()
        val parts = trimmedCmd.split(" ")
        val command = parts[0].lowercase()

        val newOutputs = mutableListOf<String>()
        newOutputs.add("$ $trimmedCmd")

        when (command) {
            "help" -> {
                newOutputs.add("Standard interactive terminal utilities:")
                newOutputs.add("  help                       Show this help assistance index")
                newOutputs.add("  ls                         List workspace active room files")
                newOutputs.add("  cat <file>                 Print exact text of a workspace file")
                newOutputs.add("  node <file> | python <file> Compile/Run a local Javascript/Python script")
                newOutputs.add("  cipher Caesar <shift> <s>  Check Caesar cipher (e.g. cipher Caesar 3 hello)")
                newOutputs.add("  cipher Vigenere <key> <s>  Check Vigenere cipher (e.g. cipher Vigenere key msg)")
                newOutputs.add("  cipher Base64 encode <s>   Base64 encode a string")
                newOutputs.add("  cipher Base64 decode <s>   Base64 decode a string")
                newOutputs.add("  neofetch                   Print beautiful retro Termux system specs")
                newOutputs.add("  matrix                     Launch live cyberpunk cascade streams")
                newOutputs.add("  clear                      Clear terminal console buffer screen")
                newOutputs.add("  whoami                     Display current device credential profile")
                newOutputs.add("  date                       Display current clock timezone time")
                newOutputs.add("  pwd                        Display Termux workspace absolute path")
            }
            "clear" -> {
                history = emptyList()
                currentInput = ""
                return
            }
            "ls" -> {
                val files = viewModel.workspaceFiles.value
                if (files.isEmpty()) {
                    newOutputs.add("Directory is empty. No files configured in the Ceezix room.")
                } else {
                    newOutputs.add("Listing workspace files:")
                    files.forEach { file ->
                        val sizeString = "${file.content.length} B"
                        newOutputs.add("  -rw-r--r--   ceezix_dev   $sizeString   ${file.name}")
                    }
                }
            }
            "neofetch" -> {
                newOutputs.add("       .---.       ceezix_dev@termux-ceezix")
                newOutputs.add("      /     \\      ------------------------")
                newOutputs.add("      \\_.._/       OS: Android OS - Ceezix Console Edition")
                newOutputs.add("      |    |       Host: Google AI Studio Engine Platform")
                newOutputs.add("    __|_.._|__     Kernel: ARMv8-A Terminal Environment")
                newOutputs.add("   /          \\    UpTime: 2 hours 45 mins")
                newOutputs.add("  |  ( )  ( )  |   Shell: bash / termux-sh v5.2")
                newOutputs.add("  |    _.._    |   Packages: 1045 (pkg-manager)")
                newOutputs.add("   \\_        _/    Active Theme: ${viewModel.editorTheme.value}")
                newOutputs.add("     '------'      Crypto Engine: Atbash, Caesar, Vigenere, Base64")
            }
            "matrix" -> {
                newOutputs.add("CEEZIX CYBERPUNK CASCADE ONLINE...")
                for (i in 1..10) {
                    val lineStr = (1..20).map {
                        val chars = "0123456789ABCDEF!@#$%^&*()_+{}|:<>?"
                        chars.random()
                    }.joinToString(" ")
                    newOutputs.add("  $lineStr")
                }
            }
            "whoami" -> {
                newOutputs.add("ceezix_dev")
            }
            "pwd" -> {
                newOutputs.add("/data/data/com.termux/files/ceezix_workspace")
            }
            "date" -> {
                newOutputs.add(java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", java.util.Locale.US).format(java.util.Date()))
            }
            "pkg" -> {
                if (parts.size >= 3 && parts[1].lowercase() == "install") {
                    val pkgName = parts[2]
                    newOutputs.add("Installing dependency library: $pkgName...")
                    newOutputs.add("Processing dependencies tree...")
                    newOutputs.add("Fetched package metadata from ceezix package server repositories.")
                    newOutputs.add("Adding schema registry details...")
                    newOutputs.add("SUCCESS: Package '$pkgName' successfully configured in the playground.")
                    viewModel.injectDependency("node", pkgName, "^1.0.0", "// $pkgName runtime template\nconsole.log('$pkgName successfully loaded');")
                } else {
                    newOutputs.add("Usage: pkg install <package_name> (e.g. pkg install lodash)")
                }
            }
            "cat" -> {
                if (parts.size >= 2) {
                    val lookupName = parts[1]
                    val matchFile = viewModel.workspaceFiles.value.find { it.name.equals(lookupName, ignoreCase = true) }
                    if (matchFile != null) {
                        newOutputs.add("--- File: ${matchFile.name} (${matchFile.content.length} bytes) ---")
                        matchFile.content.split("\n").forEach { line ->
                            newOutputs.add("  $line")
                        }
                    } else {
                        newOutputs.add("cat: ${lookupName}: No such file in workspace. Run 'ls' to list files.")
                    }
                } else {
                    newOutputs.add("cat: Missing filename argument. Usage: cat <filename>")
                }
            }
            "node", "python" -> {
                if (parts.size >= 2) {
                    val lookupName = parts[1]
                    val matchFile = viewModel.workspaceFiles.value.find { it.name.equals(lookupName, ignoreCase = true) }
                    if (matchFile != null) {
                        newOutputs.add("[Termux Compiler Engine running ${command.uppercase()} runtime]")
                        newOutputs.add("Executing sandbox workspace processes for ${matchFile.name}...")
                        newOutputs.add("--- EXECUTION CONSOLE STREAMS ---")
                        if (command == "node") {
                            newOutputs.add("  > JS RUNTIME COMPILATION COMPLETED.")
                            if (matchFile.content.contains("console.log")) {
                                matchFile.content.split("\n")
                                    .filter { it.contains("console.log") }
                                    .forEach { line ->
                                        val extracted = line.substringAfter("console.log(").substringBefore(")")
                                            .replace("\"", "").replace("'", "")
                                        newOutputs.add("  [log] $extracted")
                                    }
                            } else {
                                newOutputs.add("  [log] Hello from Ceezix Javascript Playground!")
                            }
                        } else {
                            if (matchFile.content.contains("print")) {
                                matchFile.content.split("\n")
                                    .filter { it.contains("print") }
                                    .forEach { line ->
                                        val extracted = line.substringAfter("print(").substringBefore(")")
                                            .replace("\"", "").replace("'", "")
                                        newOutputs.add("  [python] $extracted")
                                    }
                            } else {
                                newOutputs.add("  > Successfully simulated Python logic. Context parsed details.")
                            }
                        }
                        newOutputs.add("---------------------------------")
                    } else {
                        newOutputs.add("$command: ${lookupName}: Target file not found.")
                    }
                } else {
                    newOutputs.add("Usage: $command <filename> (e.g. $command main.js)")
                }
            }
            "cipher" -> {
                try {
                    if (parts.size >= 3) {
                        val subMode = parts[1].lowercase()
                        if (subMode == "caesar" && parts.size >= 5) {
                            val shift = parts[2].toInt()
                            val rawMsg = parts.drop(3).joinToString(" ")
                            val enc = io.ceezix.cryptography.CipherEngine.encryptCaesar(rawMsg, shift)
                            val dec = io.ceezix.cryptography.CipherEngine.decryptCaesar(rawMsg, shift)
                            newOutputs.add("Cipher Type: Caesar Shift")
                            newOutputs.add("Shift Value: $shift")
                            newOutputs.add("Encrypted  : $enc")
                            newOutputs.add("Decrypted  : $dec")
                        } else if (subMode == "vigenere" && parts.size >= 4) {
                            val key = parts[2]
                            val rawMsg = parts.drop(3).joinToString(" ")
                            val enc = io.ceezix.cryptography.CipherEngine.encryptVigenere(rawMsg, key)
                            val dec = io.ceezix.cryptography.CipherEngine.decryptVigenere(rawMsg, key)
                            newOutputs.add("Cipher Type: Vigenere Keyword")
                            newOutputs.add("Keyword    : $key")
                            newOutputs.add("Encrypted  : $enc")
                            newOutputs.add("Decrypted  : $dec")
                        } else if (subMode == "base64" && parts.size >= 3) {
                            val subAction = parts[2].lowercase()
                            val rawMsg = parts.drop(3).joinToString(" ")
                            if (subAction == "encode") {
                                val enc = io.ceezix.cryptography.CipherEngine.encryptBase64(rawMsg)
                                newOutputs.add("Base64 Encoded: $enc")
                            } else {
                                val dec = io.ceezix.cryptography.CipherEngine.decryptBase64(rawMsg)
                                newOutputs.add("Base64 Decoded: $dec")
                            }
                        } else {
                            newOutputs.add("Invalid cipher flags. Try:")
                            newOutputs.add("  cipher Caesar <shift> <message>")
                            newOutputs.add("  cipher Vigenere <key> <message>")
                            newOutputs.add("  cipher Base64 encode <message>")
                            newOutputs.add("  cipher Base64 decode <message>")
                        }
                    } else {
                        newOutputs.add("Usage:")
                        newOutputs.add("  cipher Caesar <shift> <message>")
                        newOutputs.add("  cipher Vigenere <key> <message>")
                        newOutputs.add("  cipher Base64 encode/decode <message>")
                    }
                } catch (ex: java.lang.Exception) {
                    newOutputs.add("cipher: error parsing inputs: ${ex.message}")
                }
            }
            "git" -> {
                if (parts.size >= 2) {
                    val gitSub = parts[1].lowercase()
                    when (gitSub) {
                        "init" -> {
                            viewModel.gitInit()
                            newOutputs.add("Initialized empty Git repository in virtual workspace.")
                            newOutputs.add("Tracking all files dynamically. Registered initial commit snapshot.")
                        }
                        "status" -> {
                            if (!viewModel.gitInitialized.value) {
                                newOutputs.add("fatal: not a git repository (or any of the parent directories): .git")
                            } else {
                                val staged = viewModel.gitStagedPaths.value
                                val files = viewModel.workspaceFiles.value.filter { !it.isFolder }
                                newOutputs.add("On branch main")
                                if (staged.isEmpty()) {
                                    newOutputs.add("Nothing staged to commit (use 'git add <file>' or stage visually)")
                                } else {
                                    newOutputs.add("Changes to be committed:")
                                    newOutputs.add("  (use \"git restore --staged <file>...\" to unstage)")
                                    staged.forEach { path ->
                                        newOutputs.add("\tstaged:    $path")
                                    }
                                }
                                val unstaged = files.map { it.path }.filter { !staged.contains(it) }
                                if (unstaged.isNotEmpty()) {
                                    newOutputs.add("\nChanges not staged for commit:")
                                    newOutputs.add("  (use \"git add <file>...\" to update what will be committed)")
                                    unstaged.forEach { path ->
                                        newOutputs.add("\tmodified:  $path")
                                    }
                                }
                            }
                        }
                        "add" -> {
                            if (!viewModel.gitInitialized.value) {
                                newOutputs.add("fatal: not a git repository (or any of the parent directories): .git")
                            } else if (parts.size >= 3) {
                                val lookupFile = parts[2]
                                if (lookupFile == "." || lookupFile == "*") {
                                    viewModel.gitStageAllFiles()
                                    newOutputs.add("Staged all workspace files.")
                                } else {
                                    val existsInWorkspace = viewModel.workspaceFiles.value.any { it.path.equals(lookupFile, ignoreCase = true) }
                                    if (existsInWorkspace) {
                                        viewModel.gitToggleStageFile(viewModel.workspaceFiles.value.first { it.path.equals(lookupFile, ignoreCase = true) }.path)
                                        newOutputs.add("Toggled stage status for path: $lookupFile")
                                    } else {
                                        newOutputs.add("fatal: pathspec '$lookupFile' did not match any files.")
                                    }
                                }
                            } else {
                                newOutputs.add("Nothing specified, nothing added. Usage: git add <file_path> or git add .")
                            }
                        }
                        "commit" -> {
                            if (!viewModel.gitInitialized.value) {
                                newOutputs.add("fatal: not a git repository (or any of the parent directories): .git")
                            } else {
                                val mIdx = parts.indexOf("-m")
                                val msg = if (mIdx != -1 && parts.size > mIdx + 1) {
                                    parts.drop(mIdx + 1).joinToString(" ").replace("\"", "").replace("'", "")
                                } else {
                                    ""
                                }
                                if (msg.isBlank()) {
                                    newOutputs.add("error: switch `m' requires a value")
                                    newOutputs.add("Usage: git commit -m \"commit message\"")
                                } else if (viewModel.gitStagedPaths.value.isEmpty()) {
                                    newOutputs.add("error: nothing to commit (create staging references first)")
                                } else {
                                    coroutineScope.launch {
                                        viewModel.gitCommit(msg)
                                    }
                                    newOutputs.add("Successfully committed snapshot: '$msg'")
                                }
                            }
                        }
                        "log" -> {
                            if (!viewModel.gitInitialized.value) {
                                newOutputs.add("fatal: not a git repository (or any of the parent directories): .git")
                            } else {
                                val commits = viewModel.gitCommits.value
                                if (commits.isEmpty()) {
                                    newOutputs.add("No commits recorded yet.")
                                } else {
                                    commits.forEach { commit ->
                                        newOutputs.add("commit ${commit.hash}")
                                        newOutputs.add("Author: ${commit.author}")
                                        newOutputs.add("Date:   ${java.text.SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy", java.util.Locale.US).format(java.util.Date(commit.timestamp))}")
                                        newOutputs.add("    ${commit.message}\n")
                                    }
                                }
                            }
                        }
                        "push" -> {
                            if (!viewModel.gitInitialized.value) {
                                newOutputs.add("fatal: not a git repository (or any of the parent directories): .git")
                            } else if (viewModel.gitRemoteUrl.value.isBlank()) {
                                newOutputs.add("fatal: No remote source set. Configure origin using: git remote add origin <url>")
                            } else {
                                viewModel.gitPush()
                                newOutputs.add("Pushed references successfully. Main trunk updated.")
                            }
                        }
                        "pull" -> {
                            if (!viewModel.gitInitialized.value) {
                                newOutputs.add("fatal: not a git repository (or any of the parent directories): .git")
                            } else if (viewModel.gitRemoteUrl.value.isBlank()) {
                                newOutputs.add("fatal: No remote source configured to fetch upstream branches.")
                            } else {
                                viewModel.gitPull()
                                newOutputs.add("Upstream changes fetched. Merging trunk details.")
                            }
                        }
                        "remote" -> {
                            if (!viewModel.gitInitialized.value) {
                                newOutputs.add("fatal: not a git repository (or any of the parent directories): .git")
                            } else if (parts.size >= 4 && parts[2].lowercase() == "add") {
                                val originUrl = parts.drop(3).joinToString(" ")
                                viewModel.gitRemoteAdd(originUrl)
                                newOutputs.add("Remote origin added: $originUrl")
                            } else {
                                val url = viewModel.gitRemoteUrl.value
                                if (url.isBlank()) {
                                    newOutputs.add("No remotes configured.")
                                } else {
                                    newOutputs.add("origin\t$url (fetch)")
                                    newOutputs.add("origin\t$url (push)")
                                }
                            }
                        }
                        else -> {
                            newOutputs.add("git: '$gitSub' is not a supported git command. Supported: init, status, add, commit, log, remote, push, pull")
                        }
                    }
                } else {
                    newOutputs.add("Usage: git <command> [<args>]")
                    newOutputs.add("Compatible operations: init, status, add, commit, log, remote, push, pull")
                }
            }
            else -> {
                newOutputs.add("termux: command not found: '$command'")
                newOutputs.add("Type 'help' to show all valid ceezix sandbox packages.")
            }
        }

        history = history + newOutputs
        currentInput = ""
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0D0F12))
                    .border(1.dp, Color(0xFF1E242E), RoundedCornerShape(16.dp))
            ) {
                // Termux top header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161A22))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFF5F56))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFBD2E))
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF27C93F))
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = Color(0xFF00FF00),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "termux - bash",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.LightGray,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Terminal",
                            tint = Color.LightGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Quick Macro Command Row for prompt speed
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1F242E))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SHORTCUTS:",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Yellow,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                    listOf("help", "ls", "neofetch", "matrix", "clear", "cipher Caesar 3 test", "cipher Base64 encode peace").forEach { macro ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF10141C))
                                .clickable { executeTerminalCommand(macro) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = macro,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FF00),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Terminal main CLI outputs scrollable column
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .verticalScroll(scrollState)
                ) {
                    history.forEach { line ->
                        Text(
                            text = line,
                            fontSize = 13.sp,
                            color = if (line.startsWith("$")) Color(0xFF38BDF8) else if (line.startsWith("Error") || line.contains("not found")) Color(0xFFF87171) else Color(0xFF34D399),
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(vertical = 1.dp)
                        )
                    }
                }

                // Terminal Keyboard prompt bar input
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF161A22))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "$",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF38BDF8),
                        fontFamily = FontFamily.Monospace
                    )

                    BasicTextField(
                        value = currentInput,
                        onValueChange = { currentInput = it },
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFF000000).copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                            .border(1.dp, Color(0xFF2E3B4E), RoundedCornerShape(6.dp))
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                            .testTag("termux_cli_input_field"),
                        textStyle = TextStyle(
                            color = Color(0xFF00FF00),
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        ),
                        cursorBrush = SolidColor(Color(0xFF00FF00)),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            if (currentInput.isNotBlank()) {
                                executeTerminalCommand(currentInput)
                            }
                        },
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Color(0xFF27C93F)),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Execute Bash CLI Command",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitVersionControlDialog(
    viewModel: CezixViewModel,
    onDismiss: () -> Unit
) {
    val gitInitialized by viewModel.gitInitialized.collectAsState()
    val gitRemoteUrl by viewModel.gitRemoteUrl.collectAsState()
    val gitStagedPaths by viewModel.gitStagedPaths.collectAsState()
    val gitPushedCommits by viewModel.gitPushedCommits.collectAsState()
    val gitCommits by viewModel.gitCommits.collectAsState()
    val workspaceFiles by viewModel.workspaceFiles.collectAsState()

    var activeSubTab by remember { mutableStateOf(0) } // 0: Stage & Commit, 1: History, 2: Remote/Sync
    var commitMessage by remember { mutableStateOf("") }
    var inputRemoteUrl by remember { mutableStateOf(gitRemoteUrl) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(gitRemoteUrl) {
        inputRemoteUrl = gitRemoteUrl
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .padding(12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            ) {
                // Top Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceColorAtElevation(4.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MergeType,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "cee_zix - Git VCS Engine",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Git VCS",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                if (!gitInitialized) {
                    // Empty Repository / Initialize State
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MergeType,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Git Versioning is Not Active",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Initialize Git to begin local snapshot versioning, stage modifications, commit updates, manage remote pushes/pulls, and checkout history snapshots.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = { viewModel.gitInit() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("git_init_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Initialize Local Git Repo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Main Git Panel Tabs
                    TabRow(
                        selectedTabIndex = activeSubTab,
                        containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    ) {
                        Tab(
                            selected = activeSubTab == 0,
                            onClick = { activeSubTab = 0 },
                            text = { Text("Stage & Commit", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = activeSubTab == 1,
                            onClick = { activeSubTab = 1 },
                            text = { Text("Commits Log (${gitCommits.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        Tab(
                            selected = activeSubTab == 2,
                            onClick = { activeSubTab = 2 },
                            text = { Text("Remote / Sync", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                            icon = { Icon(Icons.Default.DeviceHub, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        when (activeSubTab) {
                            0 -> {
                                // Stage & Commit Pane
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "WORKSPACE CHANGES",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = { viewModel.gitStageAllFiles() }) {
                                            Text("Stage All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        TextButton(onClick = { viewModel.gitUnstageAllFiles() }) {
                                            Text("Unstage All", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }

                                // List of changes
                                val nonFolderFiles = workspaceFiles.filter { !it.isFolder }
                                if (nonFolderFiles.isEmpty()) {
                                    Box(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No files in workspace", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), fontSize = 11.sp)
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        modifier = Modifier.weight(1f).fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        items(nonFolderFiles.size) { index ->
                                            val file = nonFolderFiles[index]
                                            val isStaged = gitStagedPaths.contains(file.path)
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isStaged) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                                                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                                ),
                                                border = BorderStroke(
                                                    width = 1.dp,
                                                    color = if (isStaged) MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                             else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                                )
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                        Icon(
                                                            imageVector = if (isStaged) Icons.Default.CheckCircle else Icons.Default.Description,
                                                            contentDescription = null,
                                                            tint = if (isStaged) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Column {
                                                            Text(file.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                            Text(file.path, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                                        }
                                                    }
                                                    Button(
                                                        onClick = { viewModel.gitToggleStageFile(file.path) },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(26.dp),
                                                        colors = ButtonDefaults.buttonColors(
                                                            containerColor = if (isStaged) MaterialTheme.colorScheme.errorContainer
                                                                             else MaterialTheme.colorScheme.primary
                                                        ),
                                                        shape = RoundedCornerShape(4.dp)
                                                    ) {
                                                        Text(
                                                            text = if (isStaged) "Unstage" else "Stage",
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (isStaged) MaterialTheme.colorScheme.onErrorContainer
                                                                    else MaterialTheme.colorScheme.onPrimary
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(10.dp))

                                // Commit Box
                                Text(
                                    text = "COMMIT CHANGES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                OutlinedTextField(
                                    value = commitMessage,
                                    onValueChange = { commitMessage = it },
                                    label = { Text("Commit Message") },
                                    placeholder = { Text("e.g. Added Caesar Cipher decoder engine") },
                                    modifier = Modifier.fillMaxWidth().testTag("git_commit_message_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (commitMessage.isNotBlank()) {
                                            coroutineScope.launch {
                                                viewModel.gitCommit(commitMessage)
                                                commitMessage = ""
                                                Toast.makeText(context, "Commit successful!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    enabled = gitStagedPaths.isNotEmpty() && commitMessage.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth().testTag("git_commit_submit_button"),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Commit [${gitStagedPaths.size} Staged File${if (gitStagedPaths.size > 1) "s" else ""}]", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            1 -> {
                                // History Timeline List
                                Text(
                                    text = "COMMITS TIMELINE LOGS",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                if (gitCommits.isEmpty()) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Text("No commits recorded yet. Commit modifications to record snapshots.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                                        }
                                    }
                                } else {
                                    androidx.compose.foundation.lazy.LazyColumn(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.fillMaxSize()
                                    ) {
                                        items(gitCommits.size) { index ->
                                            val commit = gitCommits[index]
                                            val isPushed = gitPushedCommits.contains(commit.hash)
                                            Card(
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = CardDefaults.cardColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                                                ),
                                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .clip(RoundedCornerShape(4.dp))
                                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                                            ) {
                                                                Text(
                                                                    text = commit.hash,
                                                                    fontSize = 10.sp,
                                                                    fontWeight = FontWeight.ExtraBold,
                                                                    fontFamily = FontFamily.Monospace,
                                                                    color = MaterialTheme.colorScheme.primary
                                                                )
                                                            }
                                                            if (isPushed) {
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                Box(
                                                                    modifier = Modifier
                                                                        .clip(RoundedCornerShape(4.dp))
                                                                        .background(Color(0xFF2DA44E).copy(alpha = 0.15f))
                                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                                ) {
                                                                    Text(
                                                                        text = "pushed",
                                                                        fontSize = 8.sp,
                                                                        fontWeight = FontWeight.Bold,
                                                                        color = Color(0xFF2DA44E)
                                                                    )
                                                                }
                                                            }
                                                        }
                                                        Spacer(modifier = Modifier.height(4.dp))
                                                        Text(commit.message, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                                        Spacer(modifier = Modifier.height(2.dp))
                                                        Text(
                                                            text = "Author: ${commit.author} • ${java.text.SimpleDateFormat("MMM dd, yyyy HH:mm").format(java.util.Date(commit.timestamp))}",
                                                            fontSize = 9.sp,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                    OutlinedButton(
                                                        onClick = {
                                                            viewModel.gitCheckout(commit.hash)
                                                            Toast.makeText(context, "Checked out commit ${commit.hash}", Toast.LENGTH_SHORT).show()
                                                        },
                                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                                        modifier = Modifier.height(28.dp).testTag("git_checkout_${commit.hash}"),
                                                        shape = RoundedCornerShape(6.dp)
                                                    ) {
                                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(12.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Restore", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // Remote setting & push/pull
                                Text(
                                    text = "REMOTE CONFIGURATION (ORIGIN)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )

                                OutlinedTextField(
                                    value = inputRemoteUrl,
                                    onValueChange = { inputRemoteUrl = it },
                                    label = { Text("Remote origin URL") },
                                    placeholder = { Text("https://github.com/ceezix/caesar-sandbox.git") },
                                    modifier = Modifier.fillMaxWidth().testTag("git_remote_url_input"),
                                    shape = RoundedCornerShape(8.dp),
                                    textStyle = TextStyle(fontSize = 12.sp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Button(
                                    onClick = {
                                        if (inputRemoteUrl.isNotBlank()) {
                                            viewModel.gitRemoteAdd(inputRemoteUrl)
                                            Toast.makeText(context, "Remote updated!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.End).testTag("git_remote_save_button"),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("Set Remote Origin", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(24.dp))
                                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                                Spacer(modifier = Modifier.height(24.dp))

                                Text(
                                    text = "PUSH & PULL REMOTES",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.secondary,
                                    letterSpacing = 0.5.sp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { 
                                            viewModel.gitPull()
                                            Toast.makeText(context, "Pulled changes successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).testTag("git_pull_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Pull Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { 
                                            viewModel.gitPush()
                                            Toast.makeText(context, "Pushed commits successfully", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.weight(1f).testTag("git_push_button"),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2DA44E))
                                    ) {
                                        Icon(Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Push Changes", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Hint: Pushing commits will trace them on remote branches. Pulling changes dynamically syncs workspace files (e.g. README.md template) from your active remote repository.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    )
}
