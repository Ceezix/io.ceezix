package io.ceezix.cryptography

object CipherEngine {

    /**
     * Encrypts plaintext using Caesar Cipher with specified shift value (0-25)
     */
    fun encryptCaesar(text: String, shift: Int): String {
        val normalizedShift = ((shift % 26) + 26) % 26
        return text.map { char ->
            when {
                char.isUpperCase() -> {
                    val base = 'A'.code
                    val newCode = (char.code - base + normalizedShift) % 26
                    (newCode + base).toChar()
                }
                char.isLowerCase() -> {
                    val base = 'a'.code
                    val newCode = (char.code - base + normalizedShift) % 26
                    (newCode + base).toChar()
                }
                else -> char
            }
        }.joinToString("")
    }

    /**
     * Decrypts ciphertext using Caesar Cipher with specified shift value (0-25)
     */
    fun decryptCaesar(text: String, shift: Int): String {
        return encryptCaesar(text, 26 - shift)
    }

    /**
     * Atbash Cipher (A <-> Z, B <-> Y, etc.)
     */
    fun encryptAtbash(text: String): String {
        return text.map { char ->
            when {
                char.isUpperCase() -> {
                    val diff = char.code - 'A'.code
                    ('Z'.code - diff).toChar()
                }
                char.isLowerCase() -> {
                    val diff = char.code - 'a'.code
                    ('z'.code - diff).toChar()
                }
                else -> char
            }
        }.joinToString("")
    }

    /**
     * Decrypting Atbash is identical to encrypting because it's self-reciprocal.
     */
    fun decryptAtbash(text: String): String = encryptAtbash(text)

    /**
     * Vigenère Cipher encryption using keyword
     */
    fun encryptVigenere(text: String, key: String): String {
        if (key.isEmpty()) return text
        val cleanKey = key.filter { it.isLetter() }.uppercase()
        if (cleanKey.isEmpty()) return text

        var keyIndex = 0
        return text.map { char ->
            when {
                char.isUpperCase() -> {
                    val shift = cleanKey[keyIndex % cleanKey.length].code - 'A'.code
                    keyIndex++
                    val base = 'A'.code
                    ((char.code - base + shift) % 26 + base).toChar()
                }
                char.isLowerCase() -> {
                    val shift = cleanKey[keyIndex % cleanKey.length].code - 'A'.code
                    keyIndex++
                    val base = 'a'.code
                    ((char.code - base + shift) % 26 + base).toChar()
                }
                else -> char
            }
        }.joinToString("")
    }

    /**
     * Vigenère Cipher decryption using keyword
     */
    fun decryptVigenere(text: String, key: String): String {
        if (key.isEmpty()) return text
        val cleanKey = key.filter { it.isLetter() }.uppercase()
        if (cleanKey.isEmpty()) return text

        var keyIndex = 0
        return text.map { char ->
            when {
                char.isUpperCase() -> {
                    val shift = cleanKey[keyIndex % cleanKey.length].code - 'A'.code
                    keyIndex++
                    val base = 'A'.code
                    var newCode = (char.code - base - shift) % 26
                    if (newCode < 0) newCode += 26
                    (newCode + base).toChar()
                }
                char.isLowerCase() -> {
                    val shift = cleanKey[keyIndex % cleanKey.length].code - 'A'.code
                    keyIndex++
                    val base = 'a'.code
                    var newCode = (char.code - base - shift) % 26
                    if (newCode < 0) newCode += 26
                    (newCode + base).toChar()
                }
                else -> char
            }
        }.joinToString("")
    }

    /**
     * Base64 Encoding routine
     */
    fun encryptBase64(text: String): String {
        if (text.isEmpty()) return ""
        return try {
            android.util.Base64.encodeToString(text.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            "Error inputting raw string: ${e.message}"
        }
    }

    /**
     * Base64 Decoding routine
     */
    fun decryptBase64(text: String): String {
        if (text.isEmpty()) return ""
        return try {
            val decodedBytes = android.util.Base64.decode(text, android.util.Base64.DEFAULT)
            String(decodedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            "Error: Invalid Base64 format string"
        }
    }
}
