package com.psychedelicshayna.krypton

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_entry_viewer.*

class EntryViewer : AppCompatActivity() {
    private lateinit var entryAdapter: EntryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_entry_viewer)

        val vaultAccount: VaultAccount = (intent.getSerializableExtra("VaultAccountObject") as VaultAccount?).let {
            if(it != null) { it } else {
                Toast.makeText(this, "Received no vault account object!", Toast.LENGTH_LONG).show()
                finish()
                return
            }
        }

        entryAdapter = EntryAdapter(vaultAccount.AccountEntries)

        activityEntryViewerTextViewAccountName.text = vaultAccount.AccountName

        activityEntryViewerRecyclerViewEntries.layoutManager = LinearLayoutManager(this)
        activityEntryViewerRecyclerViewEntries.adapter = entryAdapter

        findViewById<Button>(R.id.activityEntryViewerButtonAddEntry).setOnClickListener { addEntry() }
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

            setPositiveButton("Add")     { _, _ -> }
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