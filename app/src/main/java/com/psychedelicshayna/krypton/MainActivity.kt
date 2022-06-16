package com.psychedelicshayna.krypton

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.android.synthetic.main.activity_main_layout.*

class MainActivity : AppCompatActivity() {
    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (resultCode == RESULT_OK && requestCode == ActivityResultRequestCodes.MainActivity.loadVaultFile) {
            resultData?.data?.also { selectedFileUri ->
                contentResolver.takePersistableUriPermission(
                    selectedFileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                loadVault(selectedFileUri)
            }
        }
    }

    private fun loadVault(vaultFileUri: Uri? = null) {
        if (vaultFileUri == null) {
            val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)

                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

                type = "*/*"
            }

            startActivityForResult(openFileIntent, ActivityResultRequestCodes.MainActivity.loadVaultFile)
        } else {
            Intent(this, VaultViewer::class.java).apply {
                putExtra("VaultFileUri", vaultFileUri.toString())
                startActivity(this)
            }
        }
    }

    private fun loadDefaultVault() {
        val defaultVaultFileUri: Uri = getSharedPreferences("KryptonPreferences", MODE_PRIVATE).run {
            getString("DefaultVaultUri", null)?.let { uriString ->
                Uri.parse(uriString)
            }
        } ?: run {
            Toast.makeText(this, "No default vault has been set!", Toast.LENGTH_SHORT).show()
            return
        }

        loadVault(defaultVaultFileUri)
    }

    private fun newVault() =
        startActivity(Intent(this, VaultViewer::class.java))

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.activity_main_layout)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_layout)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        btnLoadVault.setOnClickListener { loadVault() }
        btnLoadDefaultVault.setOnClickListener { loadDefaultVault() }
        btnNewVault.setOnClickListener { newVault() }
    }
}
