package com.psychedelicshayna.krypton

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.psychedelicshayna.krypton.databinding.ActivityEntryBrowserLayoutBinding
import com.psychedelicshayna.krypton.databinding.DialogInputAccountEntryValuesBinding
import com.psychedelicshayna.krypton.databinding.DialogVaultAccountEntryPasswordGeneratorBinding
import kotlinx.android.synthetic.main.activity_entry_browser_layout.*

class ActivityEntryBrowser : AppCompatActivity() {
    private lateinit var uiActivityEntryBrowser: ActivityEntryBrowserLayoutBinding

    private lateinit var vaultAccountEntryAdapter: VaultAccountEntryAdapter
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var viewingVaultAccount: Vault.Account

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        uiActivityEntryBrowser = ActivityEntryBrowserLayoutBinding.inflate(layoutInflater)
        setContentView(uiActivityEntryBrowser.root)

        viewingVaultAccount =
            (intent.getSerializableExtra("VaultAccount") as Vault.Account?) ?: run {
                Toast.makeText(
                    this,
                    "Error! Vault.AccountEntryViewer received no \"VaultAccount\" extra!",
                    Toast.LENGTH_LONG
                ).show()

                finish()

                return@onCreate
            }

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        vaultAccountEntryAdapter = VaultAccountEntryAdapter(this, viewingVaultAccount.entries)

        vaultAccountEntryAdapter.onBindViewHolderListener = { holder, _ ->
            holder.onContextMenuItemClickListener =
                { menuItem, position, contextMenu, view, contextMenuInfo ->
                    onEntryAdapterItemViewContextMenuItemSelected(
                        menuItem,
                        position,
                        contextMenu,
                        view,
                        contextMenuInfo
                    )
                }
        }

        rv_entries.layoutManager = LinearLayoutManager(this)
        rv_entries.adapter = vaultAccountEntryAdapter

        tv_account_name_header.text = viewingVaultAccount.name

        uiActivityEntryBrowser.btnAddEntry.setOnClickListener { addEntry() }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.activity_entry_browser_layout)
    }

    private fun onEntryAdapterItemViewContextMenuItemSelected(
        menuItem: MenuItem,
        position: Int,
        contextMenu: ContextMenu?,
        view: View?,
        contextMenuInfo: ContextMenu.ContextMenuInfo?
    ) {
        val selectedEntryPair: Pair<String, String> =
            vaultAccountEntryAdapter[position] ?: run {
                Toast.makeText(
                    this@ActivityEntryBrowser,
                    "The index of the selected item was out of range!",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

        when (menuItem.itemId) {
            R.id.menuEntryViewerEntryContextMenuItemCopyEntryName -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("entryName", selectedEntryPair.first)
                )
            }

            R.id.menuEntryViewerEntryContextMenuItemCopyEntryValue -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("entryValue", selectedEntryPair.second)
                )
            }

            R.id.menuEntryViewerEntryContextMenuItemEdit -> {
                editEntry(position)
            }

            R.id.menuEntryViewerEntryContextMenuItemRemove -> {
                vaultAccountEntryAdapter.removeAccountEntry(position)

                setResult(
                    RESULT_OK,
                    Intent().apply {
                        putExtra(
                            "VaultAccount",
                            viewingVaultAccount.apply {
                                entries = vaultAccountEntryAdapter.accountEntryPairs.toMap().toMutableMap()
                            }
                        )
                    }
                )
            }
        }
    }

    private fun showPasswordGeneratorDialog(
        insertToNameCallback: (AlertDialog, String) -> Unit,
        insertToValueCallback: (AlertDialog, String) -> Unit
    ) {
        val dialogEntriesPasswordGenerator: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_vault_account_entry_password_generator,
            null
        )

        val uiDialogEntriesPasswordGenerator =
            DialogVaultAccountEntryPasswordGeneratorBinding.bind(dialogEntriesPasswordGenerator)

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setView(dialogEntriesPasswordGenerator)
            setCancelable(true)
            setTitle("Password Generator")
            setNegativeButton("Cancel") { _, _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        dialogEntriesPasswordGenerator.run {
            uiDialogEntriesPasswordGenerator.btnPgenGenerate.setOnClickListener {
                val passwordGenerator = PasswordGenerator()

                val characterClassSpec: PasswordGenerator.CharacterClassSpec =
                    PasswordGenerator.CharacterClassSpec().apply {
                        uppercase.enabled =
                            uiDialogEntriesPasswordGenerator.cbPgenUpper.isChecked

                        lowercase.enabled =
                            uiDialogEntriesPasswordGenerator.cbPgenLower.isChecked

                        numerical.enabled =
                            uiDialogEntriesPasswordGenerator.cbPgenNumeric.isChecked

                        special.enabled =
                            uiDialogEntriesPasswordGenerator.cbPgenSpecial.isChecked

                        extra.enabled =
                            uiDialogEntriesPasswordGenerator.cbPgenExtra.isChecked
                    }

                val passwordLength: Int =
                    uiDialogEntriesPasswordGenerator.etPasswordLength.run {
                        if (length() > 0) text.toString().toInt()
                        else 0
                    }

                val performGenerate = {
                    if (passwordLength > 0) {
                        val generatedPassword: String =
                            passwordGenerator.generatePassword(passwordLength, characterClassSpec)

                        uiDialogEntriesPasswordGenerator.etPgenOutput.setText(generatedPassword)
                    }

                    uiDialogEntriesPasswordGenerator.btnPgenInsertName.setOnClickListener {
                        uiDialogEntriesPasswordGenerator.etPgenOutput.run {
                            insertToNameCallback.invoke(alertDialog, text.toString())
                        }
                    }

                    uiDialogEntriesPasswordGenerator.btnPgenInsertValue.setOnClickListener {
                        uiDialogEntriesPasswordGenerator.etPgenOutput.run {
                            insertToValueCallback.invoke(alertDialog, text.toString())
                        }
                    }
                }

                if (passwordLength > 10000) {
                    AlertDialog.Builder(this@ActivityEntryBrowser).apply {
                        setCancelable(true)
                        setTitle("Length Over 10,000")
                        setMessage(
                            "You're about to generate a password that's " +
                                "$passwordLength characters long! Are you sure?"
                        )

                        setPositiveButton("Yes") { _, _ -> performGenerate() }
                        setNegativeButton("Cancel") { _, _ -> }
                    }.create().show()
                } else {
                    performGenerate()
                }
            }
        }
    }

    private fun editEntry(position: Int) {
        val entry: Pair<String, String> =
            vaultAccountEntryAdapter[position] ?: run {
                Toast.makeText(
                    this,
                    "Error! Cannot access the entry at index $position, index out of range!",
                    Toast.LENGTH_LONG
                ).show()

                return
            }

        val dialogEntryValuesInput: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_account_entry_values,
            null
        )

        val uiDialogInputAccountEntryValues =
            DialogInputAccountEntryValuesBinding.bind(dialogEntryValuesInput)

        uiDialogInputAccountEntryValues.etEntryName.setText(entry.first)
        uiDialogInputAccountEntryValues.etEntryValue.setText(entry.second)

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setView(dialogEntryValuesInput)
            setCancelable(true)

            setTitle("Enter New Entry Values")

            setPositiveButton("Okay") { _, _ -> }
            setNegativeButton("Cancel") { _, _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val entryName: String = uiDialogInputAccountEntryValues.etEntryName.text.toString()
            val entryValue: String = uiDialogInputAccountEntryValues.etEntryValue.text.toString()

            if (entryName.isBlank()) {
                Toast.makeText(
                    this,
                    "Enter an entry name first. Name cannot be blank.",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            if (vaultAccountEntryAdapter.hasEntryWithName(entryName) && entryName != entry.first) {
                Toast.makeText(
                    this,
                    "An entry with that name already exists!",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            vaultAccountEntryAdapter.setAccountEntry(position, Pair(entryName, entryValue))

            setResult(
                RESULT_OK,
                Intent().apply {
                    putExtra(
                        "VaultAccount",
                        viewingVaultAccount.apply {
                            entries = vaultAccountEntryAdapter.accountEntryPairs.toMap().toMutableMap()
                        }
                    )
                }
            )

            alertDialog.dismiss()
        }

        uiDialogInputAccountEntryValues.btnPasswordGenerator.setOnClickListener {
            showPasswordGeneratorDialog(
                { alertDialog, generatedPassword ->
                    uiDialogInputAccountEntryValues.etEntryName.setText(generatedPassword)
                    alertDialog.dismiss()
                },

                { alertDialog, generatedPassword ->
                    uiDialogInputAccountEntryValues.etEntryValue.setText(generatedPassword)
                    alertDialog.dismiss()
                }
            )
        }
    }

    private fun addEntry() {
        val dialogInputAccountEntryValues: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_account_entry_values,
            null
        )

        val uiDialogInputAccountEntryValues =
            DialogInputAccountEntryValuesBinding.bind(dialogInputAccountEntryValues)

        val alertDialogBuilder: AlertDialog.Builder =
            AlertDialog.Builder(this).apply {
                setView(dialogInputAccountEntryValues)
                setCancelable(true)

                setTitle("Enter Entry Name")

                setPositiveButton("Add") { _, _ -> }
                setNegativeButton("Cancel") { _, _ -> }
            }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val entryName: String = uiDialogInputAccountEntryValues.etEntryName.text.toString()
            val entryValue: String = uiDialogInputAccountEntryValues.etEntryValue.text.toString()

            if (entryName.isBlank()) {
                Toast.makeText(
                    this,
                    "Enter an entry name first. Name cannot be blank.",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            if (vaultAccountEntryAdapter.hasEntryWithName(entryName)) {
                Toast.makeText(
                    this,
                    "An entry with that name already exists!",
                    Toast.LENGTH_LONG
                ).show()

                return@setOnClickListener
            }

            vaultAccountEntryAdapter.addAccountEntry(entryName, entryValue)

            setResult(
                RESULT_OK,
                Intent().apply {
                    putExtra(
                        "VaultAccount",
                        viewingVaultAccount.apply {
                            entries = vaultAccountEntryAdapter.accountEntryPairs.toMap().toMutableMap()
                        }
                    )
                }
            )

            alertDialog.dismiss()
        }

        uiDialogInputAccountEntryValues.btnPasswordGenerator.setOnClickListener {
            showPasswordGeneratorDialog(
                { alertDialog, generatedPassword ->
                    uiDialogInputAccountEntryValues.etEntryName.setText(generatedPassword)
                    alertDialog.dismiss()
                },

                { alertDialog, generatedPassword ->
                    uiDialogInputAccountEntryValues.etEntryValue.setText(generatedPassword)
                    alertDialog.dismiss()
                }
            )
        }
    }
}
