package com.psychedelicshayna.krypton

import android.content.Context
import android.view.*
import android.widget.TextView
import androidx.core.view.iterator
import androidx.recyclerview.widget.RecyclerView
import java.lang.IndexOutOfBoundsException
import java.util.*

class VaultAdapter(
    private val parentContext: Context,
    private val adaptedVault: Vault
) : RecyclerView.Adapter<VaultAdapter.AccountItemViewHolder>() {

    // This instance is used to actually store the vault information, and make
    // modifications to that information. displayVault is synchronized with this
    // instance; the contents of storageVault will affect the contents of displayVault,
    // but the contents of displayVault will never affect the contents of storageVault.
    // This instance has no impact on what is or isn't displayed on the RecyclerView.

    private var storageVault: Vault = adaptedVault.clone()

    // This Vault instance is purely used to display items on the RecyclerView. The data
    // contained within this instance is what will be displayed, but may not represent all
    // of the data that is being stored by this adapter. E.g. a search may be made, which
    // will remove elements from displayVault.accounts that do not match the search term,
    // therefore also removing it from the RecyclerView, but if the search term is cleared,
    // or it changes, elements that should be displayed again will be added back into the
    // the displayVault instance from storageVault.

    private var displayVault: Vault = adaptedVault.clone()

    enum class ChangeType { INSERTED, REMOVED, CHANGED }

    class AccountItemViewHolder(
        val parentContext: Context,
        val representedItemView: View
    ) : RecyclerView.ViewHolder(representedItemView) {
        var onContextMenuItemClickListener:
            ((MenuItem, Int, ContextMenu?, View?, ContextMenu.ContextMenuInfo?) -> Unit)? = null

        private fun onCreateContextMenuListener(
            menu: ContextMenu?,
            view: View?,
            menuInfo: ContextMenu.ContextMenuInfo?
        ) {
            menu?.let { contextMenu ->
                MenuInflater(parentContext).inflate(R.menu.menu_account_viewer_account_context_menu, contextMenu)

                for (menuItem: MenuItem in contextMenu) {
                    menuItem.setOnMenuItemClickListener {

                        onContextMenuItemClickListener?.invoke(
                            menuItem, bindingAdapterPosition, menu, view, menuInfo
                        )

                        return@setOnMenuItemClickListener true
                    }
                }
            }
        }

        init {
            representedItemView.setOnCreateContextMenuListener(::onCreateContextMenuListener)
        }
    }

    var onCreateViewHolderListener: ((ViewGroup, Int) -> Unit)? = null
    var onBindViewHolderListener: ((AccountItemViewHolder, Int) -> Unit)? = null

    private var accountNameSearchQuery: String = ""

    init {
        notifyItemRangeInserted(0, displayVault.accounts.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountItemViewHolder {
        return AccountItemViewHolder(
            parentContext,
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_vault_account,
                parent,
                false
            )
        ).also {
            onCreateViewHolderListener?.invoke(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: AccountItemViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.tvVaultAccountName).apply {
            text = displayVault.accounts[position].name
        }

        onBindViewHolderListener?.invoke(holder, position)
    }

    override fun getItemCount(): Int = displayVault.accounts.size

    private fun notifyStorageVaultChange(index: Int, changeType: ChangeType) {
        if (accountNameSearchQuery.isNotEmpty()) return autoPopulateDisplayVault()

        when (changeType) {
            ChangeType.INSERTED -> {
                displayVault.accounts.add(index, storageVault.accounts[index])
                notifyItemInserted(index)
            }

            ChangeType.REMOVED -> {
                displayVault.accounts.removeAt(index)
                notifyItemRemoved(index)
            }

            ChangeType.CHANGED -> {
                displayVault.accounts[index] = storageVault.accounts[index]
                notifyItemChanged(index)
            }
        }
    }

    private fun autoPopulateDisplayVault() {
        val newDisplayVault = Vault()

        for (account in storageVault.accounts) {
            if (account.name.lowercase(Locale.ROOT).contains(accountNameSearchQuery.lowercase(Locale.ROOT))) {
                newDisplayVault.accounts.add(account)
            }
        }

        displayVault.accounts.size.also {
            displayVault.accounts.clear()
            notifyItemRangeRemoved(0, it)
        }

        displayVault.accounts.addAll(newDisplayVault.accounts)
        notifyItemRangeInserted(0, newDisplayVault.accounts.size)
    }

    fun addVaultAccount(newAccount: Vault.Account): Boolean =
        storageVault.accounts.all { account ->
            account.name != newAccount.name
        }.also { isNotDuplicate ->
            if (isNotDuplicate) {
                storageVault.accounts.add(newAccount)
                notifyStorageVaultChange(storageVault.accounts.size - 1, ChangeType.INSERTED)
            }
        }

    fun removeVaultAccount(vaultAccountIndex: Int) {
        if (vaultAccountIndex < storageVault.accounts.size) {
            storageVault.accounts.removeAt(vaultAccountIndex)
            notifyStorageVaultChange(vaultAccountIndex, ChangeType.REMOVED)
        }
    }

    fun removeAllVaultAccounts() {
        storageVault.accounts.clear()

        displayVault.accounts.size.also {
            displayVault.accounts.clear()
            notifyItemRangeRemoved(0, it)
        }
    }

    fun setVault(newVault: Vault) {
        removeAllVaultAccounts()

        storageVault = newVault.clone()
        displayVault = newVault.clone()

        notifyItemRangeInserted(0, displayVault.accounts.size)
    }

    fun setVaultAccount(index: Int, newVaultAccount: Vault.Account) {
        if (index >= storageVault.accounts.size)
            throw IndexOutOfBoundsException("Index exceeds ${storageVault.accounts.size}")

        storageVault.accounts[index] = newVaultAccount
        notifyStorageVaultChange(index, ChangeType.CHANGED)
    }

    fun setVaultAccountEntries(index: Int, newEntries: MutableMap<String, String>) {
        if (index >= storageVault.accounts.size)
            throw IndexOutOfBoundsException("vaultAccountIndex exceeds ${storageVault.accounts.size}")

        storageVault.accounts[index].entries = newEntries.toMutableMap()
    }

    fun getVault(): Vault = storageVault.clone()

    fun getVaultAccounts(): MutableList<Vault.Account> = storageVault.accounts.toMutableList()

    fun getVaultAccountIndexByName(accountName: String): Int? =
        storageVault.accounts.find { vaultAccount ->
            vaultAccount.name == accountName
        }?.let { foundVaultAccount ->
            storageVault.accounts.indexOf(foundVaultAccount)
        }

    fun getVaultAccountIndexFromDisplayVaultAccountIndex(index: Int): Int? =
        getVaultAccountIndexByName(getDisplayVaultAccount(index)?.name ?: "")

    fun getDisplayVaultAccount(index: Int): Vault.Account? =
        if (index < displayVault.accounts.size) displayVault.accounts[index]
        else null

    fun getStorageVaultAccount(index: Int): Vault.Account? =
        if (index < storageVault.accounts.size) storageVault.accounts[index]
        else null

    fun performAccountNameSearchQuery(searchQuery: String) {
        accountNameSearchQuery = searchQuery
        autoPopulateDisplayVault()
    }

    fun clearAccountNameSearchQuery() {
        accountNameSearchQuery = ""

        val displayVaultSizeCopy: Int = displayVault.accounts.size

        displayVault.accounts.clear()
        notifyItemRangeRemoved(0, displayVaultSizeCopy)

        displayVault.accounts.addAll(storageVault.accounts)
        notifyItemRangeInserted(0, displayVault.accounts.size)
    }

    fun hasAccountWithName(name: String): Boolean =
        storageVault.accounts.any { account ->
            account.name.contentEquals(name)
        }
}
