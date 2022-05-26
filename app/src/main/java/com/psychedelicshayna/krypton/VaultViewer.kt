package com.psychedelicshayna.krypton

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_vault_viewer.*
import kotlinx.android.synthetic.main.new_account_prompt.view.*

class VaultViewer : AppCompatActivity() {
    private lateinit var vaultAccountAdapter:VaultAccountAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vault_viewer)
        vaultAccountAdapter = VaultAccountAdapter(mutableListOf())

        rvVaultAccounts.adapter = vaultAccountAdapter
        rvVaultAccounts.layoutManager = LinearLayoutManager(this)

        btAddAccount.setOnClickListener {
            val promptView:View = LayoutInflater.from(this).inflate(
                R.layout.new_account_prompt,
                null
            )

            val alertDialogBuilder = AlertDialog.Builder(this)
            alertDialogBuilder.setView(promptView)

            val etAccountName:EditText = promptView.findViewById<EditText>(R.id.etAccountName)

            alertDialogBuilder.apply {
                setCancelable(true)
                setTitle("Enter Account Name")

                setPositiveButton("Add") { _, _ ->
                    val accountName:String = etAccountName.text.toString()

                    if(accountName.isNotBlank()) {
                        val success = vaultAccountAdapter.addVaultAccount(VaultAccount(etAccountName.text.toString()))

                        if(!success) {
                            Toast.makeText(this.context, "An account with that name already exists!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                setNeutralButton("Cancel") { _, _ ->

                }
            }

            val alertDialog:AlertDialog = alertDialogBuilder.create()
            alertDialog.show()
        }

        svAccountSearch.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(text: String?): Boolean {

                return false
            }

            override fun onQueryTextChange(text: String?): Boolean {
                if(text == null) return false

                if(text.isEmpty()) {
                    vaultAccountAdapter.clearAccountNameFilter()
                } else {
                    vaultAccountAdapter.setAccountNameFilter(text)
                }

                return true
            }
        })
    }
}