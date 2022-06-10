package com.psychedelicshayna.krypton

import android.content.ClipData
import android.content.ClipboardManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Button
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

        entryAdapter = EntryAdapter(vaultAccount.AccountEntries).apply {
            entryAdapterListener = object : EntryAdapter.EntryAdapterListener() {
                override fun onBindViewHolderListener(holder: EntryAdapter.EntryItemViewHolder, position: Int) {
                    super.onBindViewHolderListener(holder, position)
                    registerForContextMenu(holder.itemView)
                }
            }
        }

        activityEntryViewerTextViewAccountName.text = vaultAccount.AccountName

        activityEntryViewerRecyclerViewEntries.layoutManager = LinearLayoutManager(this)
        activityEntryViewerRecyclerViewEntries.adapter = entryAdapter

        findViewById<Button>(R.id.activityEntryViewerButtonAddEntry).setOnClickListener { addEntry() }
    }

    override fun onCreateContextMenu(menu: ContextMenu?, view: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
        super.onCreateContextMenu(menu, view, menuInfo)
        menuInflater.inflate(R.menu.menu_entry_viewer_entry_context_menu, menu)
        latestContextMenuItemPressed = view
    }

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val contextMenuInfo: AdapterView.AdapterContextMenuInfo = item as AdapterView.AdapterContextMenuInfo

        val selectedEntryPair: Pair<String, String> = entryAdapter[contextMenuInfo.position].let {
            it ?: run {
                Toast.makeText(this@EntryViewer, "The index of the selected item was out of range!", Toast.LENGTH_LONG).show()
                return true
            }
        }

        when(item.itemId) {
            R.id.menuEntryViewerEntryContextMenuItemCopyEntryName -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("entryName", selectedEntryPair.first)
                )

                return true
            }

            R.id.menuEntryViewerEntryContextMenuItemCopyEntryValue -> {
                clipboardManager.setPrimaryClip(
                    ClipData.newPlainText("entryValue", selectedEntryPair.second)
                )

                return true
            }

            R.id.menuEntryViewerEntryContextMenuItemEdit -> {
//                latestContextMenuItemPressed.
                return true
            }
        }

        return super.onContextItemSelected(item)
    }

    private fun addEntry() {
        val dialogNewEntry: View = LayoutInflater.from(this).inflate(
            R.layout.dialog_new_entry,
            null
        )

        val dialogNewEntryTextEditEntryName: TextView
            = dialogNewEntry.findViewById(R.id.dialogNewEntryTextEditEntryName)

        val dialogNewEntryTextEditEntryValue: TextView
            = dialogNewEntry.findViewById(R.id.dialogNewEntryTextEditEntryValue)

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