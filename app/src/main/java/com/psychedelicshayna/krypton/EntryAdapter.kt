package com.psychedelicshayna.krypton

import android.content.Context
import android.view.*
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.view.iterator
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_account_entry.view.*

class EntryAdapter(
    private val parentContext: Context,
    private val accountEntriesMap: Map<String, String>
) : RecyclerView.Adapter<EntryAdapter.EntryItemViewHolder>() {
    class EntryItemViewHolder(
        itemView: View,
        private val parentContext: Context,
    ) : RecyclerView.ViewHolder(itemView) {
        var onContextMenuItemClickListener: ((MenuItem, Int, ContextMenu?, View?, ContextMenu.ContextMenuInfo?) -> Unit)? = null

        init {
            itemView.setOnCreateContextMenuListener { menu: ContextMenu?, view: View?, menuInfo: ContextMenu.ContextMenuInfo? ->
                menu?.let { contextMenu ->
                    MenuInflater(parentContext).inflate(R.menu.menu_entry_viewer_entry_context_menu, contextMenu)

                    for(menuItem: MenuItem in contextMenu) {
                        menuItem.setOnMenuItemClickListener {
                            onContextMenuItemClickListener?.invoke(it, bindingAdapterPosition, menu, view, menuInfo)
                            return@setOnMenuItemClickListener true
                        }
                    }
                }
            }
        }
    }

    var onCreateViewHolderListener:
            ((ViewGroup, Int) -> Unit)? = null

    var onBindViewHolderListener:
            ((EntryItemViewHolder, Int) -> Unit)? = null

    val accountEntryPairs: MutableList<Pair<String, String>> =
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
            ),
            parentContext
        ).also {
            onCreateViewHolderListener?.invoke(parent, viewType)
        }
    }

    override fun onBindViewHolder(holder: EntryItemViewHolder, position: Int) {
        holder.itemView.apply {
            itemAccountEntryTextViewEntryName.text = accountEntryPairs[position].first
            itemAccountEntryTextViewEntryValue.text = accountEntryPairs[position].second
        }

        onBindViewHolderListener?.invoke(holder, position)
    }

    override fun getItemCount(): Int {
        return accountEntryPairs.size
    }

    operator fun get(index: Int): Pair<String, String>? =
        if(index < accountEntryPairs.size) accountEntryPairs[index] else null

    fun getEntriesMap(): Map<String, String> = accountEntryPairs.associate { it }

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
        accountEntryPairs[accountEntryIndex] = newAccountEntry
        notifyItemChanged(accountEntryIndex)
    }
}