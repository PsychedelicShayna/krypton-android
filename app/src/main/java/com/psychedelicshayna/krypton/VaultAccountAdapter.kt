package com.psychedelicshayna.krypton

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_vault_account.view.*
import java.lang.IndexOutOfBoundsException
import java.util.*

class VaultAccountAdapter(
    private val vaultAccountsBackBuffer: MutableList<VaultAccount>
) : RecyclerView.Adapter<VaultAccountAdapter.VaultAccountViewHolder>() {
    class VaultAccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    enum class NotifyWhat {
        INSERTED, REMOVED, CHANGED
    }

    private val vaultAccountsFrontBuffer: MutableList<VaultAccount> = mutableListOf()
    private var accountNameFilterString: String = ""

    var vaultAccountViewClickListener: (VaultAccountViewHolder, Int) -> Unit = { _, _ -> }

    init {
        vaultAccountsFrontBuffer.addAll(vaultAccountsBackBuffer)
        notifyItemRangeInserted(0, vaultAccountsFrontBuffer.size)
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
            tvVaultAccountName.text = vaultAccountsFrontBuffer[position].AccountName
            holder.itemView.setOnClickListener {  vaultAccountViewClickListener.invoke(holder, position) }
        }
    }

    override fun getItemCount(): Int {
        return vaultAccountsFrontBuffer.size
    }

    private fun populateFilteredDataset() {
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

    private fun notifyBackBufferChange(index: Int, what: NotifyWhat) {
        if(accountNameFilterString.isNotEmpty()) return populateFilteredDataset()

        when(what) {
            NotifyWhat.INSERTED -> {
                vaultAccountsFrontBuffer.add(index, vaultAccountsBackBuffer[index])
                notifyItemInserted(index)
            }

            NotifyWhat.REMOVED -> {
                vaultAccountsFrontBuffer.removeAt(index)
                notifyItemRemoved(index)
            }

            NotifyWhat.CHANGED -> {
                vaultAccountsFrontBuffer[index] = vaultAccountsBackBuffer[index]
                notifyItemChanged(index)
            }
        }
    }

    fun getVaultAccounts(): Array<VaultAccount> = vaultAccountsBackBuffer.toTypedArray()

    fun addVaultAccount(vaultAccount: VaultAccount): Boolean {
        val duplicate: Boolean = vaultAccountsBackBuffer.any { element ->
            element.AccountName == vaultAccount.AccountName
        }

        return if(duplicate) false else {
            vaultAccountsBackBuffer.add(vaultAccount)
            notifyBackBufferChange(vaultAccountsBackBuffer.size - 1, NotifyWhat.INSERTED)
            true
        }
    }

    fun removeVaultAccount(vaultAccountIndex: Int) {
        clearAccountNameFilter()

        if(vaultAccountIndex < vaultAccountsBackBuffer.size) {
            vaultAccountsBackBuffer.removeAt(vaultAccountIndex)
        }

        notifyBackBufferChange(vaultAccountIndex, NotifyWhat.REMOVED)
    }

    fun clearVaultAccounts() {
        vaultAccountsBackBuffer.clear()

        vaultAccountsFrontBuffer.size.also {
            vaultAccountsFrontBuffer.clear()
            notifyItemRangeRemoved(0, it)
        }
    }

    fun setItemAt(vaultAccountIndex: Int, newVaultAccount: VaultAccount) {
        if(vaultAccountIndex >= vaultAccountsBackBuffer.size)
            throw IndexOutOfBoundsException("vaultAccountIndex exceeds ${vaultAccountsBackBuffer.size}")

        vaultAccountsBackBuffer[vaultAccountIndex] = newVaultAccount
        notifyBackBufferChange(vaultAccountIndex, NotifyWhat.CHANGED)
    }

    fun itemAt(index: Int): VaultAccount? =
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
        populateFilteredDataset()
    }
}