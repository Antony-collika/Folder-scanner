package com.example.fts.domain.diff

import com.example.fts.data.model.Diff
import com.example.fts.data.model.DiffItem
import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.ModifiedItem
import com.example.fts.data.model.MovedItem
import com.example.fts.data.model.RenamedItem
import com.example.fts.data.model.Snapshot

object DiffEngine {
    
    fun compare(old: Snapshot, new: Snapshot): Diff {
        val oldMap = mutableMapOf<String, Entry>()
        val newMap = mutableMapOf<String, Entry>()
        
        flatten(old.root, oldMap)
        flatten(new.root, newMap)
        
        val added = mutableListOf<DiffItem>()
        val removed = mutableListOf<DiffItem>()
        val modified = mutableListOf<ModifiedItem>()
        val renamed = mutableListOf<RenamedItem>()
        val moved = mutableListOf<MovedItem>()
        
        for ((id, newEntry) in newMap) {
            val oldEntry = oldMap[id]
            if (oldEntry == null) {
                added.add(DiffItem(newEntry.path, newEntry.type))
            } else {
                if (oldEntry.path != newEntry.path) {
                    if (oldEntry.name != newEntry.name) {
                        renamed.add(RenamedItem(id, oldEntry.path, newEntry.path))
                    } else {
                        moved.add(MovedItem(id, oldEntry.path, newEntry.path))
                    }
                }
                
                val changes = mutableMapOf<String, Pair<Any?, Any?>>()
                if (oldEntry.modified != newEntry.modified) {
                    changes["modified"] = oldEntry.modified to newEntry.modified
                }
                if (oldEntry.size != newEntry.size) {
                    changes["size"] = oldEntry.size to newEntry.size
                }
                if (changes.isNotEmpty()) {
                    modified.add(ModifiedItem(newEntry.path, changes))
                }
            }
        }
        
        for ((id, oldEntry) in oldMap) {
            if (id !in newMap) {
                removed.add(DiffItem(oldEntry.path, oldEntry.type))
            }
        }
        
        return Diff(
            fromScannedAt = old.scannedAt,
            toScannedAt = new.scannedAt,
            added = added,
            removed = removed,
            modified = modified,
            renamed = renamed,
            moved = moved
        )
    }
    
    private fun flatten(entry: Entry, map: MutableMap<String, Entry>) {
        if (entry.documentId.isNotEmpty()) {
            map[entry.documentId] = entry
        }
        entry.children?.forEach { flatten(it, map) }
    }
}