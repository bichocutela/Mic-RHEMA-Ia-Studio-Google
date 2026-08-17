package com.aistudio.micrhema

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

object BibleReadingPreferences {
    private const val PREFS_NAME = "micrhema_bible_reading"
    private const val KEY_HIGHLIGHTS = "highlighted_verses"
    private const val KEY_FAVORITES = "favorite_verses"
    private const val KEY_BOOKMARK_BOOK = "bookmark_book"
    private const val KEY_BOOKMARK_CHAPTER = "bookmark_chapter"
    private const val KEY_BOOKMARK_VERSE = "bookmark_verse"
    private const val KEY_BOOKMARK_VERSION = "bookmark_version"
    private const val KEY_LAST_BOOK = "last_reading_book"
    private const val KEY_LAST_CHAPTER = "last_reading_chapter"
    private const val KEY_LAST_VERSE = "last_reading_verse"
    private const val KEY_LAST_VERSION = "last_reading_version"

    data class Bookmark(
        val book: String,
        val chapter: Int,
        val verse: Int,
        val version: String
    )

    data class ReadingPosition(
        val book: String,
        val chapter: Int,
        val verse: Int,
        val version: String
    )

    fun key(book: String, chapter: Int, verse: Int, version: String): String =
        "$book|$chapter|$verse|$version"

    fun isHighlighted(context: Context, key: String): Boolean =
        getStringSet(context, KEY_HIGHLIGHTS).contains(key)

    fun toggleHighlight(context: Context, key: String): Boolean {
        val values = getStringSet(context, KEY_HIGHLIGHTS)
        val isAdding = values.add(key)
        if (!isAdding) values.remove(key)
        saveStringSet(context, KEY_HIGHLIGHTS, values)
        return isAdding
    }

    fun isFavorite(context: Context, key: String): Boolean =
        getStringSet(context, KEY_FAVORITES).contains(key)

    fun setFavorite(context: Context, key: String, enabled: Boolean) {
        val values = getStringSet(context, KEY_FAVORITES)
        if (enabled) values.add(key) else values.remove(key)
        saveStringSet(context, KEY_FAVORITES, values)
    }

    fun saveBookmark(context: Context, bookmark: Bookmark) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_BOOKMARK_BOOK, bookmark.book)
            .putInt(KEY_BOOKMARK_CHAPTER, bookmark.chapter)
            .putInt(KEY_BOOKMARK_VERSE, bookmark.verse)
            .putString(KEY_BOOKMARK_VERSION, bookmark.version)
            .apply()
    }

    fun getBookmark(context: Context): Bookmark? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val book = prefs.getString(KEY_BOOKMARK_BOOK, null)?.takeIf { it.isNotBlank() } ?: return null
        return Bookmark(
            book = book,
            chapter = prefs.getInt(KEY_BOOKMARK_CHAPTER, 1),
            verse = prefs.getInt(KEY_BOOKMARK_VERSE, 1),
            version = prefs.getString(KEY_BOOKMARK_VERSION, "ARA") ?: "ARA"
        )
    }

    fun clearBookmark(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_BOOKMARK_BOOK)
            .remove(KEY_BOOKMARK_CHAPTER)
            .remove(KEY_BOOKMARK_VERSE)
            .remove(KEY_BOOKMARK_VERSION)
            .apply()
    }

    fun saveLastReading(context: Context, position: ReadingPosition) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_BOOK, position.book)
            .putInt(KEY_LAST_CHAPTER, position.chapter)
            .putInt(KEY_LAST_VERSE, position.verse)
            .putString(KEY_LAST_VERSION, position.version)
            .apply()
    }

    fun getLastReading(context: Context): ReadingPosition? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val book = prefs.getString(KEY_LAST_BOOK, null)?.takeIf { it.isNotBlank() } ?: return null
        return ReadingPosition(
            book = book,
            chapter = prefs.getInt(KEY_LAST_CHAPTER, 1),
            verse = prefs.getInt(KEY_LAST_VERSE, 1),
            version = prefs.getString(KEY_LAST_VERSION, "ARA") ?: "ARA"
        )
    }

    fun clearLastReading(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LAST_BOOK)
            .remove(KEY_LAST_CHAPTER)
            .remove(KEY_LAST_VERSE)
            .remove(KEY_LAST_VERSION)
            .apply()
    }

    fun loadLocalFavoritesIntoState(context: Context) {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("favorite_items", null) ?: return
        runCatching {
            val array = JSONArray(raw)
            val localItems = (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                FavoriteItem(
                    id = item.optString("id"),
                    type = item.optString("type", "bible"),
                    reference = item.optString("reference"),
                    text = item.optString("text"),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis())
                )
            }
            localItems.forEach { item ->
                if (favoriteItemsState.none { it.id == item.id }) favoriteItemsState.add(item)
            }
        }
    }

    fun saveLocalFavorite(context: Context, item: FavoriteItem) {
        val items = readLocalFavorites(context).filterNot { it.id == item.id } + item
        writeLocalFavorites(context, items)
    }

    fun removeLocalFavorite(context: Context, id: String) {
        writeLocalFavorites(context, readLocalFavorites(context).filterNot { it.id == id })
    }

    private fun readLocalFavorites(context: Context): List<FavoriteItem> {
        val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString("favorite_items", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                val item = array.optJSONObject(index) ?: return@mapNotNull null
                FavoriteItem(
                    id = item.optString("id"),
                    type = item.optString("type", "bible"),
                    reference = item.optString("reference"),
                    text = item.optString("text"),
                    timestamp = item.optLong("timestamp", System.currentTimeMillis())
                )
            }
        }.getOrDefault(emptyList())
    }

    private fun writeLocalFavorites(context: Context, items: List<FavoriteItem>) {
        val array = JSONArray()
        items.forEach { item ->
            array.put(
                JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type)
                    put("reference", item.reference)
                    put("text", item.text)
                    put("timestamp", item.timestamp)
                }
            )
        }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString("favorite_items", array.toString())
            .apply()
    }

    private fun getStringSet(context: Context, key: String): MutableSet<String> =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getStringSet(key, emptySet())
            ?.toMutableSet()
            ?: mutableSetOf()

    private fun saveStringSet(context: Context, key: String, values: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putStringSet(key, values.toSet())
            .apply()
    }
}
