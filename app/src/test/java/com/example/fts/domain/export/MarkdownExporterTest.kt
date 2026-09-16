package com.example.fts.domain.export

import com.example.fts.data.model.Entry
import com.example.fts.data.model.EntryType
import com.example.fts.data.model.MarkdownModel
import com.example.fts.data.model.Snapshot
import com.example.fts.data.model.SnapshotStats
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownExporterTest {
    private fun snapshot(): Snapshot {
        val file = Entry("File_[one].md", EntryType.FILE, "folder/File_[one].md", "file", 100L, 42L, "text/markdown")
        val folder = Entry("folder", EntryType.FOLDER, "folder", "folder", 100L, null, null, mutableListOf(file))
        val root = Entry("root", EntryType.FOLDER, "", "root", 100L, null, null, mutableListOf(folder))
        return Snapshot(rootName = "root", rootUri = "content://root", scannedAt = 100L, root = root, stats = SnapshotStats(2, 2, 1, 42))
    }

    @Test fun allFourModelsAreDistinctAndFollowTheirShape() {
        val s = snapshot()
        val a = MarkdownExporter.export(s, MarkdownModel.A)
        val b = MarkdownExporter.export(s, MarkdownModel.B)
        val c = MarkdownExporter.export(s, MarkdownModel.C)
        val d = MarkdownExporter.export(s, MarkdownModel.D)
        assertTrue(a.contains("## folder")); assertTrue(a.contains("- File_\\[one\\].md"))
        assertTrue(b.contains("## folder")); assertTrue(b.contains("---"))
        assertTrue(c.contains("- root/")); assertTrue(c.contains("  - folder/"))
        assertTrue(d.contains("root/")); assertTrue(d.contains("└── folder/")); assertTrue(d.contains("    └── File_\\[one\\].md"))
    }

    @Test fun markdownEscapesSpecialCharacters() {
        val s = snapshot().copy(rootName = "root_[x]*")
        val md = MarkdownExporter.export(s, MarkdownModel.D)
        assertTrue(md.contains("root_\\[x\\]\\*"))
    }
}
