package com.psychedelicshayna.krypton

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_account_entry.view.*

class EntryAdapter(
    private val accountEntriesMap: Map<String, String>
) : RecyclerView.Adapter<EntryAdapter.VaultEntryViewHolder>() {
    class VaultEntryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    private val accountEntryPairs: MutableList<Pair<String, String>> =
        mutableListOf<Pair<String, String>>().apply {
            addAll(accountEntriesMap.map { entry ->
                Pair(entry.key, entry.value)
            })
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaultEntryViewHolder {
        return VaultEntryViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_account_entry,
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: EntryAdapter.VaultEntryViewHolder, position: Int) {
        holder.itemView.apply {
            tvAccountEntryName.text = accountEntryPairs[position].first
            tvAccountEntryValue.text = accountEntryPairs[position].second
        }
    }

    override fun getItemCount(): Int {
        return accountEntryPairs.size
    }

    fun hasEntryWithName(entryName: String): Boolean {
        return accountEntryPairs.find { kvPair -> kvPair.first == entryName } != null
    }

    fun hasEntryWithValue(entryValue: String): Boolean {
        return accountEntryPairs.find { kvPair -> kvPair.second == entryValue } != null
    }

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