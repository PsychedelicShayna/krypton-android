package com.psychedelicshayna.krypton

import android.annotation.SuppressLint
import android.content.Context
import android.text.method.ScrollingMovementMethod
import android.view.*
import android.widget.TextView
import androidx.core.view.iterator
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_vault_account_entry.view.*

class VaultAccountEntryAdapter(
    private val parentContext: Context,
    private val accountEntriesMap: Map<String, String>
) : RecyclerView.Adapter<VaultAccountEntryAdapter.EntryItemViewHolder>() {

    class EntryItemViewHolder(
        val parentContext: Context,
        val representedItemView: View
    ) : RecyclerView.ViewHolder(representedItemView) {

        var onContextMenuItemClickListener:
                ((MenuItem, Int, ContextMenu?, View?, ContextMenu.ContextMenuInfo?) -> Unit)? = null

        private fun onCreateContextMenuListener
                    (menu: ContextMenu?, view: View?, menuInfo: ContextMenu.ContextMenuInfo?) {

            menu?.let { contextMenu ->
                MenuInflater(parentContext).inflate(R.menu.menu_entry_viewer_entry_context_menu, contextMenu)

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
    var onBindViewHolderListener: ((EntryItemViewHolder, Int) -> Unit)? = null

    val accountEntryPairs: MutableList<Pair<String, String>> =
        mutableListOf<Pair<String, String>>().apply {
            addAll(accountEntriesMap.map { entry ->
                Pair(entry.key, entry.value)
            })
        }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EntryItemViewHolder {
        return EntryItemViewHolder(
            parentContext,
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_vault_account_entry,
                parent,
                false
            )
        ).also { viewHolder ->
            onCreateViewHolderListener?.invoke(parent, viewType)

            // Allow TextView responsible for holding entry values to be scrollable.
            viewHolder.representedItemView.findViewById<TextView>(R.id.itemAccountEntryTextViewEntryValue)?.apply {
                movementMethod = ScrollingMovementMethod()

                setOnTouchListener(View.OnTouchListener { view: View?, _ ->
                    view?.parent?.requestDisallowInterceptTouchEvent(true)
                    return@OnTouchListener false
                })
            }
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