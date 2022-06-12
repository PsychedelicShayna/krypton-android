package com.psychedelicshayna.krypton

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_vault_account.view.*
import java.lang.IndexOutOfBoundsException
import java.util.*

class VaultAccountAdapter(
    private val vaultAccountsBackBuffer: MutableList<VaultAccount>
) : RecyclerView.Adapter<VaultAccountAdapter.AccountItemViewHolder>() {
    class AccountItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    open class VaultAccountAdapterListener {
        open fun onCreateViewHolderListener(parent: ViewGroup, viewType: Int) = Unit
        open fun onBindViewHolderListener(holder: AccountItemViewHolder, position: Int) = Unit
    }

    var vaultAccountAdapterListener: VaultAccountAdapterListener = VaultAccountAdapterListener()

    enum class BackBufferChangeType { INSERTED, REMOVED, CHANGED }

    private val vaultAccountsFrontBuffer: MutableList<VaultAccount> = mutableListOf()
    private var accountNameFilterString: String = ""

    init {
        vaultAccountsFrontBuffer.addAll(vaultAccountsBackBuffer)
        notifyItemRangeInserted(0, vaultAccountsFrontBuffer.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountItemViewHolder {
        return AccountItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_vault_account,
                parent,
                false
            )
        ).also {
            vaultAccountAdapterListener.onCreateViewHolderListener(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: AccountItemViewHolder, position: Int) {
        holder.itemView.apply {
            tvVaultAccountName.text = vaultAccountsFrontBuffer[position].AccountName
            vaultAccountAdapterListener.onBindViewHolderListener(holder, position)
        }
    }

    override fun getItemCount(): Int =
        vaultAccountsFrontBuffer.size

    private fun notifyBackBufferChange(index: Int, changeType: BackBufferChangeType) {
        if(accountNameFilterString.isNotEmpty()) return autoPopulateFrontBuffer()

        when(changeType) {
            BackBufferChangeType.INSERTED -> {
                vaultAccountsFrontBuffer.add(index, vaultAccountsBackBuffer[index])
                notifyItemInserted(index)
            }

            BackBufferChangeType.REMOVED -> {
                vaultAccountsFrontBuffer.removeAt(index)
                notifyItemRemoved(index)
            }

            BackBufferChangeType.CHANGED -> {
                vaultAccountsFrontBuffer[index] = vaultAccountsBackBuffer[index]
                notifyItemChanged(index)
            }
        }
    }

    private fun autoPopulateFrontBuffer() {
        val newFrontBuffer: MutableList<VaultAccount> = mutableListOf()

        for(element in vaultAccountsBackBuffer) {
            if(element.AccountName.lowercase(Locale.ROOT).contains(accountNameFilterString.lowercase(Locale.ROOT))) {
                newFrontBuffer.add(element)
            }
        }

        vaultAccountsFrontBuffer.size.also {
            vaultAccountsFrontBuffer.clear()
            notifyItemRangeRemoved(0, it)
        }

        vaultAccountsFrontBuffer.addAll(newFrontBuffer)
        notifyItemRangeInserted(0, newFrontBuffer.size)
    }


    fun setVaultAccounts(vaultAccounts: Array<VaultAccount>) {
        clearVaultAccounts()

        vaultAccountsBackBuffer.addAll(vaultAccounts)
        vaultAccountsFrontBuffer.addAll(vaultAccounts)
        notifyItemRangeInserted(0, vaultAccountsFrontBuffer.size)
    }

    fun setVaultAccount(vaultAccountIndex: Int, newVaultAccount: VaultAccount) {
        if(vaultAccountIndex >= vaultAccountsBackBuffer.size)
            throw IndexOutOfBoundsException("vaultAccountIndex exceeds ${vaultAccountsBackBuffer.size}")

        vaultAccountsBackBuffer[vaultAccountIndex] = newVaultAccount
        notifyBackBufferChange(vaultAccountIndex, BackBufferChangeType.CHANGED)
    }

    fun setVaultAccountEntries(vaultAccountIndex: Int, newEntries: Map<String, String>) {
        if(vaultAccountIndex >= vaultAccountsBackBuffer.size)
            throw IndexOutOfBoundsException("vaultAccountIndex exceeds ${vaultAccountsBackBuffer.size}")

        vaultAccountsBackBuffer[vaultAccountIndex].AccountEntries = newEntries
    }

    fun getVaultAccounts(): Array<VaultAccount> = vaultAccountsBackBuffer.toTypedArray()

    fun getVaultAccountIndexByName(accountName: String): Int? {
        val foundVaultAccount: VaultAccount? = vaultAccountsBackBuffer.find { vaultAccount ->
            vaultAccount.AccountName == accountName
        }

        return if(foundVaultAccount != null) vaultAccountsFrontBuffer.indexOf(foundVaultAccount)
               else foundVaultAccount
    }

    fun getVaultAccountIndex(vaultAccount: VaultAccount): Int = vaultAccountsBackBuffer.indexOf(vaultAccount)

    fun addVaultAccount(vaultAccount: VaultAccount): Boolean {
        val duplicate: Boolean = vaultAccountsBackBuffer.any { element ->
            element.AccountName == vaultAccount.AccountName
        }

        return if(duplicate) false else {
            vaultAccountsBackBuffer.add(vaultAccount)
            notifyBackBufferChange(vaultAccountsBackBuffer.size - 1, BackBufferChangeType.INSERTED)
            true
        }
    }

    fun removeVaultAccount(vaultAccountIndex: Int) {
        clearAccountNameFilter()

        if(vaultAccountIndex < vaultAccountsBackBuffer.size) {
            vaultAccountsBackBuffer.removeAt(vaultAccountIndex)
        }

        notifyBackBufferChange(vaultAccountIndex, BackBufferChangeType.REMOVED)
    }

    fun clearVaultAccounts() {
        vaultAccountsBackBuffer.clear()

        vaultAccountsFrontBuffer.size.also {
            vaultAccountsFrontBuffer.clear()
            notifyItemRangeRemoved(0, it)
        }
    }


    fun itemAtFrontBuffer(index: Int): VaultAccount? =
        if(index < vaultAccountsFrontBuffer.size) vaultAccountsFrontBuffer[index] else null

    fun itemAtBackBuffer(index: Int): VaultAccount? =
        if(index < vaultAccountsBackBuffer.size) vaultAccountsBackBuffer[index] else null

    fun clearAccountNameFilter() {
        accountNameFilterString = ""

        vaultAccountsFrontBuffer.size.also {
            vaultAccountsFrontBuffer.clear()
            notifyItemRangeRemoved(0, it)
        }

        vaultAccountsFrontBuffer.addAll(vaultAccountsBackBuffer)
        notifyItemRangeInserted(0, vaultAccountsFrontBuffer.size)
    }

    fun setAccountNameFilter(filterText: String) {
        accountNameFilterString = filterText
        autoPopulateFrontBuffer()
    }

    fun hasAccountWithName(accountName: String): Boolean =
        vaultAccountsBackBuffer.any { vaultAccount -> vaultAccount.AccountName.contentEquals(accountName) }
}