package com.psychedelicshayna.krypton

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Layout
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_account_viewer.*
import kotlinx.android.synthetic.main.dialog_integrity_check_result.*
import org.json.*
import java.nio.charset.StandardCharsets
import org.apache.commons.io.IOUtils
import org.w3c.dom.Text
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.security.MessageDigest

class VaultViewer : AppCompatActivity() {
    private lateinit var vaultAccountAdapter: VaultAccountAdapter
    private val vaultSecurity: VaultSecurity = VaultSecurity()
    private var receivedVaultFileUri: Uri? = null
    private var vaultWasDecryptedWhenLoading: Boolean = false

    private fun revertVaultChanges() {

    }

    private fun diffVault() {

    }

    private fun configureVaultSecurity() {

    }

    private fun addAccount() {
        val promptView: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_new_account,
            null
        )

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(promptView)

        val etAccountName: EditText = promptView.findViewById<EditText>(R.id.etAccountName)

        alertDialogBuilder.apply {
            setCancelable(true)
            setTitle("Enter Account Name")

            setPositiveButton("Add") { _, _ ->
                val accountName: String = etAccountName.text.toString()

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

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()
    }

    private fun dumpVaultJson(): JSONObject {
        return JSONObject().apply {
            for(vaultAccount in vaultAccountAdapter.getVaultAccounts()) {
                this.put(vaultAccount.AccountName, JSONObject().apply {
                    for(accountEntry in vaultAccount.AccountEntries) {
                        put(accountEntry.key, accountEntry.value)
                    }
                })
            }
        }
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

    private fun saveVault(uri: Uri? = null) {
        val vaultFileUri: Uri = uri ?: receivedVaultFileUri.let {
            if(it != null) { it } else {
                Toast.makeText(this, "No vault file was opened! Use Save As instead.", Toast.LENGTH_LONG).show()
                return
            }
        }

        val vaultData: ByteArray = dumpVaultJson().toString().toByteArray()
        val vaultDataHash: ByteArray = MessageDigest.getInstance("SHA-256").digest(vaultData)

        contentResolver.openFileDescriptor(vaultFileUri, "w")?.let { parcelFileDescriptor ->
            FileOutputStream(parcelFileDescriptor.fileDescriptor).use { fileOutputStream ->
                fileOutputStream.write(vaultData)
            }
        }

        val vaultDataWritten: ByteArray = contentResolver.openFileDescriptor(vaultFileUri, "rw")?.let { parcelFileDescriptor ->
            FileInputStream(parcelFileDescriptor.fileDescriptor).let { fileInputStream ->
                val data = fileInputStream.readBytes()
                fileInputStream.close()
                data
            }
        }.let {
            if(it != null) { it } else {
                Toast.makeText(this, "Could not re-read the data! Something went wrong!", Toast.LENGTH_LONG).show()
                return
            }
        }

        val vaultDataHashInRam: ByteArray = MessageDigest.getInstance("SHA-256").digest(vaultData)
        val vaultDataHashOnDisk: ByteArray = MessageDigest.getInstance("SHA-256").digest(vaultDataWritten)

        val dialogIntegrityCheckResultView: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_integrity_check_result,
            null,
            false
        )

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogIntegrityCheckResultView)

        val etIntegrityCheckRamHash: TextView = dialogIntegrityCheckResultView.findViewById<TextView>(R.id.etIntegrityCheckRamHash).let {
            if(it != null) {
                it
            } else {
                Toast.makeText(this, "Couldn't find TextView etIntegrityCheckRamHash, was null!", Toast.LENGTH_LONG).show()
                return
            }
        }

        val etIntegrityCheckDiskHash: TextView = dialogIntegrityCheckResultView.findViewById<TextView>(R.id.etIntegrityCheckDiskHash).let {
            if(it != null) {
                it
            } else {
                Toast.makeText(this, "Couldn't find TextView etIntegrityCheckDiskHash, was null!", Toast.LENGTH_LONG).show()
                return
            }
        }

        alertDialogBuilder.apply {
            setView(dialogIntegrityCheckResultView)
            etIntegrityCheckRamHash.text = vaultDataHashInRam.joinToString("") { byte -> "%02x".format(byte) }
            etIntegrityCheckDiskHash.text = vaultDataHashOnDisk.joinToString("") { byte -> "%02x".format(byte) }

            if(vaultDataHashInRam.contentEquals(vaultDataHashOnDisk)) {
                setTitle("Integrity Check Passed!")
                setMessage("The SHA-256 hash of the vault in RAM matches the hash of the vault on the disk.")

                etIntegrityCheckRamHash.setTextColor(Color.GREEN)
                etIntegrityCheckDiskHash.setTextColor(Color.GREEN)
            } else {
                setTitle("Integrity Check Failed!")
                setMessage("The SHA-256 hash of the vault in RAM does not match the hash of the vault on the disk! The data was not stored properly. " +
                        "The recommended action is to use Save As to save the vault to a new location instead.")

                etIntegrityCheckRamHash.setTextColor(Color.RED)
                etIntegrityCheckDiskHash.setTextColor(Color.RED)
            }
        }.create().show()
    }

    private fun saveVaultAs() {

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
            } else {
                vaultWasDecryptedWhenLoading = false
            }

            return
        }

        // If that doesn't work, attempt to decrypt it as if it were AES encrypted.
        Toast.makeText(this, "Failed to parse vault, please provide parameters for decryption!", Toast.LENGTH_LONG).show()

        val vaultCredentialsPromptView: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_vault_credentials, null
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
                    vaultWasDecryptedWhenLoading = true
                    alertDialog.dismiss()
                    return@setOnClickListener
                } else {
                    Toast.makeText(this, "There was a problem with the JSON structure! " +
                            "The JSON is valid, but a different structure was expected!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "The provided credentials did not work!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            this@VaultViewer.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_viewer)

        vaultAccountAdapter = VaultAccountAdapter(mutableListOf())

        receivedVaultFileUri = intent.getStringExtra("VaultFileUri").let {
            if(it != null) Uri.parse(it)
            else null
        }

        rvVaultAccounts.adapter = vaultAccountAdapter
        rvVaultAccounts.layoutManager = LinearLayoutManager(this)

        vaultAccountAdapter.vaultAccountViewClickListener = fun(holder, index) {
            val vaultAccountAtIndex: VaultAccount = try {
                vaultAccountAdapter.itemAt(index)
            } catch(exception: IndexOutOfBoundsException) {
                null
            }.let {
                if(it != null) { it } else {
                    Toast.makeText(this, "IndexOutOfBoundsException when " +
                            "accessing clicked vault.", Toast.LENGTH_LONG).show()

                    return
                }
            }

            Intent(this, AccountEntryViewer::class.java).apply {
                putExtra("VaultAccountObject", vaultAccountAtIndex)
                startActivity(this)
            }
        }

        btnAddAccount.setOnClickListener    { addAccount() }

        btnSave.setOnClickListener {
            receivedVaultFileUri.let {
                if(it != null) { it } else {
                    Toast.makeText(this, "No vault file was opened! Use Save As instead.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            if(vaultWasDecryptedWhenLoading) {
                val vaultCredentialsPromptView: View = LayoutInflater.from(this).inflate(
                    R.layout.dialog_vault_credentials, null
                )

                val alertDialogBuilder = AlertDialog.Builder(this)
                alertDialogBuilder.setView(vaultCredentialsPromptView)

                alertDialogBuilder.apply {
                    setCancelable(true)
                    setTitle("Retype Your Password")

                    setPositiveButton("Encrypt") { _, _ -> }

                    setNeutralButton("Cancel")   { _, _ ->
                        Toast.makeText(this@VaultViewer, "Vault wasn't saved, password wasn't provided!", Toast.LENGTH_LONG).show()
                    }
                }

                val alertDialog: AlertDialog = alertDialogBuilder.create()
                alertDialog.show()

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    val etPassword: EditText = vaultCredentialsPromptView.findViewById(R.id.etPassword)

                    if(vaultSecurity.verifyPassword(etPassword.text.toString())) {
                        alertDialog.dismiss()
                        saveVault()
                    } else {
                        Toast.makeText(this, "The password doesn't match the password used to load the vault!", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                saveVault()
            }
        }

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