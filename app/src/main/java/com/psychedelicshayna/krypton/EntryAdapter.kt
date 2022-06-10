package com.psychedelicshayna.krypton

import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_account_entry.view.*
import java.security.KeyStore


class EntryAdapter(
    private val accountEntriesMap: Map<String, String>
) : RecyclerView.Adapter<EntryAdapter.EntryItemViewHolder>() {
    class EntryItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    open class EntryAdapterListener {
        open fun onCreateViewHolderListener(parent: ViewGroup, viewType: Int) = Unit
        open fun onBindViewHolderListener(holder: EntryItemViewHolder, position: Int) = Unit
    }

    var entryAdapterListener: EntryAdapterListener = EntryAdapterListener()

    private val accountEntryPairs: MutableList<Pair<String, String>> =
        mutableListOf<Pair<String, String>>().apply {
            addAll(accountEntriesMap.map { entry ->
                Pair(entry.key, entry.value)
            })
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryItemViewHolder {
        return EntryItemViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_account_entry,
                parent,
                false
            )
        ).also {
            entryAdapterListener.onCreateViewHolderListener(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: EntryItemViewHolder, position: Int) {
        holder.itemView.apply {
            tvAccountEntryName.text = accountEntryPairs[position].first
            tvAccountEntryValue.text = accountEntryPairs[position].second
            entryAdapterListener.onBindViewHolderListener(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return accountEntryPairs.size
    }

    operator fun get(index: Int): Pair<String, String>? =
        if(index < accountEntryPairs.size) accountEntryPairs[index] else null

    fun hasEntryWithName(entryName: String): Boolean =
        accountEntryPairs.any { kvPair -> kvPair.first.contentEquals(entryName) }

    fun hasEntryWithValue(entryValue: String): Boolean =
        accountEntryPairs.any { kvPair -> kvPair.second.contentEquals(entryValue) }

    fun addAccountEntry(entryName: String, entryValue: String) {
        accountEntryPairs.add(Pair(entryName, entryValue))
        notifyItemInserted(accountEntryPairs.size - 1)
    }

    fun removeAccountEntry(index: Int) {
        if(index < accountEntryPairs.size) {
            accountEntryPairs.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun clearAccountEntries() {
        accountEntryPairs.size.apply {
            accountEntryPairs.clear()
            notifyItemRangeRemoved(0, this)
        }
    }

    fun setAccountEntry(accountEntryIndex: Int, newAccountEntry: Pair<String, String>)  {
        accountEntryPairs.apply {
            this[accountEntryIndex] = newAccountEntry
        }
    }
}