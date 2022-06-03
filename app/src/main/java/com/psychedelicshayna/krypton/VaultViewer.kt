package com.psychedelicshayna.krypton

import android.app.Activity
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.style.TabStopSpan
import android.util.JsonReader
import android.util.Xml
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.SearchView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_vault_viewer.*
import kotlinx.android.synthetic.main.vault_credentials_prompt.*
import org.json.*
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.Locale
import org.apache.commons.io.IOUtils
import java.io.FileNotFoundException

class VaultViewer : AppCompatActivity() {
    private lateinit var vaultAccountAdapter: VaultAccountAdapter
    private val vaultSecurity: VaultSecurity = VaultSecurity()
    private var receivedVaultFileUri: Uri? = null

    private fun addAccount() {
        val promptView: View = LayoutInflater.from(this).inflate(
            R.layout.new_account_prompt,
            null
        )

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(promptView)

        val etAccountName:EditText = promptView.findViewById<EditText>(R.id.etAccountName)

        alertDialogBuilder.apply {
            setCancelable(true)
            setTitle("Enter Account Name")

            setPositiveButton("Add") { _, _ ->
                val accountName:String = etAccountName.text.toString()

                if(accountName.isNotBlank()) {
                    val success = vaultAccountAdapter.addVaultAccount(VaultAccount(etAccountName.text.toString()))

                    if(!success) {
                        Toast.makeText(this.context, "An account with that name already exists!", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            setNeutralButton("Cancel") { _, _ ->

            }
        }

        val alertDialog:AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun saveVault(){
    }

    private fun saveVaultAs() {

    }

    private fun revertVaultChanges() {

    }

    private fun diffVault() {

    }

    private fun configureVaultSecurity() {

    }

    private fun loadVaultJson(vaultDataJson: JSONObject): Boolean {
        vaultAccountAdapter.clearVaultAccounts()

        try {
            for(accountName in vaultDataJson.keys()) {
                val accountEntriesMap: MutableMap<String, String> = mutableMapOf()
                val accountEntries: JSONObject = vaultDataJson.getJSONObject(accountName)

                for(accountEntry: String in accountEntries.keys()) {
                    accountEntriesMap[accountEntry] = accountEntries.getString(accountEntry)
                }

                vaultAccountAdapter.addVaultAccount(VaultAccount(accountName, accountEntriesMap))
            }
        } catch(exception: JSONException) {
            Toast.makeText(this, "Encountered exception when adding entries to viewer. " +
                    "The JSON was loaded and parsed, but the structure might be wrong.", Toast.LENGTH_LONG).show()

            vaultAccountAdapter.clearVaultAccounts()
            return false
        }

        return true
    }

    private fun loadVaultFile(vaultFileUri: Uri) {
        val vaultFileDataBytes: ByteArray? = try {
            applicationContext.contentResolver.openInputStream(vaultFileUri)?.use { inputStream ->
                IOUtils.toByteArray(inputStream)
            }
        } catch(exception: FileNotFoundException) {
            Toast.makeText(this, "FileNotFoundException when attempting to open vault file \"$vaultFileUri\"", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if(vaultFileDataBytes == null) {
            Toast.makeText(this, "Failed to read vault file; file data is null!", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val vaultFileDataString: String = vaultFileDataBytes.toString(StandardCharsets.UTF_8)

        // Attempt to parse the vault data as if it were plaintext JSON.
        var vaultFileDataJson: JSONObject? = try {
            JSONObject(vaultFileDataString)
        } catch(exception: JSONException) {
            null
        }

        if(vaultFileDataJson != null) {
            if(!loadVaultJson(vaultFileDataJson)) {
                Toast.makeText(this, "There was a problem with the JSON structure! " +
                        "The JSON is valid, but a different structure was expected!", Toast.LENGTH_LONG).show()

                finish()
            }

            return
        }

        // If that doesn't work, attempt to decrypt it as if it were AES encrypted.
        Toast.makeText(this, "Failed to parse vault, please provide parameters for decryption!", Toast.LENGTH_LONG).show()

        val vaultCredentialsPromptView: View = LayoutInflater.from(this).inflate(
            R.layout.vault_credentials_prompt, null
        )

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(vaultCredentialsPromptView)

        alertDialogBuilder.apply {
            setCancelable(true)
            setTitle("Provide Decryption Parameters")
            setPositiveButton("Decrypt") { _, _ -> }
            setNeutralButton("Cancel")   { _, _ -> this@VaultViewer.finish() }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val password: String = vaultCredentialsPromptView.findViewById<EditText>(R.id.etPassword).text.toString()
            val ivMaskLength: Int = vaultCredentialsPromptView.findViewById<EditText>(R.id.etIVMaskLength).text.toString().toIntOrNull() ?: vaultSecurity.aesBlockSize

            if(ivMaskLength < vaultSecurity.aesBlockSize) {
                Toast.makeText(this, "The provided IV mask length is invalid! Must be greater than or equal " +
                        "to the AES block size: ${vaultSecurity.aesBlockSize}", Toast.LENGTH_LONG).show()

                return@setOnClickListener
            }

            vaultSecurity.setCryptoParameters(password, ivMaskLength)

            vaultFileDataJson = try {
                JSONObject(String(vaultSecurity.decryptVault(vaultFileDataBytes)))
            } catch(exception: JSONException) {
                null
            }

            if(vaultFileDataJson != null) {
                if(loadVaultJson(vaultFileDataJson as JSONObject)) {
                    alertDialog.dismiss()
                    return@setOnClickListener
                } else {
                    Toast.makeText(this, "There was a problem with the JSON structure! " +
                            "The JSON is valid, but a different structure was expected!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "The provided credentials did not work!", Toast.LENGTH_LONG).show()
            }

            this@VaultViewer.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault_viewer)

        vaultAccountAdapter = VaultAccountAdapter(mutableListOf())
        receivedVaultFileUri = Uri.parse(intent.getStringExtra("VaultFileUri"))

        rvVaultAccounts.adapter = vaultAccountAdapter
        rvVaultAccounts.layoutManager = LinearLayoutManager(this)

        btnAddAccount.setOnClickListener    { addAccount() }
        btnSave.setOnClickListener          { saveVault() }
        btnSaveAs.setOnClickListener        { saveVaultAs() }
        btnRevertChanges.setOnClickListener { revertVaultChanges() }
        btnDiff.setOnClickListener          { diffVault() }
        btnSecurity.setOnClickListener      { configureVaultSecurity() }

        svAccountSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(text: String?): Boolean {
                if(text == null) return false

                if(text.isEmpty()) {
                    vaultAccountAdapter.clearAccountNameFilter()
                } else {
                    vaultAccountAdapter.setAccountNameFilter(text)
                }

                return true
            }
        })

        if(receivedVaultFileUri != null) loadVaultFile(receivedVaultFileUri!!)
    }
}