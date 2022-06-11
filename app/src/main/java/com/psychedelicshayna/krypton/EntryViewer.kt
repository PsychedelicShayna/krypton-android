package com.psychedelicshayna.krypton

import android.content.ClipData
import android.content.ClipboardManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_entry_viewer.*

class EntryViewer : AppCompatActivity() {
    private lateinit var entryAdapter: EntryAdapter
    private lateinit var clipboardManager: ClipboardManager
    private var latestContextMenuItemPressed: View? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_viewer)

        clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

        val vaultAccount: VaultAccount = (intent.getSerializableExtra("VaultAccountObject") as VaultAccount?).let {
            if(it != null) { it } else {
                Toast.makeText(this, "Received no vault account object!", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        entryAdapter = EntryAdapter(this, vaultAccount.AccountEntries).apply {
            onBindViewHolderListener = { holder, position ->
                holder.onContextMenuItemClickListener = { menuItem, position, contextMenu, view, contextMenuInfo ->
                    onEntryAdapterItemViewContextMenuItemSelected(menuItem, position, contextMenu, view, contextMenuInfo)
                }
            }
        }

        activityEntryViewerTextViewAccountName.text = vaultAccount.AccountName

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

                return
            }

            R.id.menuEntryViewerEntryContextMenuItemCopyEntryValue -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("entryValue", selectedEntryPair.second)
                )

                return
            }

            R.id.menuEntryViewerEntryContextMenuItemEdit -> {
                editEntry(position)
                return
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

        val dialogEntryValuesInputEditTextEntryName: EditText =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputEditTextEntryName) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the entry name EditText!", Toast.LENGTH_SHORT).show()
                return@editEntry
            }

        val dialogEntryValuesInputEditTextEntryValue: EditText =
            dialogEntryValuesInput.findViewById(R.id.dialogEntryValuesInputEditTextEntryValue) ?: run {
                Toast.makeText(this, "Error! Could not find the view for the entry value EditText!", Toast.LENGTH_SHORT).show()
                return@editEntry
            }

        dialogEntryValuesInputEditTextEntryName.setText(entry.first)
        dialogEntryValuesInputEditTextEntryValue.setText(entry.second)

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
            val entryName: String = dialogEntryValuesInputEditTextEntryName.text.toString()
            val entryValue: String = dialogEntryValuesInputEditTextEntryValue.text.toString()

            if(entryName.isBlank()) {
                Toast.makeText(this, "Enter an entry name first. Name cannot be blank.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(entryAdapter.hasEntryWithName(entryName) &&  entryName != entry.first) {
                Toast.makeText(this, "An entry with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            entryAdapter.setAccountEntry(position, Pair(entryName, entryValue))
            alertDialog.dismiss()
        }
    }

    private fun addEntry() {
        val dialogNewEntry: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_entry_values_input,
            null
        )

        val dialogNewEntryTextEditEntryName: TextView
            = dialogNewEntry.findViewById(R.id.dialogEntryValuesInputEditTextEntryName)

        val dialogNewEntryTextEditEntryValue: TextView
            = dialogNewEntry.findViewById(R.id.dialogEntryValuesInputEditTextEntryValue)

        val alertDialogBuilder: AlertDialog.Builder = AlertDialog.Builder(this).apply {
            setView(dialogNewEntry)
            setCancelable(true)

            setTitle("Enter Entry Name")

            setPositiveButton("Add")     { _, _  -> }
            setNegativeButton("Cancel")  { _ , _ -> }
        }

        val alertDialog: AlertDialog = alertDialogBuilder.create()
        alertDialog.show()

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val entryName: String = dialogNewEntryTextEditEntryName.text.toString()
            val entryValue: String = dialogNewEntryTextEditEntryValue.text.toString()

            if(entryName.isBlank()) {
                Toast.makeText(this, "Enter an entry name first. Name cannot be blank.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if(entryAdapter.hasEntryWithName(entryName)) {
                Toast.makeText(this, "An entry with that name already exists!", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            entryAdapter.addAccountEntry(entryName, entryValue)
            alertDialog.dismiss()
        }
    }
}