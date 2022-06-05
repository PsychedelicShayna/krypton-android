package com.psychedelicshayna.krypton

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.InspectableProperty
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_vault_account.view.*
import java.lang.IndexOutOfBoundsException
import java.util.*

class VaultAccountAdapter(
    private val vaultAccounts: MutableList<VaultAccount>
) : RecyclerView.Adapter<VaultAccountAdapter.VaultAccountViewHolder>() {
    class VaultAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    private val displayedVaultAccounts: MutableList<VaultAccount> = mutableListOf()

    private val vaultAccountsFilterReference: MutableList<VaultAccount> = mutableListOf()
    private var accountNameFilterString: String = ""

    var vaultAccountViewClickListener: (VaultAccountViewHolder, Int) -> Unit = { _, _ -> }

    init {
        displayedVaultAccounts.addAll(vaultAccounts)
    }

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
            tvVaultAccountName.text = displayedVaultAccounts[position].AccountName
            holder.itemView.setOnClickListener {  vaultAccountViewClickListener.invoke(holder, position) }
        }
    }

    override fun getItemCount(): Int {
        return displayedVaultAccounts.size
    }

    fun getVaultAccounts(): Array<VaultAccount> = displayedVaultAccounts.toTypedArray()

    fun addVaultAccount(vaultAccount: VaultAccount): Boolean {
        accountNameFilterString.let { filterString ->
            clearAccountNameFilter()

            val duplicate: Boolean = displayedVaultAccounts.any { element ->
                element.AccountName == vaultAccount.AccountName
            }

            if(duplicate) {
                return false
            }

            displayedVaultAccounts.add(vaultAccount)
            notifyItemInserted(displayedVaultAccounts.size - 1)

            setAccountNameFilter(filterString)
            return true
        }

    }

    fun removeVaultAccount(vaultAccountIndex: Int) {
        accountNameFilterString.let { filterString ->
            clearAccountNameFilter()

            if(vaultAccountIndex < displayedVaultAccounts.size) {
                displayedVaultAccounts.removeAt(vaultAccountIndex)
                notifyItemRemoved(vaultAccountIndex)
            }

            setAccountNameFilter(filterString)
        }
    }

    fun clearVaultAccounts() {
        accountNameFilterString.let { filterString ->
            clearAccountNameFilter()

            displayedVaultAccounts.size.also {
                displayedVaultAccounts.clear()
                notifyItemRangeRemoved(0, it)
            }

            setAccountNameFilter(filterString)
        }
    }

    fun setItemAt(vaultAccountIndex: Int, newVaultAccount: VaultAccount) {
        if(vaultAccountIndex >= displayedVaultAccounts.size)
            throw IndexOutOfBoundsException("vaultAccountIndex exceeds ${displayedVaultAccounts.size}")

        displayedVaultAccounts[vaultAccountIndex] = newVaultAccount
        notifyItemChanged(vaultAccountIndex)
    }

    fun itemAt(index: Int): VaultAccount? =
        if(index < displayedVaultAccounts.size) displayedVaultAccounts[index]  else null

    fun clearAccountNameFilter() {
        if(vaultAccountsFilterReference.isNotEmpty()) {
            val elementsRemoved: Int = displayedVaultAccounts.size
            displayedVaultAccounts.clear()

            notifyItemRangeRemoved(0, elementsRemoved)

            displayedVaultAccounts.addAll(vaultAccountsFilterReference)
            notifyItemRangeInserted(0, displayedVaultAccounts.size)

            vaultAccountsFilterReference.clear()
            accountNameFilterString = ""
        }
    }

    fun setAccountNameFilter(filterText: String) {
        accountNameFilterString = filterText

        if(vaultAccountsFilterReference.isEmpty()) {
            vaultAccountsFilterReference.addAll(displayedVaultAccounts)
        }

        val elementsRemoved: Int = displayedVaultAccounts.size
        displayedVaultAccounts.clear()

        notifyItemRangeRemoved(0, elementsRemoved)

        for(index in 0 until vaultAccountsFilterReference.size) {
            val vaultAccount = vaultAccountsFilterReference[index]

            if(vaultAccount.AccountName.lowercase(Locale.ROOT).contains(filterText.toLowerCase(Locale.ROOT))) {
                displayedVaultAccounts.add(vaultAccount)
                notifyItemInserted(displayedVaultAccounts.size)
            }
        }
    }
}