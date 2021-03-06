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
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.psychedelicshayna.krypton.databinding.ActivityAccountBrowserLayoutBinding
import com.psychedelicshayna.krypton.databinding.DialogInputAccountNameBinding
import com.psychedelicshayna.krypton.databinding.DialogInputVaultPasswordBinding
import com.psychedelicshayna.krypton.databinding.DialogIntegrityCheckResultBinding
import com.psychedelicshayna.krypton.databinding.DialogVaultDiffBinding
import com.psychedelicshayna.krypton.databinding.DialogVaultSecurityConfigurationBinding
import kotlinx.android.synthetic.main.activity_account_browser_layout.*
import org.json.JSONException
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

class ActivityAccountBrowser : AppCompatActivity() {
    private lateinit var ui: ActivityAccountBrowserLayoutBinding

    private lateinit var clipboardManager: ClipboardManager

    private lateinit var vaultAdapter: VaultAccountAdapter
    private var vaultBackup: Vault = Vault()

    private val kryptonCrypto: KryptonCrypto = KryptonCrypto()
    private var vaultEncryptionEnabled: Boolean = false

    private var activeVaultFileUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ui = ActivityAccountBrowserLayoutBinding.inflate(layoutInflater)
        setContentView(ui.root)
        setSupportActionBar(ui.accountBrowserToolbar)

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        vaultAdapter = VaultAccountAdapter(this, Vault())

        vaultAdapter.onBindViewHolderListener = { holder, position ->
            holder.onContextMenuItemClickListener = { menuItem, position, contextMenu, view, contextMenuInfo ->
                onAccountAdapterItemViewContextMenuItemSelected(menuItem, position, contextMenu, view, contextMenuInfo)
            }

            holder.itemView.setOnClickListener {
                val clickedVaultAccount: Vault.Account? = vaultAdapter.getDisplayVaultAccount(position)

                val openEntryViewerIntent = Intent(
                    this,
                    ActivityEntryBrowser::class.java
                ).apply {
                    putExtra("VaultAccount", clickedVaultAccount)
                }

                startActivityForResult(openEntryViewerIntent, ActivityResultRequestCodes.EntryViewer.updateAccount)
            }
        }

        activeVaultFileUri = intent.getStringExtra("VaultFileUri")?.let { Uri.parse(it) }

        ui.rvAccounts.apply {
            layoutManager = LinearLayoutManager(this@ActivityAccountBrowser)
            adapter = vaultAdapter
        }

        ui.fabAddAccount.setOnClickListener { addAccount() }

        ui.svAccountSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(text: String?): Boolean {
                text?.run {
                    if (isEmpty()) vaultAdapter.clearAccountNameSearchQuery()
                    else vaultAdapter.performAccountNameSearchQuery(this)
                }

                return false
            }
        })

        if (activeVaultFileUri != null) loadVaultFile(activeVaultFileUri!!)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_account_browser_actions, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onBackPressed() {
        if (unsavedChanges()) {
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.item_save -> {
                saveVault()
            }

            R.id.item_save_as -> {
                saveVaultAs()
            }

            R.id.item_see_changes -> {
                diffVault()
            }

            R.id.item_revert_changes -> {
                vaultAdapter.setVault(vaultBackup)
            }

            R.id.item_config_encryption -> {
                configureVaultSecurity()
            }

            R.id.item_set_default -> {
                setDefaultVault()
            }
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.activity_account_browser_layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (resultCode == RESULT_OK && requestCode == ActivityResultRequestCodes.AccountViewer.saveVaultAs) {
            resultData?.data?.also { selectedFileUri ->
                contentResolver.takePersistableUriPermission(
                    selectedFileUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                saveVaultAs(selectedFileUri)
            }
        }

        if (resultCode == RESULT_OK && requestCode == ActivityResultRequestCodes.EntryViewer.updateAccount) {
            resultData?.run {
                val updatedVaultAccount: Vault.Account =
                    (getSerializableExtra("VaultAccount") as Vault.Account?) ?: run {
                        Toast.makeText(
                            this@ActivityAccountBrowser,
                            "Error! Received null as the " +
                                "updated vault account extra!",
                            Toast.LENGTH_LONG
                        ).show()

                        return@onActivityResult
                    }

                val updatedVaultAccountIndex: Int =
                    vaultAdapter.getVaultAccountIndexByName(updatedVaultAccount.name) ?: run {
                        Toast.makeText(
                            this@ActivityAccountBrowser,
                            "Error! The index of the updated " +
                                "account was null!",
                            Toast.LENGTH_LONG
                        ).show()

                        return@onActivityResult
                    }

                vaultAdapter.setVaultAccount(updatedVaultAccountIndex, updatedVaultAccount)
            }
        }
    }

    private fun onAccountAdapterItemViewContextMenuItemSelected(
        menuItem: MenuItem,
        position: Int,
        contextMenu: ContextMenu?,
        view: View?,
        contextMenuInfo: ContextMenu.ContextMenuInfo?
    ) {
        val selectedVaultAccount: Vault.Account = vaultAdapter.getStorageVaultAccount(position) ?: return

        when (menuItem.itemId) {
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

    private fun unsavedChanges(): Boolean {
        val latestChanges: Vault = vaultAdapter.getVault()
        val savedChanges: Vault = vaultBackup

        var unsavedChanges = latestChanges.accounts.size != savedChanges.accounts.size

        if (!unsavedChanges) {
            for (index in 0 until latestChanges.accounts.size) {
                if (!latestChanges.accounts[index].contentEquals(savedChanges.accounts[index])) {
                    unsavedChanges = true
                    break
                }
            }
        }

        return unsavedChanges
    }

    private fun diffVault() {
        val dialogVaultDiff: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_vault_diff,
            null
        )

        val uiDialogVaultDiff = DialogVaultDiffBinding.bind(dialogVaultDiff)

        val alertDialogBuilder = AlertDialog.Builder(this).apply {
            setView(dialogVaultDiff)
            setCancelable(true)
        }

        val entriesAdded: MutableList<String> = mutableListOf()
        val entriesRemoved: MutableList<String> = mutableListOf()
        val entriesChanged: MutableList<String> = mutableListOf()

        val alertDialog: AlertDialog = alertDialogBuilder.create()

        val newVaultAccounts: MutableList<Vault.Account> = vaultAdapter.getVault().accounts
        val oldVaultAccounts: MutableList<Vault.Account> = vaultBackup.accounts

        for (newAccount: Vault.Account in newVaultAccounts) {
            val oldAccount: Vault.Account? = oldVaultAccounts.find { vaultAccount ->
                vaultAccount.name == newAccount.name
            }

            if (oldAccount == null) {
                entriesAdded.add("+ %s".format(newAccount.name))

                for (entry: Map.Entry<String, String> in newAccount.entries) {
                    entriesAdded.add("+ %s/%s".format(newAccount.name, entry.key))
                }
            } else if (!newAccount.contentEquals(oldAccount)) {
                for (entry: Map.Entry<String, String> in newAccount.entries) {
                    if (!oldAccount.entries.containsKey(entry.key)) {
                        entriesAdded.add("+ %s/%s".format(newAccount.name, entry.key))
                    } else if (oldAccount.entries[entry.key] != entry.value) {
                        entriesChanged.add("~ %s/%s".format(newAccount.name, entry.key))
                    }
                }
            }
        }

        for (oldAccount: Vault.Account in oldVaultAccounts) {
            val newAccount: Vault.Account? = newVaultAccounts.find { vaultAccount ->
                vaultAccount.name == oldAccount.name
            }

            if (newAccount == null) {
                entriesRemoved.add("- %s".format(oldAccount.name))

                for (entry: Map.Entry<String, String> in oldAccount.entries) {
                    entriesRemoved.add("- %s/%s".format(oldAccount.name, entry.key))
                }
            } else if (!oldAccount.contentEquals(newAccount)) {
                for (entry: Map.Entry<String, String> in oldAccount.entries) {
                    if (!newAccount.entries.containsKey(entry.key)) {
                        entriesRemoved.add("- %s/%s".format(oldAccount.name, entry.key))
                    }
                }
            }
        }

        if (entriesAdded.isNotEmpty())
            uiDialogVaultDiff.tvDiffAdded.text = entriesAdded.joinToString("\n")

        if (entriesRemoved.isNotEmpty())
            uiDialogVaultDiff.tvDiffRemoved.text = entriesRemoved.joinToString("\n")

        if (entriesChanged.isNotEmpty())
            uiDialogVaultDiff.tvDiffChanged.text = entriesChanged.joinToString("\n")

        alertDialog.show()
    }

    private fun configureVaultSecurity() {
        val dialogVaultSecurityConfiguration: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_vault_security_configuration,
            null
        )

        val uiDialogVaultSecurityConfiguration =
            DialogVaultSecurityConfigurationBinding.bind(dialogVaultSecurityConfiguration)

        val alertDialog: AlertDialog = AlertDialog.Builder(this).run {
            setView(dialogVaultSecurityConfiguration)
            setCancelable(true)
            setTitle("Configure Vault Security")

            create().apply { show() }
        }

        uiDialogVaultSecurityConfiguration.btnEnableEncryption.setOnClickListener {
            val password: String =
                uiDialogVaultSecurityConfiguration.etNewPassword.text.toString()

            val confirmationPassword: String =
                uiDialogVaultSecurityConfiguration.etNewPasswordConfirm.text.toString()

            if (password.isEmpty() || confirmationPassword.isEmpty()) {
                Toast.makeText(
                    this,
                    "Supply a password first; populate both password fields.",
                    Toast.LENGTH_LONG
                ).show()
            } else if (!password.contentEquals(confirmationPassword)) {
                Toast.makeText(
                    this,
                    "The confirmation password doesn't match the original password!",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                kryptonCrypto.setCryptoParameters(password)
                vaultEncryptionEnabled = true
                Toast.makeText(
                    this,
                    "Encryption key set successfully.",
                    Toast.LENGTH_SHORT
                ).show()

                alertDialog.dismiss()
            }
        }

        uiDialogVaultSecurityConfiguration.btnDisableEncryption.setOnClickListener {
            Toast.makeText(this, "Encryption has been disabled.", Toast.LENGTH_LONG).show()
            vaultEncryptionEnabled = false
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun addAccount() {
        val dialogInputAccountName: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_account_name,
            null
        )

        val uiDialogInputAccountName = DialogInputAccountNameBinding.bind(dialogInputAccountName)

        val alertDialogBuilder = AlertDialog.Builder(this).apply {
            setView(dialogInputAccountName)
            setCancelable(true)
            setTitle("Specify Account Name")

            setNeutralButton("Cancel") { _, _ -> }
            setPositiveButton("Add") { _, _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val accountName: String = uiDialogInputAccountName.etAccountName.text.toString()

            if (accountName.isBlank()) {
                Toast.makeText(this, "Enter an account name first. Name cannot be blank.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (vaultAdapter.hasAccountWithName(accountName)) {
                Toast.makeText(this, "An account with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            vaultAdapter.addVaultAccount(Vault.Account(accountName))
            alertDialog.dismiss()
        }
    }

    private fun editAccountName(position: Int) {
        val dialogInputAccountName: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_account_name,
            null
        )

        val uiDialogInputAccountName = DialogInputAccountNameBinding.bind(dialogInputAccountName)

        val vaultAccount: Vault.Account = vaultAdapter.getDisplayVaultAccount(position) ?: return

        uiDialogInputAccountName.etAccountName.setText(vaultAccount.name)

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setView(dialogInputAccountName)
            setCancelable(true)
            setTitle("Enter New Account Name")

            setPositiveButton("Okay") { _, _ -> }
            setNegativeButton("Cancel") { _, _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val newAccountName: String = uiDialogInputAccountName.etAccountName.text.toString()

            if (newAccountName.isBlank()) {
                Toast.makeText(this, "Enter an account name first. Name cannot be blank.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (vaultAdapter.hasAccountWithName(newAccountName)) {
                Toast.makeText(this, "An account with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val backBufferPosition: Int =
                vaultAdapter.getVaultAccountIndexFromDisplayVaultAccountIndex(position) ?: return@setOnClickListener

            vaultAdapter.setVaultAccount(
                backBufferPosition,
                vaultAccount.apply {
                    name = newAccountName
                }
            )

            alertDialog.dismiss()
        }
    }

    private fun saveVault() {
        activeVaultFileUri ?: run {
            Toast.makeText(this, "No vault file was opened! Use Save As instead.", Toast.LENGTH_LONG).show()
            return
        }

        if (vaultEncryptionEnabled) {
            val dialogInputVaultPassword: View = LayoutInflater.from(this).inflate(
                R.layout.dialog_input_vault_password,
                null
            )

            val uiDialogInputVaultPassword = DialogInputVaultPasswordBinding.bind(dialogInputVaultPassword)

            val alertDialog: AlertDialog = AlertDialog.Builder(this).run {
                setView(dialogInputVaultPassword)

                setCancelable(true)
                setTitle("Retype Your Password")

                setPositiveButton("Encrypt") { _, _ -> }

                setNeutralButton("Cancel") { _, _ ->
                    Toast.makeText(
                        this@ActivityAccountBrowser,
                        "Vault wasn't saved, password wasn't provided!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                create().apply { show() }
            }

            alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (kryptonCrypto.verifyPassword(uiDialogInputVaultPassword.etPassword.text.toString())) {
                    alertDialog.dismiss()
                    saveVaultAs(activeVaultFileUri)
                } else {
                    Toast.makeText(
                        this,
                        "The password doesn't match the password " +
                            "used to load the vault!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            saveVaultAs(activeVaultFileUri)
        }
    }

    private fun saveVaultAs(uri: Uri? = null) {
        val vaultFileUri: Uri = uri ?: run {
            Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                flags = Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION

                type = "*/*"

                startActivityForResult(this, ActivityResultRequestCodes.AccountViewer.saveVaultAs)
            }

            return@saveVaultAs
        }

        contentResolver.persistedUriPermissions.find { uriPermission ->
            uriPermission.uri == vaultFileUri
        } ?: run {
            contentResolver.takePersistableUriPermission(
                vaultFileUri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }

        val vaultData: ByteArray =
            vaultAdapter.getVault().dumpToJsonObject().toString().toByteArray().run {
                if (vaultEncryptionEnabled) kryptonCrypto.encrypt(this)
                else this
            }

        contentResolver.openOutputStream(vaultFileUri, "wt")?.use { outputStream ->
            outputStream.write(vaultData)
        }

        val integrityChecker = object {
            private fun attemptReadingWrittenData() {
                contentResolver.openInputStream(vaultFileUri)?.use { inputStream ->
                    inputStream.readBytes()
                }?.also { data ->
                    performComparison(data)
                } ?: run {
                    val alertDialog: AlertDialog = AlertDialog.Builder(this@ActivityAccountBrowser).run {
                        setCancelable(true)
                        setTitle("Read Failure")

                        setMessage(
                            "Tried to re-read the written vault file to calculate its hash, but an" +
                                "input stream to the file could not be opened."
                        )

                        setPositiveButton("Retry") { _, _ -> }
                        setNegativeButton("Cancel") { _, _ -> }

                        create().apply { show() }
                    }

                    alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                        alertDialog.dismiss()
                        attemptReadingWrittenData()
                    }
                }
            }

            private fun performComparison(dataReadFromDisk: ByteArray) {
                val vaultDataHashInRam: ByteArray = MessageDigest.getInstance("SHA-256").digest(vaultData)
                val vaultDataHashOnDisk: ByteArray = MessageDigest.getInstance("SHA-256").digest(dataReadFromDisk)

                val dialogIntegrityCheckResultView: View = LayoutInflater.from(this@ActivityAccountBrowser).inflate(
                    R.layout.dialog_integrity_check_result,
                    null,
                    false
                )

                val uiIntegrityCheckResult = DialogIntegrityCheckResultBinding.bind(dialogIntegrityCheckResultView)

                val editTextIntegrityCheckRamHash: TextView =
                    uiIntegrityCheckResult.etRamHash

                val editTextIntegrityCheckDiskHash: TextView =
                    uiIntegrityCheckResult.etDiskHash

                editTextIntegrityCheckRamHash.text =
                    vaultDataHashInRam.joinToString("") { byte -> "%02x".format(byte) }

                editTextIntegrityCheckDiskHash.text =
                    vaultDataHashOnDisk.joinToString("") { byte -> "%02x".format(byte) }

                val alertDialog: AlertDialog = AlertDialog.Builder(this@ActivityAccountBrowser).run {
                    setView(dialogIntegrityCheckResultView)

                    if (vaultDataHashInRam.contentEquals(vaultDataHashOnDisk)) {
                        setTitle("Integrity Check Passed!")
                        setMessage("The SHA-256 hash of the vault in RAM matches the hash of the vault on the disk.")

                        setPositiveButton("Okay") { _, _ -> }

                        editTextIntegrityCheckRamHash.setTextColor(Color.GREEN)
                        editTextIntegrityCheckDiskHash.setTextColor(Color.GREEN)

                        vaultBackup.accounts.clear()
                        vaultBackup.accounts.addAll(vaultAdapter.getVault().accounts)

                        activeVaultFileUri = vaultFileUri
                    } else {
                        setTitle("Integrity Check Failed!")

                        setMessage(
                            "The SHA-256 hash of the vault in RAM does not match the hash of the " +
                                "vault on the disk! The data was not stored properly. The recommended action " +
                                "is to use Save As to save the vault to a new location instead."
                        )

                        setPositiveButton("Retry") { _, _ -> }
                        setNegativeButton("Cancel") { _, _ -> }

                        editTextIntegrityCheckRamHash.setTextColor(Color.RED)
                        editTextIntegrityCheckDiskHash.setTextColor(Color.RED)
                    }

                    create().apply { show() }
                }

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                    alertDialog.dismiss()

                    if (alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).text == "Retry") {
                        attemptReadingWrittenData()
                    }
                }
            }

            fun attemptIntegrityCheck() = attemptReadingWrittenData()
        }

        integrityChecker.attemptIntegrityCheck()
    }

    private fun loadVaultFile(vaultFileUri: Uri) {
        val fileBytes: ByteArray =
            contentResolver.openInputStream(vaultFileUri)?.use { inputStream ->
                inputStream.readBytes()
            } ?: run {
                Toast.makeText(
                    this,
                    "Tried to load vault file, but an input stream to the file could not be opened!",
                    Toast.LENGTH_LONG
                ).show()

                return@loadVaultFile
            }

        // Attempt to parse the file's bytes as if it were plaintext JSON.
        val fileContentsJson: JSONObject? = try {
            JSONObject(fileBytes.toString(StandardCharsets.UTF_8))
        } catch (exception: JSONException) {
            null
        }

        // If it worked, attempt to load the JSONObject vault data.
        fileContentsJson?.also { json ->
            try {
                vaultAdapter.setVault(Vault().apply { loadFromJsonObject(json) })

                vaultBackup = vaultAdapter.getVault().clone()
                vaultEncryptionEnabled = false

                return@loadVaultFile
            } catch (except: JSONException) {
                Toast.makeText(
                    this,
                    "There was a problem with the JSON structure! The JSON is valid, but a " +
                        "different structure was expected!",
                    Toast.LENGTH_LONG
                ).show()

                return@loadVaultFile
            }
        }

        // If it didn't work, attempt to decrypt the bytes before trying again.
        Toast.makeText(this, "Assuming the vault is encrypted.", Toast.LENGTH_LONG).show()

        val dialogInputVaultPassword: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_vault_password,
            null
        )

        val uiDialogInputVaultPassword = DialogInputVaultPasswordBinding.bind(dialogInputVaultPassword)

        val alertDialog: AlertDialog = AlertDialog.Builder(this).run {
            setCancelable(false)
            setTitle("Provide Decryption Parameters")
            setView(dialogInputVaultPassword)

            setPositiveButton("Decrypt") { _, _ -> }
            setNeutralButton("Cancel") { _, _ -> this@ActivityAccountBrowser.finish() }

            create().apply { show() }
        }

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val password: String =
                uiDialogInputVaultPassword.etPassword.text.toString()

            val ivMaskLength: Int =
                uiDialogInputVaultPassword.etIvMaskLength.text.toString().toIntOrNull()
                    ?: KryptonCrypto.CryptoConstants.aesBlockSize

            if (ivMaskLength < KryptonCrypto.CryptoConstants.aesBlockSize) {
                Toast.makeText(
                    this,
                    "The provided IV mask length is invalid! Must be greater than or equal to " +
                        "the AES block size: ${KryptonCrypto.CryptoConstants.aesBlockSize}",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            kryptonCrypto.setCryptoParameters(password, ivMaskLength)

            val decryptedFileContentsJson: JSONObject? = try {
                JSONObject(String(kryptonCrypto.decrypt(fileBytes)))
            } catch (exception: JSONException) {
                null
            }

            decryptedFileContentsJson?.also { json ->
                try {
                    vaultAdapter.setVault(
                        Vault().apply { loadFromJsonObject(json) }
                    )

                    vaultBackup = vaultAdapter.getVault().clone()
                    vaultEncryptionEnabled = true

                    alertDialog.dismiss()
                } catch (exception: JSONException) {
                    Toast.makeText(
                        this,
                        "There was a problem with the JSON structure! Decryption worked and " +
                            "the JSON is valid, but a different structure was expected!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } ?: run {
                Toast.makeText(this, "Decryption failed!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setDefaultVault() {
        if (activeVaultFileUri == null) {
            Toast.makeText(
                this,
                "No active vault file! Save the vault first, or load the vault you would like to set.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        getSharedPreferences("KryptonPreferences", MODE_PRIVATE).edit().run {
            putString("DefaultVaultUri", activeVaultFileUri.toString())
            apply()
        }

        Toast.makeText(this, "The default vault has been set.", Toast.LENGTH_LONG).show()
    }
}
