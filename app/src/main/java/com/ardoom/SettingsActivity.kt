package com.ardoom

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var labelMasterVolume: TextView
    private lateinit var seekbarMasterVolume: SeekBar
    private lateinit var labelSfxVolume: TextView
    private lateinit var seekbarSfxVolume: SeekBar
    private lateinit var switchHaptics: SwitchCompat
    private lateinit var spinnerDifficulty: Spinner
    private lateinit var labelCameraSensitivity: TextView
    private lateinit var seekbarCameraSensitivity: SeekBar
    private lateinit var switchShowFps: SwitchCompat
    private lateinit var btnSave: Button
    private lateinit var btnBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Load existing settings
        GameSettings.load(this)

        // Bind views
        labelMasterVolume = findViewById(R.id.label_master_volume)
        seekbarMasterVolume = findViewById(R.id.seekbar_master_volume)
        labelSfxVolume = findViewById(R.id.label_sfx_volume)
        seekbarSfxVolume = findViewById(R.id.seekbar_sfx_volume)
        switchHaptics = findViewById(R.id.switch_haptics)
        spinnerDifficulty = findViewById(R.id.spinner_difficulty)
        labelCameraSensitivity = findViewById(R.id.label_camera_sensitivity)
        seekbarCameraSensitivity = findViewById(R.id.seekbar_camera_sensitivity)
        switchShowFps = findViewById(R.id.switch_show_fps)
        btnSave = findViewById(R.id.btn_save)
        btnBack = findViewById(R.id.btn_back)

        // Setup Master Volume Seekbar
        val masterVolProgress = (GameSettings.masterVolume * 100).toInt()
        seekbarMasterVolume.progress = masterVolProgress
        updateMasterVolumeLabel(masterVolProgress)
        seekbarMasterVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateMasterVolumeLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Setup SFX Volume Seekbar
        val sfxVolProgress = (GameSettings.sfxVolume * 100).toInt()
        seekbarSfxVolume.progress = sfxVolProgress
        updateSfxVolumeLabel(sfxVolProgress)
        seekbarSfxVolume.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                updateSfxVolumeLabel(progress)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Setup Haptics Toggle
        switchHaptics.isChecked = GameSettings.hapticsEnabled

        // Setup Difficulty Spinner
        val difficultyLabels = listOf(
            getString(R.string.difficulty_easy),
            getString(R.string.difficulty_normal),
            getString(R.string.difficulty_hard),
            getString(R.string.difficulty_nightmare)
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, difficultyLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDifficulty.adapter = adapter
        spinnerDifficulty.setSelection(GameSettings.difficulty.ordinal)

        // Setup Camera Sensitivity Seekbar (0.5 to 2.0 mapped onto 0..15 progress)
        val sensProgress = ((GameSettings.cameraSensitivity - 0.5f) * 10f).toInt().coerceIn(0, 15)
        seekbarCameraSensitivity.progress = sensProgress
        updateCameraSensitivityLabel(0.5f + sensProgress * 0.1f)
        seekbarCameraSensitivity.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val sensitivity = 0.5f + progress * 0.1f
                updateCameraSensitivityLabel(sensitivity)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Setup Show FPS Toggle
        switchShowFps.isChecked = GameSettings.showFPS

        // Save Button
        btnSave.setOnClickListener {
            saveSettings()
            finish()
        }

        // Back Button
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun updateMasterVolumeLabel(progress: Int) {
        labelMasterVolume.text = getString(R.string.label_master_volume, progress)
    }

    private fun updateSfxVolumeLabel(progress: Int) {
        labelSfxVolume.text = getString(R.string.label_sfx_volume, progress)
    }

    private fun updateCameraSensitivityLabel(sensitivity: Float) {
        labelCameraSensitivity.text = getString(R.string.label_camera_sensitivity, sensitivity)
    }

    private fun saveSettings() {
        GameSettings.masterVolume = seekbarMasterVolume.progress / 100f
        GameSettings.sfxVolume = seekbarSfxVolume.progress / 100f
        GameSettings.hapticsEnabled = switchHaptics.isChecked

        val selectedDifficultyIndex = spinnerDifficulty.selectedItemPosition
        GameSettings.difficulty = Difficulty.values().getOrElse(selectedDifficultyIndex) { Difficulty.NORMAL }

        GameSettings.cameraSensitivity = 0.5f + seekbarCameraSensitivity.progress * 0.1f
        GameSettings.showFPS = switchShowFps.isChecked

        GameSettings.save(this)
    }
}
