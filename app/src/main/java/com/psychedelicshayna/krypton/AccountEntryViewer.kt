package com.psychedelicshayna.krypton

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_entry_viewer.*

class AccountEntryViewer : AppCompatActivity() {
    private lateinit var accountEntryAdapter: AccountEntryAdapter

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

        tvOpenedVaultAccountName.text = vaultAccount.AccountName

        accountEntryAdapter = AccountEntryAdapter(vaultAccount.AccountEntries)

        rvEntries.adapter = accountEntryAdapter
        rvEntries.layoutManager = LinearLayoutManager(this)
    }
}