package com.example.fts.util

object MarkdownEscape {
    /** Escape only the characters required by the FTS Markdown specification. */
    fun escape(text: String): String = text
        .replace("\\", "\\\\")
        .replace("*", "\\*")
        .replace("_", "\\_")
        .replace("[", "\\[")
        .replace("]", "\\]")
        .replace("`", "\\`")
}