package com.github.cvzi.screenshottile.activities


import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.github.cvzi.screenshottile.App
import com.github.cvzi.screenshottile.BR
import com.github.cvzi.screenshottile.R
import com.github.cvzi.screenshottile.databinding.ActivityBackupPrefsBinding
import com.github.cvzi.screenshottile.utils.PrefBackup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class BackupPrefsActivity : AppCompatActivity() {
    companion object {
        const val TAG = "BackupPrefsActivity"

        /**
         * Start activity
         */
        fun start(ctx: Context) = ctx.startActivity(Intent(ctx, BackupPrefsActivity::class.java))
    }

    private lateinit var binding: ActivityBackupPrefsBinding
    private var timeoutJob: Job? = null
    private var exportFilename: String? = null

    private val exportPrefsLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            if (uri != null) {
                try {
                    val json = PrefBackup.exportPrefsToJson(this)
                    contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(json.toByteArray(Charsets.UTF_8))
                        out.flush()
                    }
                    Toast.makeText(
                        this,
                        "Preferences exported\n${exportFilename ?: ""}",
                        Toast.LENGTH_SHORT
                    ).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Export failed", e)
                    Toast.makeText(this, "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }


    private val importPrefsLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                try {
                    val json = contentResolver.openInputStream(uri)?.use { input ->
                        input.bufferedReader().readText()
                    } ?: throw IllegalStateException("Empty file")
                    PrefBackup.importPrefsFromJson(this, json)
                    Toast.makeText(this, "Preferences imported", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Log.e(TAG, "Import failed", e)
                    Toast.makeText(this, "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_backup_prefs)
        binding.setVariable(BR.strings, App.texts)

        binding.btnExportPrefs.setOnClickListener {
            exportFilename = "screenshottile-backup-${
                java.time.LocalDateTime.now().toString().replace(':', '-').split(".")[0]
            }.json"
            exportPrefsLauncher.launch(exportFilename)
        }
        binding.btnImportPrefs.setOnClickListener {
            importPrefsLauncher.launch(arrayOf("application/json", "text/*"))
        }
        binding.btnResetPrefs.setOnClickListener {
            timeoutJob?.cancel()
            timeoutJob = lifecycleScope.launch {
                delay(2000)
                runOnUiThread {
                    confirmReset()
                }
            }

        }
    }

    private fun confirmReset() {
        AlertDialog.Builder(this)
            .setTitle("Reset all settings?")
            .setMessage("This will erase all custom settings and restore defaults. Continue?")
            .setPositiveButton("Reset") { _, _ ->
                resetPreferences()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun resetPreferences() {
        PrefBackup.resetToDefaults(this)
        Toast.makeText(this, "All settings reset to default", Toast.LENGTH_SHORT).show()
        // Restart
        MainActivity.startNewTask(this)
        finish()
    }

    override fun onPause() {
        super.onPause()
        timeoutJob?.cancel()
        timeoutJob = null
    }
}
