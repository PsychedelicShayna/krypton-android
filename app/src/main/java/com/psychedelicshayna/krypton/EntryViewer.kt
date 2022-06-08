package com.psychedelicshayna.krypton

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
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

        activityEntryViewerTextViewAccountName.text = vaultAccount.AccountName

        entryAdapter = EntryAdapter(vaultAccount.AccountEntries)

        activityEntryViewerRecyclerViewEntries.adapter = entryAdapter
        activityEntryViewerRecyclerViewEntries.layoutManager = LinearLayoutManager(this)
    }

    fun addEntry() {

    }
}