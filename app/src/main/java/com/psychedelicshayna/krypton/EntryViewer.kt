package com.psychedelicshayna.krypton

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_entry_viewer.*

class EntryViewer : AppCompatActivity() {
    private lateinit var entryAdapter: EntryAdapter
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var receivedVaultAccount: VaultAccount

    private var latestContextMenuItemPressed: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_viewer)

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        receivedVaultAccount = (intent.getSerializableExtra("VaultAccount") as VaultAccount?).let {
            if(it != null) { it } else {
                Toast.makeText(this, "Received no vault account object!", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        entryAdapter = EntryAdapter(this, receivedVaultAccount.AccountEntries).apply {
            onBindViewHolderListener = { holder, position ->
                holder.onContextMenuItemClickListener = { menuItem, position, contextMenu, view, contextMenuInfo ->
                    onEntryAdapterItemViewContextMenuItemSelected(menuItem, position, contextMenu, view, contextMenuInfo)
                }
            }
        }

        activityEntryViewerTextViewAccountName.text = receivedVaultAccount.AccountName

        activityEntryViewerRecyclerViewEntries.layoutManager = LinearLayoutManager(this)
        activityEntryViewerRecyclerViewEntries.adapter = entryAdapter

        findViewById<Button>(R.id.activityEntryViewerButtonAddEntry).setOnClickListener { addEntry() }
    }

    private fun onEntryAdapterItemViewContextMenuItemSelected(menuItem: MenuItem, position: Int, contextMenu: ContextMenu?, view: View?, contextMenuInfo: ContextMenu.ContextMenuInfo?) {
        val selectedEntryPair: Pair<String, String> = entryAdapter[position] ?: run {
            Toast.makeText(this@EntryViewer, "The index of the selected item was out of range!", Toast.LENGTH_LONG).show()
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
                entryAdapter.removeAccountEntry(position)

                setResult(RESULT_OK, Intent().apply {
                    putExtra("VaultAccount", receivedVaultAccount.apply {
                        AccountEntries = entryAdapter.accountEntryPairs.associate { it }
                    })
                })
            }
        }
    }

    private fun showPasswordGeneratorDialog(insertToNameCallback: (AlertDialog, String) -> Unit, insertToValueCallback: (AlertDialog, String) -> Unit) {
        val dialogEntriesPasswordGenerator: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_entries_password_generator,
            null
        )

        val buttonGeneratePassword: Button =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorButtonGenerate) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the generate button.", Toast.LENGTH_LONG).show()
                return
            }

        val buttonInsertToName: Button =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorButtonInsertName) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the insert to name button.", Toast.LENGTH_LONG).show()
                return
            }

        val buttonInsertToValue: Button =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorButtonInsertValue) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the insert to value button.", Toast.LENGTH_LONG).show()
                return
            }

        val editTextPasswordLength: EditText =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorEditTextLength) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the password length edit text.", Toast.LENGTH_LONG).show()
                return
            }

        val editTextGeneratedPassword: EditText =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorEditTextPassword) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the password length edit text.", Toast.LENGTH_LONG).show()
                return
            }

        val checkBoxUpper: CheckBox =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorCheckBoxUpper) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the uppercase checkbox.", Toast.LENGTH_LONG).show()
                return
            }

        val checkBoxLower: CheckBox =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorCheckBoxLower) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the lowercase checkbox.", Toast.LENGTH_LONG).show()
                return
            }

        val checkBoxNumerical: CheckBox =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorCheckBoxNumeric) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the numerical checkbox.", Toast.LENGTH_LONG).show()
                return
            }

        val checkBoxSpecial: CheckBox =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorCheckBoxSpecial) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the special characters checkbox.", Toast.LENGTH_LONG).show()
                return
            }

        val checkBoxExtra: CheckBox =
            dialogEntriesPasswordGenerator.findViewById(R.id.dialogEntriesPasswordGeneratorCheckBoxExtra) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the extra characters checkbox.", Toast.LENGTH_LONG).show()
                return
            }

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setView(dialogEntriesPasswordGenerator)
            setCancelable(true)
            setTitle("Password Generator")
            setNegativeButton("Cancel") { _, _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        buttonGeneratePassword.setOnClickListener {
            val passwordGenerator = PasswordGenerator()

            val characterClassSpec: PasswordGenerator.CharacterClassSpec = PasswordGenerator.CharacterClassSpec().apply {
                uppercase.enabled = checkBoxUpper.isChecked
                lowercase.enabled = checkBoxLower.isChecked
                numerical.enabled = checkBoxNumerical.isChecked
                special.enabled   = checkBoxSpecial.isChecked
                extra.enabled     = checkBoxExtra.isChecked
            }

            val passwordLength: Int = editTextPasswordLength.text.toString().toInt()
            val generatedPassword: String = passwordGenerator.generatePassword(passwordLength, characterClassSpec)

            editTextGeneratedPassword.setText(generatedPassword)
        }

        buttonInsertToName.setOnClickListener {
            insertToNameCallback.invoke(alertDialog, editTextGeneratedPassword.text.toString())
        }

        buttonInsertToValue.setOnClickListener {
            insertToValueCallback.invoke(alertDialog, editTextGeneratedPassword.text.toString())
        }
    }

    private fun editEntry(position: Int) {
        val entry: Pair<String, String> = entryAdapter[position] ?: run {
            Toast.makeText(this, "Error! Cannot access the entry at index $position, index out of range!", Toast.LENGTH_LONG).show()
            return
        }

        val dialogEntryValuesInput: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_entry_values_input,
            null
        )

        val editTextEntryName: EditText =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputEditTextEntryName) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the entry name EditText!", Toast.LENGTH_SHORT).show()
                return@editEntry
            }

        val editTextEntryValue: EditText =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputEditTextEntryValue) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the entry value EditText!", Toast.LENGTH_SHORT).show()
                return@editEntry
            }

        val buttonPasswordGenerator: Button =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputButtonPasswordGenerator) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the password generator button!", Toast.LENGTH_SHORT).show()
                return@editEntry
            }

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

            if(entryAdapter.hasEntryWithName(entryName) &&  entryName != entry.first) {
                Toast.makeText(this, "An entry with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            entryAdapter.setAccountEntry(position, Pair(entryName, entryValue))

            setResult(ActivityResultRequestCodes.EntryViewer.updateAccount, Intent().apply {
                putExtra("VaultAccount", receivedVaultAccount.apply {
                    AccountEntries = entryAdapter.accountEntryPairs.associate { it }
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

    private fun addEntry() {
        val dialogEntryValuesInput: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_entry_values_input,
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

            if(entryAdapter.hasEntryWithName(entryName)) {
                Toast.makeText(this, "An entry with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            entryAdapter.addAccountEntry(entryName, entryValue)

            setResult(RESULT_OK, Intent().apply {
                putExtra("VaultAccount", receivedVaultAccount.apply {
                    AccountEntries = entryAdapter.accountEntryPairs.associate { it }
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