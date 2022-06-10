package com.psychedelicshayna.krypton

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_account_viewer.*
import org.json.*
import java.nio.charset.StandardCharsets
import org.apache.commons.io.IOUtils
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.lang.Exception
import java.security.MessageDigest


class AccountViewer : AppCompatActivity() {
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var vaultAccountAdapter: VaultAccountAdapter

    private val backupOfVaultAccounts: MutableList<VaultAccount> = mutableListOf()

    private val vaultSecurity: VaultSecurity = VaultSecurity()
    private var vaultEncryptionEnabled: Boolean = false

    private var loadedVaultFileUri: Uri? = null

    private var latestContextMenuItemPressed: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_viewer)

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        vaultAccountAdapter = VaultAccountAdapter(mutableListOf())

        vaultAccountAdapter.vaultAccountAdapterListener = object : VaultAccountAdapter.VaultAccountAdapterListener() {
            override fun onBindViewHolderListener(holder: VaultAccountAdapter.AccountItemViewHolder, position: Int) {
                super.onBindViewHolderListener(holder, position)
                registerForContextMenu(holder.itemView)

                holder.itemView.setOnClickListener { itemView: View ->
                    vaultAccountAdapter.itemAtFrontBuffer(position)?.also { vaultAccount ->
                        Intent(this@AccountViewer, EntryViewer::class.java).also { intent ->
                            intent.putExtra("VaultAccountObject", vaultAccount)
                            startActivity(intent)
                        }
                    }
                }
            }
        }

        loadedVaultFileUri = intent.getStringExtra("VaultFileUri")?.let { Uri.parse(it) }

        findViewById<RecyclerView>(R.id.activityAccountViewerRecyclerViewVaultAccounts).apply {
            layoutManager = LinearLayoutManager(this@AccountViewer)
            adapter = vaultAccountAdapter
        }

        activityAccountViewerButtonAddAccount.setOnClickListener { addAccount() }

        activityAccountViewerButtonSave.setOnClickListener {
            loadedVaultFileUri.let {
                if(it != null) { it } else {
                    Toast.makeText(this, "No vault file was opened! Use Save As instead.", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }
            }

            if(vaultEncryptionEnabled) {
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
                        Toast.makeText(this@AccountViewer, "Vault wasn't saved, password wasn't provided!", Toast.LENGTH_LONG).show()
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

        activityAccountViewerButtonSaveAs.setOnClickListener        { saveVaultAs() }

        activityAccountViewerButtonRevertChanges.setOnClickListener {
            vaultAccountAdapter.setVaultAccounts(backupOfVaultAccounts.toTypedArray())
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
                    vaultAccountAdapter.clearAccountNameFilter()
                } else {
                    vaultAccountAdapter.setAccountNameFilter(text)
                }

                return true
            }
        })

        if(loadedVaultFileUri != null) loadVaultFile(loadedVaultFileUri!!)
    }

    override fun onCreateContextMenu(menu: ContextMenu?, view: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, view, menuInfo)
        menuInflater.inflate(R.menu.menu_account_viewer_account_context_menu, menu)
        latestContextMenuItemPressed = view
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val itemPressedView: View = latestContextMenuItemPressed.let {
            if(it != null) {
                it
            } else {
                Toast.makeText(this, "View was null", Toast.LENGTH_SHORT).show()
                return true
            }
        }

        val textViewAccountName: TextView =
            itemPressedView.findViewById(R.id.tvVaultAccountName)

        when(item.itemId) {
            R.id.menuItemCopyAccountName -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("entryName", textViewAccountName.text)
                )

                return true
            }
        }

        return super.onContextItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if(resultCode == RESULT_OK && requestCode == ActivityResultRequestCodes.AccountViewer.saveVaultAs) {
            resultData?.data?.also {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
                saveVaultAs(it)
            }
        }
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

        val newVaultAccounts: Array<VaultAccount> = vaultAccountAdapter.getVaultAccounts()
        val oldVaultAccounts: Array<VaultAccount> = backupOfVaultAccounts.toTypedArray()

        for (newAccount: VaultAccount in newVaultAccounts) {
            val oldAccount: VaultAccount? = oldVaultAccounts.find { vaultAccount ->
                vaultAccount.contentEquals(newAccount)
            }

            if(oldAccount == null) {
                entriesAdded.add("+ %s".format(newAccount.AccountName))

                for(entry: Map.Entry<String, String> in newAccount.AccountEntries) {
                    entriesAdded.add("+ %s/%s".format(newAccount.AccountName, entry.key))
                }
            } else {
                for(entry: Map.Entry<String, String> in newAccount.AccountEntries) {
                    if(!oldAccount.AccountEntries.containsKey(entry.key)) {
                        entriesAdded.add("+ %s/%s".format(newAccount.AccountName, entry.key))
                    } else if(oldAccount.AccountEntries[entry.key] != entry.value) {
                        entriesChanged.add("~ %s/%s".format(newAccount.AccountName, entry.key))
                    }
                }
            }
        }

        for (oldAccount: VaultAccount in oldVaultAccounts) {
            val newAccount: VaultAccount? = newVaultAccounts.find { vaultAccount ->
                vaultAccount.contentEquals(oldAccount)
            }

            if(newAccount == null) {
                entriesRemoved.add("- %s".format(oldAccount.AccountName))

                for(entry: Map.Entry<String, String> in oldAccount.AccountEntries) {
                    entriesRemoved.add("- %s/%s".format(oldAccount.AccountName, entry.key))
                }
            } else {
                for(entry: Map.Entry<String, String> in oldAccount.AccountEntries) {
                    if(!newAccount.AccountEntries.containsKey(entry.key)) {
                        entriesRemoved.add("- %s/%s".format(oldAccount.AccountName, entry.key))
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
            R.layout.dialog_vault_security,
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
                Toast.makeText(this, "Supply a password first; populate both password fields.", Toast.LENGTH_LONG).show()
            } else if(!password.contentEquals(confirmationPassword)) {
                Toast.makeText(this, "The confirmation password doesn't match the original password!", Toast.LENGTH_LONG).show()
            } else {
                vaultSecurity.setCryptoParameters(password)
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

    private fun addAccount() {
        val dialogNewAccountView: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_new_account,
            null
        )

        val etAccountName: EditText =
            dialogNewAccountView.findViewById<EditText>(R.id.dialogNewAccountEditTextAccountName)

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

            if(vaultAccountAdapter.hasAccountWithName(accountName)) {
                Toast.makeText(this, "An account with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            vaultAccountAdapter.addVaultAccount(VaultAccount(accountName))
            alertDialog.dismiss()
        }
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

            backupOfVaultAccounts.clear()
            backupOfVaultAccounts.addAll(vaultAccountAdapter.getVaultAccounts())
        } catch(exception: JSONException) {
            Toast.makeText(this, "Encountered exception when adding entries to viewer. " +
                    "The JSON was loaded and parsed, but the structure might be wrong.", Toast.LENGTH_LONG).show()

            vaultAccountAdapter.clearVaultAccounts()
            return false
        }

        return true
    }

    private fun saveVault(uri: Uri? = null) {
        val vaultFileUri: Uri = uri ?: loadedVaultFileUri.let {
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
            dumpVaultJson()
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
                    vaultSecurity.encryptVault(it)
                } catch (exception: Exception) {
                    Toast.makeText(this, "Cannot save vault! Exception occurred during encryption. " +
                            "If the data is important, maybe try without encryption?", Toast.LENGTH_LONG).show()

                    return@saveVaultAs
                }
            } else {
                it
            }
        }

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

                backupOfVaultAccounts.clear()
                backupOfVaultAccounts.addAll(vaultAccountAdapter.getVaultAccounts())
            } else {
                setTitle("Integrity Check Failed!")
                setMessage("The SHA-256 hash of the vault in RAM does not match the hash of the vault on the disk! The data was not stored properly. " +
                        "The recommended action is to use Save As to save the vault to a new location instead.")

                etIntegrityCheckRamHash.setTextColor(Color.RED)
                etIntegrityCheckDiskHash.setTextColor(Color.RED)
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
                vaultEncryptionEnabled = false
            }

            return
        }

        // If that doesn't work, attempt to decrypt it as if it were AES encrypted.
        Toast.makeText(this, "Assuming the vault is encrypted.", Toast.LENGTH_LONG).show()

        val vaultCredentialsPromptView: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_vault_credentials, null
        )

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(vaultCredentialsPromptView)

        alertDialogBuilder.apply {
            setCancelable(false)
            setTitle("Provide Decryption Parameters")
            setPositiveButton("Decrypt") { _, _ -> }
            setNeutralButton("Cancel")   { _, _ -> this@AccountViewer.finish() }
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
            } catch(exception: Exception) {
                null
            }

            if(vaultFileDataJson != null) {
                if(loadVaultJson(vaultFileDataJson as JSONObject)) {
                    vaultEncryptionEnabled = true
                    alertDialog.dismiss()
                    return@setOnClickListener
                } else {
                    Toast.makeText(this, "There was a problem with the JSON structure! Decryption worked and " +
                            "the JSON is valid, but a different structure was expected!", Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(this, "Decryption failed!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            this@AccountViewer.finish()
        }
    }

}