package com.nazma_humayra.firebase_journal_app

import androidx.recyclerview.widget.DiffUtil

class JournalDiffCallback(
  private val oldList: List<Journal>,
 private val newList: List<Journal>
) : DiffUtil.Callback() {

    override fun getOldListSize() = oldList.size
  override fun getNewListSize() = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
