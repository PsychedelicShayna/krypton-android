package com.psychedelicshayna.krypton

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_vault_account_viewer.*
import org.apache.commons.io.IOUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.MessageDigest


class VaultViewer : AppCompatActivity() {
    private lateinit var clipboardManager: ClipboardManager

    private lateinit var vaultAdapter: VaultAdapter
    private var vaultBackup: Vault = Vault()

    private val kryptonCrypto: KryptonCrypto = KryptonCrypto()
    private var vaultEncryptionEnabled: Boolean = false

    private var activeVaultFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault_account_viewer)

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        vaultAdapter = VaultAdapter(this, Vault())

        vaultAdapter.onBindViewHolderListener = { holder, position ->
            holder.onContextMenuItemClickListener = { menuItem, position, contextMenu, view, contextMenuInfo ->
                onAccountAdapterItemViewContextMenuItemSelected(menuItem, position, contextMenu, view, contextMenuInfo)
            }

            holder.itemView.setOnClickListener {
                val clickedVaultAccount: Vault.Account? = vaultAdapter.getDisplayVaultAccount(position)

                val openEntryViewerIntent = Intent(
                    this,
                    VaultAccountEntryViewer::class.java
                ).apply {
                    putExtra("VaultAccount", clickedVaultAccount)
                }

                startActivityForResult(openEntryViewerIntent, ActivityResultRequestCodes.EntryViewer.updateAccount)
            }
        }

        activeVaultFileUri = intent.getStringExtra("VaultFileUri")?.let { Uri.parse(it) }

        findViewById<RecyclerView>(R.id.activityAccountViewerRecyclerViewVaultAccounts).apply {
            layoutManager = LinearLayoutManager(this@VaultViewer)
            adapter = vaultAdapter
        }

        activityAccountViewerButtonAddAccount.setOnClickListener(::addAccount)
        activityAccountViewerButtonSave.setOnClickListener(::onSaveButtonClickListener)

        activityAccountViewerButtonSaveAs.setOnClickListener        { saveVaultAs() }

        activityAccountViewerButtonRevertChanges.setOnClickListener {
            vaultAdapter.setVault(vaultBackup)
        }

        activityAccountViewerButtonDiff.setOnClickListener          { diffVault() }
        activityAccountViewerButtonSecurity.setOnClickListener      { configureVaultSecurity() }

        activityAccountViewerSearchViewAccountSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(text: String?): Boolean {
                if(text == null) return false

                if(text.isEmpty()) {
                    vaultAdapter.clearAccountNameSearchQuery()
                } else {
                    vaultAdapter.performAccountNameSearchQuery(text)
                }

                return true
            }
        })

        if(activeVaultFileUri != null) loadVaultFile(activeVaultFileUri!!)
    }

    override fun onBackPressed() {
        if(unsavedChanges()) {
            val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
                setCancelable(false)
                title = "Unsaved Changes"
                setMessage("You have unsaved changes, are you sure you want to exit?")

                setPositiveButton("Yes") { _, _ ->
                    super.onBackPressed()
                }

                setNegativeButton("Cancel") { _, _ -> }
            }

            alertDialogBuilder.create().show()
        } else {
            super.onBackPressed()
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.activity_vault_account_viewer)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if(resultCode == RESULT_OK && requestCode == ActivityResultRequestCodes.AccountViewer.saveVaultAs) {
            resultData?.data?.also {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                            or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                saveVaultAs(it)
            }
        }

        if(resultCode == RESULT_OK && requestCode == ActivityResultRequestCodes.EntryViewer.updateAccount) {
            resultData?.run {
                val updatedVaultAccount: Vault.Account =
                    (getSerializableExtra("VaultAccount") as Vault.Account?) ?: run {

                        Toast.makeText(this@VaultViewer, "Error! Received null as the " +
                                "updated vault account extra!", Toast.LENGTH_LONG).show()

                        return@onActivityResult
                }


                val updatedVaultAccountIndex: Int =
                    vaultAdapter.getVaultAccountIndexByName(updatedVaultAccount.name) ?: run {
                        Toast.makeText(this@VaultViewer, "Error! The index of the updated " +
                                "account was null!", Toast.LENGTH_LONG).show()

                        return@onActivityResult
                }

                vaultAdapter.setVaultAccount(updatedVaultAccountIndex, updatedVaultAccount)
            }
        }
    }

    private fun onAccountAdapterItemViewContextMenuItemSelected(menuItem: MenuItem, position: Int, contextMenu: ContextMenu?, view: View?, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
        val selectedVaultAccount: Vault.Account = vaultAdapter.getStorageVaultAccount(position) ?: return

        when(menuItem.itemId) {
            R.id.accountViewerContextMenuItemCopyAccountName -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("accountName", selectedVaultAccount.name)
                )
            }

            R.id.accountViewerContextMenuItemEditAccountName -> {
                editAccountName(position)
            }

            R.id.accountViewerContextMenuItemRemoveAccount -> {
                vaultAdapter.getVaultAccountIndexFromDisplayVaultAccountIndex(position)?.also {
                    vaultAdapter.removeVaultAccount(it)
                }
            }
        }
    }

    private fun onSaveButtonClickListener(view: View) {
        activeVaultFileUri.let {
            if(it != null) { it } else {
                Toast.makeText(this, "No vault file was opened! Use Save As instead.", Toast.LENGTH_LONG).show()
                return
            }
        }

        if(vaultEncryptionEnabled) {
            val vaultCredentialsPromptView: View = LayoutInflater.from(this).inflate(
                R.layout.dialog_input_vault_password, null
            )

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setView(vaultCredentialsPromptView)

            alertDialogBuilder.apply {
                setCancelable(true)
                setTitle("Retype Your Password")

                setPositiveButton("Encrypt") { _, _ -> }

                setNeutralButton("Cancel")   { _, _ ->
                    Toast.makeText(this@VaultViewer, "Vault wasn't saved, password " +
                            "wasn't provided!", Toast.LENGTH_LONG).show()
                }
            }

            val alertDialog: AlertDialog = alertDialogBuilder.create()
            alertDialog.show()

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val etPassword: EditText = vaultCredentialsPromptView.findViewById(R.id.etPassword)

                if(kryptonCrypto.verifyPassword(etPassword.text.toString())) {
                    alertDialog.dismiss()
                    saveVault()
                } else {
                    Toast.makeText(this, "The password doesn't match the password " +
                            "used to load the vault!", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            saveVault()
        }
    }

    private fun unsavedChanges(): Boolean {
        val latestChanges: Vault = vaultAdapter.getVault()
        val savedChanges: Vault = vaultBackup

        var unsavedChanges = latestChanges.accounts.size != savedChanges.accounts.size

        if(!unsavedChanges) {
            for(index in 0 until latestChanges.accounts.size - 1) {
                if(!latestChanges.accounts[index].contentEquals(savedChanges.accounts[index])) {
                    unsavedChanges = true
                    break
                }
            }
        }

        return unsavedChanges
    }

    private fun diffVault() {
        val vaultDiffDialogView: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_vault_diff,
            null
        )

        val dialogVaultDiffTextViewEntriesAdded: TextView =
            vaultDiffDialogView.findViewById(R.id.dialogVaultDiffTextViewEntriesAdded)

        val dialogVaultDiffTextViewEntriesRemoved: TextView =
            vaultDiffDialogView.findViewById(R.id.dialogVaultDiffTextViewEntriesRemoved)

        val dialogVaultDiffTextViewEntriesChanged: TextView =
            vaultDiffDialogView.findViewById(R.id.dialogVaultDiffTextViewEntriesChanged)

        val alertDialogBuilder = AlertDialog.Builder(this).apply {
            setView(vaultDiffDialogView)
            setCancelable(true)
        }

        val entriesAdded: MutableList<String> = mutableListOf()
        val entriesRemoved: MutableList<String> = mutableListOf()
        val entriesChanged: MutableList<String> = mutableListOf()

        val alertDialog: AlertDialog = alertDialogBuilder.create()

        val newVaultAccounts: MutableList<Vault.Account> = vaultAdapter.getVault().accounts
        val oldVaultAccounts: MutableList<Vault.Account> = vaultBackup.accounts

        for(newAccount: Vault.Account in newVaultAccounts) {
            val oldAccount: Vault.Account? = oldVaultAccounts.find { vaultAccount ->
                vaultAccount.name == newAccount.name
            }

            if(oldAccount == null) {
                entriesAdded.add("+ %s".format(newAccount.name))

                for(entry: Map.Entry<String, String> in newAccount.entries) {
                    entriesAdded.add("+ %s/%s".format(newAccount.name, entry.key))
                }
            } else if(!newAccount.contentEquals(oldAccount)) {
                for(entry: Map.Entry<String, String> in newAccount.entries) {
                    if(!oldAccount.entries.containsKey(entry.key)) {
                        entriesAdded.add("+ %s/%s".format(newAccount.name, entry.key))
                    } else if(oldAccount.entries[entry.key] != entry.value) {
                        entriesChanged.add("~ %s/%s".format(newAccount.name, entry.key))
                    }
                }
            }
        }

        for(oldAccount: Vault.Account in oldVaultAccounts) {
            val newAccount: Vault.Account? = newVaultAccounts.find { vaultAccount ->
                vaultAccount.name == oldAccount.name
            }

            if(newAccount == null) {
                entriesRemoved.add("- %s".format(oldAccount.name))

                for(entry: Map.Entry<String, String> in oldAccount.entries) {
                    entriesRemoved.add("- %s/%s".format(oldAccount.name, entry.key))
                }
            } else if(!oldAccount.contentEquals(newAccount)) {
                for(entry: Map.Entry<String, String> in oldAccount.entries) {
                    if(!newAccount.entries.containsKey(entry.key)) {
                        entriesRemoved.add("- %s/%s".format(oldAccount.name, entry.key))
                    }
                }
            }
        }

        if(entriesAdded.isNotEmpty())
            dialogVaultDiffTextViewEntriesAdded.text = entriesAdded.joinToString("\n")

        if(entriesRemoved.isNotEmpty())
            dialogVaultDiffTextViewEntriesRemoved.text = entriesRemoved.joinToString("\n")

        if(entriesChanged.isNotEmpty())
            dialogVaultDiffTextViewEntriesChanged.text = entriesChanged.joinToString("\n")

        alertDialog.show()
    }

    private fun configureVaultSecurity() {
        val vaultSecurityDialogView: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_vault_security_configuration,
            null
        )

        val editTextSecurityDialogPassword: EditText =
            vaultSecurityDialogView.findViewById(R.id.editTextDialogVaultSecurityPassword)

        val editTextSecurityDialogConfirmPassword: EditText =
            vaultSecurityDialogView.findViewById(R.id.editTextDialogVaultSecurityConfirmPassword)

        val buttonSecurityDialogEnableEncryption: Button =
            vaultSecurityDialogView.findViewById(R.id.buttonDialogVaultSecurityEnableEncryption)

        val buttonSecurityDialogDisableEncryption: Button =
            vaultSecurityDialogView.findViewById(R.id.buttonDialogVaultSecurityDisableEncryption)

        val alertDialogBuilder = AlertDialog.Builder(this).apply {
            setView(vaultSecurityDialogView)
            setCancelable(true)
            setTitle("Configure Vault Security")
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()

        buttonSecurityDialogEnableEncryption.setOnClickListener {
            val password: String = editTextSecurityDialogPassword.text.toString()
            val confirmationPassword: String = editTextSecurityDialogConfirmPassword.text.toString()

            if(password.isEmpty() || confirmationPassword.isEmpty()) {
                Toast.makeText(this, "Supply a password first; populate " +
                        "both password fields.", Toast.LENGTH_LONG).show()

            } else if(!password.contentEquals(confirmationPassword)) {
                Toast.makeText(this, "The confirmation password doesn't " +
                        "match the original password!", Toast.LENGTH_LONG).show()

            } else {
                kryptonCrypto.setCryptoParameters(password)
                vaultEncryptionEnabled = true
                Toast.makeText(this, "Encryption key set successfully.", Toast.LENGTH_SHORT).show()
                alertDialog.dismiss()
            }
        }

        buttonSecurityDialogDisableEncryption.setOnClickListener {
            Toast.makeText(this, "Encryption has been disabled.", Toast.LENGTH_LONG).show()
            vaultEncryptionEnabled = false
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun addAccount(view: View) {
        val dialogNewAccountView: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_account_name,
            null
        )

        val etAccountName: EditText =
            dialogNewAccountView.findViewById<EditText>(R.id.dialogInputAccountNameEditTextAccountName)

        val alertDialogBuilder = AlertDialog.Builder(this).apply {
            setView(dialogNewAccountView)
            setCancelable(true)
            setTitle("Specify Account Name")

            setNeutralButton("Cancel") { _, _ -> }
            setPositiveButton("Add")   { _, _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val accountName: String = etAccountName.text.toString()

            if(accountName.isBlank()) {
                Toast.makeText(this, "Enter an account name first. Name cannot be blank.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(vaultAdapter.hasAccountWithName(accountName)) {
                Toast.makeText(this, "An account with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            vaultAdapter.addVaultAccount(Vault.Account(accountName))
            alertDialog.dismiss()
        }
    }

    private fun editAccountName(position: Int) {
        val dialogAccountNameInput: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_account_name,
            null
        )

        val vaultAccount: Vault.Account = vaultAdapter.getDisplayVaultAccount(position) ?: return

        val editTextAccountName: EditText =
            dialogAccountNameInput.findViewById(R.id.dialogInputAccountNameEditTextAccountName)

        editTextAccountName.setText(vaultAccount.name)

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setView(dialogAccountNameInput)
            setCancelable(true)
            setTitle("Enter New Account Name")

            setPositiveButton("Okay") { _, _ -> }
            setNegativeButton("Cancel") { _, _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newAccountName: String = editTextAccountName.text.toString()

            if(newAccountName.isBlank()) {
                Toast.makeText(this, "Enter an account name first. Name cannot be blank.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(vaultAdapter.hasAccountWithName(newAccountName)) {
                Toast.makeText(this, "An account with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val backBufferPosition: Int =
                vaultAdapter.getVaultAccountIndexFromDisplayVaultAccountIndex(position) ?: return@setOnClickListener

            vaultAdapter.setVaultAccount(backBufferPosition, vaultAccount.apply {
                name = newAccountName
            })

            alertDialog.dismiss()
        }
    }

//    private fun dumpVaultJson(): JSONObject {
//        return JSONObject().apply {
//            for(vaultAccount in vaultAdapter.storageVault.accounts) {
//                this.put(vaultAccount.name, JSONObject().apply {
//                    for(accountEntry in vaultAccount.entries) {
//                        put(accountEntry.key, accountEntry.value)
//                    }
//                })
//            }
//        }
//    }
//
//    private fun loadVaultJson(vaultDataJson: JSONObject): Boolean {
//        vaultAdapter.clearVault()
//
//        try {
//            for(accountName in vaultDataJson.keys()) {
//                val accountEntriesMap: MutableMap<String, String> = mutableMapOf()
//                val accountEntries: JSONObject = vaultDataJson.getJSONObject(accountName)
//
//                for(accountEntry: String in accountEntries.keys()) {
//                    accountEntriesMap[accountEntry] = accountEntries.getString(accountEntry)
//                }
//
//                vaultAdapter.addVaultAccount(Vault.Account(accountName, accountEntriesMap))
//            }
//
//            vaultBackup.clear()
//            vaultBackup.addAll(vaultAdapter.getVaultAccounts())
//        } catch(exception: JSONException) {
//            Toast.makeText(this, "Encountered exception when adding entries to viewer. " +
//                    "The JSON was loaded and parsed, but the structure might be wrong.", Toast.LENGTH_LONG).show()
//
//            vaultAdapter.clearVault()
//            return false
//        }
//
//        return true
//    }

    private fun saveVault(uri: Uri? = null) {
        val vaultFileUri: Uri = uri ?: activeVaultFileUri.let {
            if(it != null) { it } else {
                Toast.makeText(this, "No vault file was opened! Use Save As instead.", Toast.LENGTH_LONG).show()
                return
            }
        }

        saveVaultAs(vaultFileUri)
    }

    private fun saveVaultAs(uri: Uri? = null) {
        val vaultFileUri: Uri = uri.let {
            if(it != null) {
                it
            } else {
                Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    type = "*/*.vlt"

                    startActivityForResult(this, ActivityResultRequestCodes.AccountViewer.saveVaultAs)
                }

                return@saveVaultAs
            }
        }

        val vaultData: ByteArray = try {
            vaultAdapter.getVault().dumpToJsonObject()
        } catch(exception: Exception) {
            Toast.makeText(this, "Cannot save vault! Exception occurred during JSON serialization; " +
                    "the source data was probably invalid in some way.", Toast.LENGTH_LONG).show()

            return
        }.let { // Attempt to turn the JSONObject into a string.
            try {
                it.toString()
            } catch(exception: Exception) {
                Toast.makeText(this, "Cannot save vault! Exception occurred when attempting to turn " +
                        "the JSONObject into a string.", Toast.LENGTH_SHORT).show()

                return@saveVaultAs
            }
        }.toByteArray().let { // Attempt to encrypt the string's bytes if vault encryption is enabled.
            if (vaultEncryptionEnabled) {
                try {
                    kryptonCrypto.encrypt(it)
                } catch (exception: Exception) {
                    Toast.makeText(this, "Cannot save vault! Exception occurred during encryption. " +
                            "If the data is important, maybe try without encryption?", Toast.LENGTH_LONG).show()

                    return@saveVaultAs
                }
            } else {
                it
            }
        }

        contentResolver.openFileDescriptor(vaultFileUri, "wt")?.let { parcelFileDescriptor ->
            FileOutputStream(parcelFileDescriptor.fileDescriptor).use { fileOutputStream ->
                fileOutputStream.flush()
                fileOutputStream.write(vaultData)
                fileOutputStream.close()
            }
        }

        val vaultDataWritten: ByteArray = contentResolver.openFileDescriptor(vaultFileUri, "r")?.let { parcelFileDescriptor ->
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
                Toast.makeText(this, "Couldn't find TextView etIntegrityCheckRamHash" +
                        ", was null!", Toast.LENGTH_LONG).show()
                return
            }
        }

        val editTextIntegrityCheckDiskHash: TextView =
            dialogIntegrityCheckResultView.findViewById(R.id.etIntegrityCheckDiskHash)

        alertDialogBuilder.apply {
            setView(dialogIntegrityCheckResultView)

            etIntegrityCheckRamHash.text = vaultDataHashInRam.joinToString("") { byte ->
                "%02x".format(byte)
            }

            editTextIntegrityCheckDiskHash.text = vaultDataHashOnDisk.joinToString("") { byte ->
                "%02x".format(byte)
            }

            if(vaultDataHashInRam.contentEquals(vaultDataHashOnDisk)) {
                setTitle("Integrity Check Passed!")
                setMessage("The SHA-256 hash of the vault in RAM matches the hash of the vault on the disk.")

                etIntegrityCheckRamHash.setTextColor(Color.GREEN)
                editTextIntegrityCheckDiskHash.setTextColor(Color.GREEN)

                vaultBackup.accounts.clear()
                vaultBackup.accounts.addAll(vaultAdapter.getVault().accounts)
                activeVaultFileUri = vaultFileUri
            } else {
                setTitle("Integrity Check Failed!")

                setMessage("The SHA-256 hash of the vault in RAM does not match the hash of the " +
                        "vault on the disk! The data was not stored properly. The recommended action " +
                        "is to use Save As to save the vault to a new location instead.")

                etIntegrityCheckRamHash.setTextColor(Color.RED)
                editTextIntegrityCheckDiskHash.setTextColor(Color.RED)
            }
        }.create().show()
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

        val vaultDataJsonString: String = vaultFileDataBytes.toString(StandardCharsets.UTF_8)

        // Attempt to parse the vault data as if it were plaintext JSON.
        var vaultDataJsonObject: JSONObject? = try {
            JSONObject(vaultDataJsonString)
        } catch(exception: JSONException) {
            null
        }

        vaultDataJsonObject?.also { json ->
            try {
                vaultAdapter.setVault(Vault().apply {
                    loadFromJsonObject(json)
                })

                vaultBackup = vaultAdapter.getVault().clone()
                vaultEncryptionEnabled = true
            } catch(except: JSONException) {
                Toast.makeText(this, "There was a problem with the JSON structure! " +
                        "The JSON is valid, but a different structure was expected!", Toast.LENGTH_LONG).show()

                return@loadVaultFile
            }
        }

        // If that doesn't work, attempt to decrypt it as if it were AES encrypted.
        Toast.makeText(this, "Assuming the vault is encrypted.", Toast.LENGTH_LONG).show()

        val vaultCredentialsPromptView: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_vault_password, null
        )

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(vaultCredentialsPromptView)

        alertDialogBuilder.apply {
            setCancelable(false)
            setTitle("Provide Decryption Parameters")
            setPositiveButton("Decrypt") { _, _ -> }
            setNeutralButton("Cancel")   { _, _ -> this@VaultViewer.finish() }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val password: String =
                vaultCredentialsPromptView.findViewById<EditText>(R.id.etPassword).text.toString()

            val ivMaskLength: Int =
                vaultCredentialsPromptView.findViewById<EditText>(R.id.etIVMaskLength).text.toString().toIntOrNull()
                    ?: KryptonCrypto.CryptoConstants.aesBlockSize

            if(ivMaskLength < KryptonCrypto.CryptoConstants.aesBlockSize) {
                Toast.makeText(this,
                "The provided IV mask length is invalid! Must be greater than or equal to " +
                    "the AES block size: ${KryptonCrypto.CryptoConstants.aesBlockSize}",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            kryptonCrypto.setCryptoParameters(password, ivMaskLength)

            vaultDataJsonObject = try {
                JSONObject(String(kryptonCrypto.decrypt(vaultFileDataBytes)))
            } catch(exception: Exception) {
                null
            }

            vaultDataJsonObject?.also { json ->
                try {
                    val vault = Vault()
                    vault.loadFromJsonObject(json)

                    vaultAdapter.setVault(Vault().apply {
                        loadFromJsonObject(json)
                    })

                    vaultBackup = vaultAdapter.getVault().clone()
                    vaultEncryptionEnabled = true

                    alertDialog.dismiss()
                } catch(exception: JSONException) {
                    Toast.makeText(this, "There was a problem with the JSON structure! Decryption worked and " +
                            "the JSON is valid, but a different structure was expected!", Toast.LENGTH_LONG).show()
                }
            } ?: run {
                Toast.makeText(this, "Decryption failed!", Toast.LENGTH_LONG).show()
            }
        }
    }
}