package com.psychedelicshayna.krypton

import android.app.UiModeManager.MODE_NIGHT_YES
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
//import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContentProviderCompat.requireContext
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_account_viewer.*
import java.io.File
//import com.psychedelicshayna.krypton.

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    private var loadVaultFileUri: Uri? = null
    private val loadVaultActivityResultRequestCode: Int = 1

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if(resultCode == RESULT_OK && requestCode == loadVaultActivityResultRequestCode) {
            resultData?.data?.also {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                loadVaultFileUri = it
                loadVault()
            }
        }
    }

    private fun loadVault() {
        if(loadVaultFileUri == null) {
            val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                type = "*/*"
            }

            startActivityForResult(openFileIntent, loadVaultActivityResultRequestCode)
        } else {
            Intent(this, VaultViewer::class.java).apply {
                putExtra("VaultFileUri", loadVaultFileUri.toString())
                startActivity(this)
            }

            loadVaultFileUri = null
        }
    }

    private fun loadDefaultVault() {
//
//        val defaultVaultFilePath: String? = sharedPreferences.getString("DefaultVaultFilePath", null)
//
//        if(defaultVaultFilePath.isNullOrBlank()) {
//            Toast.makeText(this, "No default vault has been set!", Toast.LENGTH_SHORT).show()
//        }
//
//        else {
//            val defaultVaultFile = File(defaultVaultFilePath)
//
//            if(defaultVaultFile.exists()) {
//                loadVaultFilePath = defaultVaultFilePath
//                loadVault()
//            } else {
//                Toast.makeText(this,
//                    "The path to the default vault file does not exist! - $defaultVaultFilePath",
//                    Toast.LENGTH_SHORT).show()
//            }
//        }
    }

    private fun newVault() {
        val newVaultIntent = Intent(this, VaultViewer::class.java)
        startActivity(newVaultIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

        sharedPreferences   = getSharedPreferences("KryptonPreferences", Context.MODE_PRIVATE)

        btnLoadVault.setOnClickListener        { loadVault()        }
        btnLoadDefaultVault.setOnClickListener { loadDefaultVault() }
        btnNewVault.setOnClickListener         { newVault()         }
    }
}