package com.ardoom

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainMenuActivity : AppCompatActivity() {

    private lateinit var textHighScore: TextView
    private lateinit var btnPlay: Button
    private lateinit var btnSettings: Button
    private lateinit var btnHowToPlay: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        textHighScore = findViewById(R.id.text_high_score)
        btnPlay = findViewById(R.id.btn_play)
        btnSettings = findViewById(R.id.btn_settings)
        btnHowToPlay = findViewById(R.id.btn_how_to_play)

        btnPlay.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnHowToPlay.setOnClickListener {
            showHowToPlayDialog()
        }

        updateHighScoreDisplay()
    }

    override fun onResume() {
        super.onResume()
        updateHighScoreDisplay()
    }

    private fun updateHighScoreDisplay() {
        GameSettings.load(this)
        textHighScore.text = getString(R.string.high_score_label, GameSettings.highScore)
    }

    private fun showHowToPlayDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.how_to_play_title))
            .setMessage(getString(R.string.how_to_play_content))
            .setPositiveButton(getString(R.string.button_close)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
