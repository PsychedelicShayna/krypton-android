package com.psychedelicshayna.krypton

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
//import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var btnLoadVault        : Button
    private lateinit var btnLoadDefaultVault : Button
    private lateinit var btnNewVault         : Button

    private var loadVaultFilePath: String? = null
    private val loadVaultActivityResultRequestCode: Int = 1

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(resultCode == RESULT_OK && requestCode == loadVaultActivityResultRequestCode) {
            if (data != null) {
                loadVaultFilePath = data.dataString
                loadVault()
            }
        }
    }

    private fun loadVault() {
        if(loadVaultFilePath.isNullOrBlank()) {
            val openFileIntent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                type = "*/*"
            }

            startActivityForResult(openFileIntent, loadVaultActivityResultRequestCode)
        } else {
            val openVaultIntent = Intent(this, VaultViewer::class.java).apply {
                putExtra("VaultFilePath", loadVaultFilePath)
            }

            startActivity(openVaultIntent)
            loadVaultFilePath = null
        }
    }

    private fun loadDefaultVault() {
        val defaultVaultFilePath: String? = sharedPreferences.getString("DefaultVaultFilePath", null)

        if(defaultVaultFilePath.isNullOrBlank()) {
            Toast.makeText(this, "No default vault has been set!", Toast.LENGTH_SHORT).show()
        }

        else {
            val defaultVaultFile = File(defaultVaultFilePath)

            if(defaultVaultFile.exists()) {
                loadVaultFilePath = defaultVaultFilePath
                loadVault()
            } else {
                Toast.makeText(this,
                    "The path to the default vault file does not exist! - $defaultVaultFilePath",
                    Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun newVault() {
        val newVaultIntent = Intent(this, VaultViewer::class.java)
        startActivity(newVaultIntent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sharedPreferences   = getSharedPreferences("KryptonPreferences", Context.MODE_PRIVATE)

        btnLoadVault        = findViewById(R.id.btnLoadVault)
        btnLoadDefaultVault = findViewById(R.id.btnLoadDefaultVault)
        btnNewVault         = findViewById(R.id.btnNewVault)

        btnLoadVault.setOnClickListener        { loadVault()        }
        btnLoadDefaultVault.setOnClickListener { loadDefaultVault() }
        btnNewVault.setOnClickListener         { newVault()         }
    }
}