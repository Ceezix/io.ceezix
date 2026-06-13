package io.ceezix.models

data class CipherChallenge(
    val id: String,
    val title: String,
    val description: String,
    val cipherType: String, // "Caesar", "Atbash", "Vigenere"
    val ciphertext: String,
    val expectedShift: Int = 0,
    val expectedKey: String = "",
    val clue: String,
    val xpReward: Int,
    val difficulty: String // "EASY", "MEDIUM", "HARD"
)

object ChallengeDatabase {
    val challenges = listOf(
        CipherChallenge(
            id = "roman_general",
            title = "The Roman General",
            description = "An ancient message intercepted from a centurion marching along the Rhine. He seems to have shifted his letters to guard his attack plans.",
            cipherType = "Caesar",
            ciphertext = "MDUV DUH WR EH GHSOXBHG DW GDZQ",
            expectedShift = 3,
            clue = "The word 'MDUV' is Caesar shifted by 3. Try rotating the wheel 3 clicks back to decode!",
            xpReward = 100,
            difficulty = "EASY"
        ),
        CipherChallenge(
            id = "atbash_scroll",
            title = "The Mirrors of Giza",
            description = "A reflective scroll discovered inside the chamber of tombs. The letters seem to be mirrored perfectly from front to back.",
            cipherType = "Atbash",
            ciphertext = "GSV TREZ KBIZNRW SLOWH GSV KZHHTVBVB",
            clue = "This uses the Atbash layout: A becomes Z, B becomes Y. Decrypt it to reveal Pharaoh's entry password.",
            xpReward = 150,
            difficulty = "EASY"
        ),
        CipherChallenge(
            id = "deep_space",
            title = "Centauri Intercept",
            description = "A crackling stellar transmission from Proxima Centauri. Our radio telescopes cleaned up the radio noise, but the telemetry remains encoded.",
            cipherType = "Caesar",
            ciphertext = "WUXVW WKH SURFHVV DQG NHHS LQLWLDWLQJ CONTACT",
            expectedShift = 3,
            clue = "Standard galactic sentinel shift. Try shift Left 3 (Value 3) or Right 23.",
            xpReward = 200,
            difficulty = "MEDIUM"
        ),
        CipherChallenge(
            id = "spy_whisper",
            title = "A Cold War Whisper",
            description = "A shortwave radio numbers station broadcast a specific string before going silent. A double agent mentioned something about a 'three shift left' rule.",
            cipherType = "Caesar",
            ciphertext = "WKH PHHWLQJ LV DW PLGQLJKW",
            expectedShift = 3,
            clue = "The first word 'WKH' maps directly to 'THE'.",
            xpReward = 180,
            difficulty = "MEDIUM"
        ),
        CipherChallenge(
            id = "vigenere_fort",
            title = "The Fortress Key",
            description = "An encrypted dispatch sent over Telegraph that requires a symmetric codeword to unlock the citadel gates.",
            cipherType = "Vigenere",
            ciphertext = "ALW CHXW GOM AW VPSIOWO",
            expectedKey = "KEY",
            clue = "This uses Vigenère encryption! The keyword is 'KEY'. Select Vigenère Mode, input Code 'KEY' to decrypt.",
            xpReward = 300,
            difficulty = "HARD"
        ),
        CipherChallenge(
            id = "alien_beacon",
            title = "Cosmic Quantum Signal",
            description = "An anomalous burst of high-energy neutrinos detected containing a math code shifted backward. We believe it's a cosmic greeting.",
            cipherType = "Caesar",
            ciphertext = "OLSSV LHSAOSPUNZ DL JVTL PU WLHJL",
            expectedShift = 7,
            clue = "A shift of 7. Top text 'OLSSV' unlocks to 'HELLO'!",
            xpReward = 250,
            difficulty = "MEDIUM"
        )
    )
}
