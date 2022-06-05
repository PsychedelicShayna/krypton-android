package com.psychedelicshayna.krypton

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.android.synthetic.main.activity_main.*

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
        val defaultVaultFileUri: Uri? = sharedPreferences.getString("DefaultVaultFilePath", null).let {
            if(it != null) Uri.parse(it)
            else {
                Toast.makeText(this, "No default vault has been set!", Toast.LENGTH_SHORT).show()
                return
            }
        }

        loadVaultFileUri = defaultVaultFileUri
        loadVault()
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