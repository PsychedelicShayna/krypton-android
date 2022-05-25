package com.psychedelicshayna.krypton

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_vault_account.view.*

class VaultAccountAdapter(
    private val vaultAccounts:MutableList<VaultAccount>
) : RecyclerView.Adapter<VaultAccountAdapter.VaultAccountViewHolder>() {
    class VaultAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val vaultAccountsFilterReference:MutableList<VaultAccount> = mutableListOf()

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

    fun addVaultAccount(vaultAccount: VaultAccount) {
        vaultAccounts.add(vaultAccount)
        notifyItemInserted(vaultAccounts.size - 1)
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

    fun itemAt(index:Int): VaultAccount? {
        if(index >= vaultAccounts.size) return null
        return vaultAccounts[index]
    }

    fun updateAccountNameFilter(filterText:String) {
       if(filterText.isBlank() && vaultAccountsFilterReference.isNotEmpty()) {
           vaultAccounts.clear()
           vaultAccounts.addAll(vaultAccountsFilterReference)
           vaultAccountsFilterReference.clear()
       } else {
           if(vaultAccountsFilterReference.isEmpty()) {
               vaultAccountsFilterReference.addAll(vaultAccounts)
           }

           for(i in 0 until vaultAccounts.size - 1) {
               vaultAccounts.removeAt(i)
               notifyItemRemoved(i)
           }

           for(i in 0 until vaultAccountsFilterReference.size - 1) {
               val vaultAccount:VaultAccount = vaultAccountsFilterReference[i]

               if(vaultAccount.AccountName.contains(filterText)) {
                   vaultAccounts.add(vaultAccount)
                   notifyItemInserted(vaultAccounts.size - 1)
               }
           }
       }
    }
}