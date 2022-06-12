package com.psychedelicshayna.krypton

import android.content.Context
import android.view.*
import androidx.core.view.iterator
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_vault_account.view.*
import java.lang.IndexOutOfBoundsException
import java.util.*

class VaultAccountAdapter(
    private val parentContext: Context,
    private val vaultAccountBackBuffer: MutableList<VaultAccount>
) : RecyclerView.Adapter<VaultAccountAdapter.AccountItemViewHolder>() {

    enum class BackBufferChangeType { INSERTED, REMOVED, CHANGED }

    class AccountItemViewHolder(
        val parentContext: Context,
        val representedItemView: View
    ) : RecyclerView.ViewHolder(representedItemView) {
        var onContextMenuItemClickListener: ((MenuItem, Int, ContextMenu?, View?, ContextMenu.ContextMenuInfo?) -> Unit)? = null

        private fun onCreateContextMenuListener(menu: ContextMenu?, view: View?, menuInfo: ContextMenu.ContextMenuInfo?) {
            menu?.let { contextMenu ->
                MenuInflater(parentContext).inflate(R.menu.menu_account_viewer_account_context_menu, contextMenu)

                for(menuItem: MenuItem in contextMenu) {
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

    private val vaultAccountFrontBuffer: MutableList<VaultAccount> = mutableListOf()
    private var accountNameSearchQuery: String = ""

    init {
        vaultAccountFrontBuffer.addAll(vaultAccountBackBuffer)
        notifyItemRangeInserted(0, vaultAccountFrontBuffer.size)
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
        holder.itemView.apply {
            tvVaultAccountName.text = vaultAccountFrontBuffer[position].AccountName
            onBindViewHolderListener?.invoke(holder, position)
        }
    }

    override fun getItemCount(): Int =
        vaultAccountFrontBuffer.size

    private fun notifyBackBufferChange(index: Int, changeType: BackBufferChangeType) {
        if(accountNameSearchQuery.isNotEmpty()) return autoPopulateFrontBuffer()

        when(changeType) {
            BackBufferChangeType.INSERTED -> {
                vaultAccountFrontBuffer.add(index, vaultAccountBackBuffer[index])
                notifyItemInserted(index)
            }

            BackBufferChangeType.REMOVED -> {
                vaultAccountFrontBuffer.removeAt(index)
                notifyItemRemoved(index)
            }

            BackBufferChangeType.CHANGED -> {
                vaultAccountFrontBuffer[index] = vaultAccountBackBuffer[index]
                notifyItemChanged(index)
            }
        }
    }

    private fun autoPopulateFrontBuffer() {
        val newFrontBuffer: MutableList<VaultAccount> = mutableListOf()

        for(element in vaultAccountBackBuffer) {
            if(element.AccountName.lowercase(Locale.ROOT).contains(accountNameSearchQuery.lowercase(Locale.ROOT))) {
                newFrontBuffer.add(element)
            }
        }

        vaultAccountFrontBuffer.size.also {
            vaultAccountFrontBuffer.clear()
            notifyItemRangeRemoved(0, it)
        }

        vaultAccountFrontBuffer.addAll(newFrontBuffer)
        notifyItemRangeInserted(0, newFrontBuffer.size)
    }


    fun setVaultAccounts(vaultAccounts: Array<VaultAccount>) {
        clearVaultAccounts()

        vaultAccountBackBuffer.addAll(vaultAccounts)
        vaultAccountFrontBuffer.addAll(vaultAccounts)
        notifyItemRangeInserted(0, vaultAccountFrontBuffer.size)
    }

    fun setVaultAccount(vaultAccountIndex: Int, newVaultAccount: VaultAccount) {
        if(vaultAccountIndex >= vaultAccountBackBuffer.size)
            throw IndexOutOfBoundsException("vaultAccountIndex exceeds ${vaultAccountBackBuffer.size}")

        vaultAccountBackBuffer[vaultAccountIndex] = newVaultAccount
        notifyBackBufferChange(vaultAccountIndex, BackBufferChangeType.CHANGED)
    }

    fun setVaultAccountEntries(vaultAccountIndex: Int, newEntries: Map<String, String>) {
        if(vaultAccountIndex >= vaultAccountBackBuffer.size)
            throw IndexOutOfBoundsException("vaultAccountIndex exceeds ${vaultAccountBackBuffer.size}")

        vaultAccountBackBuffer[vaultAccountIndex].AccountEntries = newEntries
    }

    fun getVaultAccounts(): Array<VaultAccount> = vaultAccountBackBuffer.toTypedArray()

    fun getVaultAccountIndexByName(accountName: String): Int? {
        val foundVaultAccount: VaultAccount? = vaultAccountBackBuffer.find { vaultAccount ->
            vaultAccount.AccountName == accountName
        }

        return if(foundVaultAccount != null) vaultAccountBackBuffer.indexOf(foundVaultAccount)
               else foundVaultAccount
    }

    fun getVaultAccountIndex(vaultAccount: VaultAccount): Int =
        vaultAccountBackBuffer.indexOf(vaultAccount)

    fun getBackBufferIndexFromFrontBuffer(index: Int): Int? =
        getVaultAccountIndexByName(itemAtFrontBuffer(index)?.AccountName ?: "")

    fun addVaultAccount(vaultAccount: VaultAccount): Boolean {
        val duplicate: Boolean = vaultAccountBackBuffer.any { element ->
            element.AccountName == vaultAccount.AccountName
        }

        return if(duplicate) false else {
            vaultAccountBackBuffer.add(vaultAccount)
            notifyBackBufferChange(vaultAccountBackBuffer.size - 1, BackBufferChangeType.INSERTED)
            true
        }
    }

    fun removeVaultAccount(vaultAccountIndex: Int) {
        if(vaultAccountIndex < vaultAccountBackBuffer.size) {
            vaultAccountBackBuffer.removeAt(vaultAccountIndex)
            notifyBackBufferChange(vaultAccountIndex, BackBufferChangeType.REMOVED)
        }
    }

    fun clearVaultAccounts() {
        vaultAccountBackBuffer.clear()

        vaultAccountFrontBuffer.size.also {
            vaultAccountFrontBuffer.clear()
            notifyItemRangeRemoved(0, it)
        }
    }

    fun itemAtFrontBuffer(index: Int): VaultAccount? =
        if(index < vaultAccountFrontBuffer.size) vaultAccountFrontBuffer[index] else null

    fun itemAtBackBuffer(index: Int): VaultAccount? =
        if(index < vaultAccountBackBuffer.size) vaultAccountBackBuffer[index] else null

    fun clearAccountNameSearch() {
        accountNameSearchQuery = ""

        val vaultAccountFrontBufferSizeCopy: Int = vaultAccountFrontBuffer.size

        vaultAccountFrontBuffer.clear()
        notifyItemRangeRemoved(0, vaultAccountFrontBufferSizeCopy)

        vaultAccountFrontBuffer.addAll(vaultAccountBackBuffer)
        notifyItemRangeInserted(0, vaultAccountFrontBuffer.size)
    }

    fun searchAccountNames(searchQuery: String) {
        accountNameSearchQuery = searchQuery
        autoPopulateFrontBuffer()
    }

    fun hasAccountWithName(accountName: String): Boolean =
        vaultAccountBackBuffer.any { vaultAccount -> vaultAccount.AccountName.contentEquals(accountName) }
}