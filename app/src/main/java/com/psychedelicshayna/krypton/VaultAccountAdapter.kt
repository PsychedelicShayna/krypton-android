package com.psychedelicshayna.krypton

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_vault_account.view.*
import java.util.*

class VaultAccountAdapter(
    private val vaultAccounts:MutableList<VaultAccount>
) : RecyclerView.Adapter<VaultAccountAdapter.VaultAccountViewHolder>() {
    class VaultAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val vaultAccountsFilterReference:MutableList<VaultAccount> = mutableListOf()
    private var vaultAccountNameFilterActive:Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultAccountViewHolder {
        return VaultAccountViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_vault_account,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: VaultAccountViewHolder, position: Int) {
        holder.itemView.apply {
            tvVaultAccountName.text = vaultAccounts[position].AccountName
        }
    }

    override fun getItemCount(): Int {
        return vaultAccounts.size
    }

    fun addVaultAccount(vaultAccount: VaultAccount): Boolean {
        val duplicate: Boolean = vaultAccounts.any { element ->
            element.AccountName == vaultAccount.AccountName
        }

        if(duplicate) {
            return false
        }

        vaultAccounts.add(vaultAccount)
        notifyItemInserted(vaultAccounts.size - 1)
        return true
    }

    fun removeVaultAccount(vaultAccountIndex: Int) {
        if(vaultAccountIndex < vaultAccounts.size) {
            vaultAccounts.removeAt(vaultAccountIndex)
            notifyItemRemoved(vaultAccountIndex)
        }
    }

    fun removeVaultAccount(vaultAccount: VaultAccount) {
        if(vaultAccounts.contains(vaultAccount)) {
            removeVaultAccount(vaultAccounts.indexOf(vaultAccount))
        }
    }

    fun clearVaultAccounts() {
        val elementsRemoved = vaultAccounts.size
        vaultAccounts.clear()

        notifyItemRangeRemoved(0, elementsRemoved)
    }

    fun itemAt(index: Int): VaultAccount? {
        if(index >= vaultAccounts.size) return null
        return vaultAccounts[index]
    }

    fun clearAccountNameFilter() {
        if(vaultAccountsFilterReference.isNotEmpty()) {
            val elementsRemoved: Int = vaultAccounts.size
            vaultAccounts.clear()

            notifyItemRangeRemoved(0, elementsRemoved)

            vaultAccounts.addAll(vaultAccountsFilterReference)
            notifyItemRangeInserted(0, vaultAccounts.size)

            vaultAccountsFilterReference.clear()
        }
    }

    fun setAccountNameFilter(filterText: String) {
        if(vaultAccountsFilterReference.isEmpty()) {
            vaultAccountsFilterReference.addAll(vaultAccounts)
        }

        val elementsRemoved: Int = vaultAccounts.size
        vaultAccounts.clear()

        notifyItemRangeRemoved(0, elementsRemoved)

        for(index in 0 until vaultAccountsFilterReference.size) {
            val vaultAccount = vaultAccountsFilterReference[index]

            if(vaultAccount.AccountName.toLowerCase(Locale.ROOT).contains(filterText.toLowerCase(Locale.ROOT))) {
                vaultAccounts.add(vaultAccount)
                notifyItemInserted(vaultAccounts.size)
            }
        }
    }
}