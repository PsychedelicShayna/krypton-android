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
import kotlinx.android.synthetic.main.activity_vault_account_entry_viewer.*

class VaultAccountEntryViewer : AppCompatActivity() {
    private lateinit var vaultAccountEntryAdapter: VaultAccountEntryAdapter
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var viewingVaultAccount: Vault.Account

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault_account_entry_viewer)

        viewingVaultAccount = (intent.getSerializableExtra("VaultAccount") as Vault.Account?) ?: run {

            Toast.makeText(this, "Error! Vault.AccountEntryViewer received no" +
                    " \"VaultAccount\" extra!", Toast.LENGTH_LONG).show()

            finish()

            return@onCreate
        }

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        vaultAccountEntryAdapter = VaultAccountEntryAdapter(this, viewingVaultAccount.entries)

        vaultAccountEntryAdapter.onBindViewHolderListener = { holder, _ ->
            holder.onContextMenuItemClickListener = { menuItem, position, contextMenu, view, contextMenuInfo ->
                onEntryAdapterItemViewContextMenuItemSelected(menuItem, position, contextMenu, view, contextMenuInfo)
            }
        }

        activityEntryViewerRecyclerViewEntries.layoutManager = LinearLayoutManager(this)
        activityEntryViewerRecyclerViewEntries.adapter = vaultAccountEntryAdapter

        activityEntryViewerTextViewAccountName.text = viewingVaultAccount.name

        findViewById<Button>(R.id.activityEntryViewerButtonAddEntry).setOnClickListener(::addEntry)
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        setContentView(R.layout.activity_vault_account_entry_viewer)
    }

    private fun onEntryAdapterItemViewContextMenuItemSelected(menuItem: MenuItem, position: Int, contextMenu: ContextMenu?, view: View?, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
        val selectedEntryPair: Pair<String, String> = vaultAccountEntryAdapter[position] ?: run {
            Toast.makeText(this@VaultAccountEntryViewer, "The index of the selected item was out of range!", Toast.LENGTH_LONG).show()
            return
        }

        when(menuItem.itemId) {
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

                setResult(RESULT_OK, Intent().apply {
                    putExtra("VaultAccount", viewingVaultAccount.apply {
                        entries = vaultAccountEntryAdapter.accountEntryPairs.associate { it }.toMutableMap()
                    })
                })
            }
        }
    }

    private fun showPasswordGeneratorDialog(insertToNameCallback: (AlertDialog, String) -> Unit, insertToValueCallback: (AlertDialog, String) -> Unit) {
        val dialogEntriesPasswordGenerator: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_vault_account_entry_password_generator,
            null
        )

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setView(dialogEntriesPasswordGenerator)
            setCancelable(true)
            setTitle("Password Generator")
            setNegativeButton("Cancel") { _, _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        dialogEntriesPasswordGenerator.run {
            findViewById<Button>(R.id.dialogEntriesPasswordGeneratorButtonGenerate)?.setOnClickListener {
                val passwordGenerator = PasswordGenerator()

                val characterClassSpec: PasswordGenerator.CharacterClassSpec = PasswordGenerator.CharacterClassSpec().apply {
                    uppercase.enabled = findViewById<CheckBox>(R.id.dialogEntriesPasswordGeneratorCheckBoxUpper)   ?.isChecked ?: false
                    lowercase.enabled = findViewById<CheckBox>(R.id.dialogEntriesPasswordGeneratorCheckBoxLower)   ?.isChecked ?: false
                    numerical.enabled = findViewById<CheckBox>(R.id.dialogEntriesPasswordGeneratorCheckBoxNumeric) ?.isChecked ?: false
                    special.enabled   = findViewById<CheckBox>(R.id.dialogEntriesPasswordGeneratorCheckBoxSpecial) ?.isChecked ?: false
                    extra.enabled     = findViewById<CheckBox>(R.id.dialogEntriesPasswordGeneratorCheckBoxExtra)   ?.isChecked ?: false
                }

                val passwordLength: Int = findViewById<EditText>(R.id.dialogEntriesPasswordGeneratorEditTextLength)?.run {
                    if(length() > 0) text.toString().toInt()
                    else 0
                } ?: 0

                if(passwordLength > 0) {
                    val generatedPassword: String = passwordGenerator.generatePassword(passwordLength, characterClassSpec)
                    findViewById<EditText>(R.id.dialogEntriesPasswordGeneratorEditTextPassword)?.setText(generatedPassword)
                }
            }

            findViewById<Button>(R.id.dialogEntriesPasswordGeneratorButtonInsertName)?.setOnClickListener {
                findViewById<EditText>(R.id.dialogEntriesPasswordGeneratorEditTextPassword)?.run {
                    insertToNameCallback.invoke(alertDialog, text.toString())
                }
            }

            findViewById<Button>(R.id.dialogEntriesPasswordGeneratorButtonInsertValue)?.setOnClickListener {
                findViewById<EditText>(R.id.dialogEntriesPasswordGeneratorEditTextPassword)?.run {
                    insertToValueCallback.invoke(alertDialog, text.toString())
                }
            }
        }
    }

    private fun editEntry(position: Int) {
        val entry: Pair<String, String> = vaultAccountEntryAdapter[position] ?: run {
            Toast.makeText(this, "Error! Cannot access the entry at index $position, index out of range!", Toast.LENGTH_LONG).show()
            return
        }

        val dialogEntryValuesInput: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_account_entry_values,
            null
        )

        val editTextEntryName: EditText =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputEditTextEntryName)

        val editTextEntryValue: EditText =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputEditTextEntryValue)

        val buttonPasswordGenerator: Button =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputButtonPasswordGenerator)

        editTextEntryName.setText(entry.first)
        editTextEntryValue.setText(entry.second)

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setView(dialogEntryValuesInput)
            setCancelable(true)

            setTitle("Enter New Entry Values")

            setPositiveButton("Okay")     { _, _  -> }
            setNegativeButton("Cancel")  { _ , _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val entryName: String = editTextEntryName.text.toString()
            val entryValue: String = editTextEntryValue.text.toString()

            if(entryName.isBlank()) {
                Toast.makeText(this, "Enter an entry name first. Name cannot be blank.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(vaultAccountEntryAdapter.hasEntryWithName(entryName) &&  entryName != entry.first) {
                Toast.makeText(this, "An entry with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            vaultAccountEntryAdapter.setAccountEntry(position, Pair(entryName, entryValue))

            setResult(RESULT_OK, Intent().apply {
                putExtra("VaultAccount", viewingVaultAccount.apply {
                    entries = vaultAccountEntryAdapter.accountEntryPairs.associate { it }.toMutableMap()
                })
            })

            alertDialog.dismiss()
        }

        buttonPasswordGenerator.setOnClickListener {
            showPasswordGeneratorDialog({alertDialog, generatedPassword ->
                editTextEntryName.setText(generatedPassword)
                alertDialog.dismiss()
            }, { alertDialog, generatedPassword ->
                editTextEntryValue.setText(generatedPassword)
                alertDialog.dismiss()
            })
        }
    }

    private fun addEntry(view: View?) {
        val dialogEntryValuesInput: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_input_account_entry_values,
            null
        )

        val editTextEntryName: TextView =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputEditTextEntryName)

        val editTextEntryValue: TextView =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputEditTextEntryValue)

        val buttonPasswordGenerator: Button =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputButtonPasswordGenerator)

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setView(dialogEntryValuesInput)
            setCancelable(true)

            setTitle("Enter Entry Name")

            setPositiveButton("Add")     { _, _  -> }
            setNegativeButton("Cancel")  { _ , _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val entryName: String = editTextEntryName.text.toString()
            val entryValue: String = editTextEntryValue.text.toString()

            if(entryName.isBlank()) {
                Toast.makeText(this, "Enter an entry name first. Name cannot be blank.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(vaultAccountEntryAdapter.hasEntryWithName(entryName)) {
                Toast.makeText(this, "An entry with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            vaultAccountEntryAdapter.addAccountEntry(entryName, entryValue)

            setResult(RESULT_OK, Intent().apply {
                putExtra("VaultAccount", viewingVaultAccount.apply {
                    entries = vaultAccountEntryAdapter.accountEntryPairs.associate { it }.toMutableMap()
                })
            })

            alertDialog.dismiss()
        }

        buttonPasswordGenerator.setOnClickListener {
            showPasswordGeneratorDialog({alertDialog, generatedPassword ->
                editTextEntryName.text = generatedPassword
                alertDialog.dismiss()
            }, { alertDialog, generatedPassword ->
                editTextEntryValue.text = generatedPassword
                alertDialog.dismiss()
            })
        }
    }
}