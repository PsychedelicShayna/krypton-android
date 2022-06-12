package com.psychedelicshayna.krypton

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.view.View.OnTouchListener
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_entry_viewer.*
import kotlinx.android.synthetic.main.dialog_entries_password_generator.view.*


class EntryViewer : AppCompatActivity() /*, OnTouchListener */ {
    private lateinit var entryAdapter: EntryAdapter
    private lateinit var clipboardManager: ClipboardManager
    private lateinit var receivedVaultAccount: VaultAccount

    private var latestContextMenuItemPressed: View? = null

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_viewer)

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        receivedVaultAccount = (intent.getSerializableExtra("VaultAccount") as VaultAccount?).let {
            if (it != null) {
                it
            } else {
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

            setResult(RESULT_OK, Intent().apply {
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