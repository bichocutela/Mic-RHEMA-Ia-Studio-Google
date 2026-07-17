package com.aistudio.micrhema

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class IbrDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "ibr_progress.db"
        private const val DATABASE_VERSION = 4
        
        private const val TABLE_PROGRESS = "course_progress"
        private const val COLUMN_COURSE_ID = "course_id"
        private const val COLUMN_CHAPTER_ID = "chapter_id"
        private const val COLUMN_LAST_POSITION = "last_position_seconds"
        private const val COLUMN_TOTAL_DURATION = "total_duration_seconds"
        private const val COLUMN_IS_COMPLETED = "is_completed"

        private const val TABLE_BOOKMARKS = "devotional_bookmarks"
        private const val COLUMN_BOOKMARK_ID = "id"
        private const val COLUMN_BOOKMARK_TITLE = "title"
        private const val COLUMN_BOOKMARK_DATE = "date"
        private const val COLUMN_BOOKMARK_VERSE = "verse"
        private const val COLUMN_BOOKMARK_VERSE_REF = "verse_reference"
        private const val COLUMN_BOOKMARK_CONTENT = "content"

        private const val TABLE_CACHED_DEVOTIONALS = "cached_devotionals"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_PROGRESS (
                $COLUMN_COURSE_ID TEXT,
                $COLUMN_CHAPTER_ID TEXT,
                $COLUMN_LAST_POSITION INTEGER,
                $COLUMN_TOTAL_DURATION INTEGER,
                $COLUMN_IS_COMPLETED INTEGER,
                PRIMARY KEY ($COLUMN_COURSE_ID, $COLUMN_CHAPTER_ID)
            )
        """.trimIndent()
        db.execSQL(createTableQuery)

        val createBookmarksTableQuery = """
            CREATE TABLE $TABLE_BOOKMARKS (
                $COLUMN_BOOKMARK_ID TEXT PRIMARY KEY,
                $COLUMN_BOOKMARK_TITLE TEXT,
                $COLUMN_BOOKMARK_DATE TEXT,
                $COLUMN_BOOKMARK_VERSE TEXT,
                $COLUMN_BOOKMARK_VERSE_REF TEXT,
                $COLUMN_BOOKMARK_CONTENT TEXT
            )
        """.trimIndent()
        db.execSQL(createBookmarksTableQuery)

        val createCacheTableQuery = """
            CREATE TABLE $TABLE_CACHED_DEVOTIONALS (
                $COLUMN_BOOKMARK_ID TEXT PRIMARY KEY,
                $COLUMN_BOOKMARK_TITLE TEXT,
                $COLUMN_BOOKMARK_DATE TEXT,
                $COLUMN_BOOKMARK_VERSE TEXT,
                $COLUMN_BOOKMARK_VERSE_REF TEXT,
                $COLUMN_BOOKMARK_CONTENT TEXT
            )
        """.trimIndent()
        db.execSQL(createCacheTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROGRESS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_BOOKMARKS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_CACHED_DEVOTIONALS")
        onCreate(db)
    }

    fun saveCachedDevotionals(devotionals: List<Devotional>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM $TABLE_CACHED_DEVOTIONALS")
            for (devotional in devotionals) {
                val values = ContentValues().apply {
                    put(COLUMN_BOOKMARK_ID, devotional.id)
                    put(COLUMN_BOOKMARK_TITLE, devotional.title)
                    put(COLUMN_BOOKMARK_DATE, devotional.date)
                    put(COLUMN_BOOKMARK_VERSE, devotional.verse)
                    put(COLUMN_BOOKMARK_VERSE_REF, devotional.verseReference)
                    put(COLUMN_BOOKMARK_CONTENT, devotional.content)
                }
                db.insertWithOnConflict(
                    TABLE_CACHED_DEVOTIONALS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        db.close()
    }

    fun getCachedDevotionals(): List<Devotional> {
        val list = mutableListOf<Devotional>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_CACHED_DEVOTIONALS", null)
        
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_ID)
            val titleIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_TITLE)
            val dateIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_DATE)
            val verseIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_VERSE)
            val verseRefIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_VERSE_REF)
            val contentIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_CONTENT)
            
            do {
                val id = if (idIndex != -1) cursor.getString(idIndex) else ""
                val title = if (titleIndex != -1) cursor.getString(titleIndex) else ""
                val date = if (dateIndex != -1) cursor.getString(dateIndex) else ""
                val verse = if (verseIndex != -1) cursor.getString(verseIndex) else ""
                val verseRef = if (verseRefIndex != -1) cursor.getString(verseRefIndex) else ""
                val content = if (contentIndex != -1) cursor.getString(contentIndex) else ""
                
                list.add(
                    Devotional(
                        id = id,
                        title = title,
                        date = date,
                        verse = verse,
                        verseReference = verseRef,
                        content = content
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun saveBookmark(devotional: Devotional) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_BOOKMARK_ID, devotional.id)
            put(COLUMN_BOOKMARK_TITLE, devotional.title)
            put(COLUMN_BOOKMARK_DATE, devotional.date)
            put(COLUMN_BOOKMARK_VERSE, devotional.verse)
            put(COLUMN_BOOKMARK_VERSE_REF, devotional.verseReference)
            put(COLUMN_BOOKMARK_CONTENT, devotional.content)
        }
        db.insertWithOnConflict(
            TABLE_BOOKMARKS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db.close()
    }

    fun removeBookmark(id: String) {
        val db = writableDatabase
        db.delete(TABLE_BOOKMARKS, "$COLUMN_BOOKMARK_ID = ?", arrayOf(id))
        db.close()
    }

    fun isBookmarked(id: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT 1 FROM $TABLE_BOOKMARKS WHERE $COLUMN_BOOKMARK_ID = ?", arrayOf(id))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun getAllBookmarks(): List<Devotional> {
        val list = mutableListOf<Devotional>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_BOOKMARKS", null)
        
        if (cursor.moveToFirst()) {
            val idIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_ID)
            val titleIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_TITLE)
            val dateIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_DATE)
            val verseIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_VERSE)
            val verseRefIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_VERSE_REF)
            val contentIndex = cursor.getColumnIndex(COLUMN_BOOKMARK_CONTENT)
            
            do {
                val id = if (idIndex != -1) cursor.getString(idIndex) else ""
                val title = if (titleIndex != -1) cursor.getString(titleIndex) else ""
                val date = if (dateIndex != -1) cursor.getString(dateIndex) else ""
                val verse = if (verseIndex != -1) cursor.getString(verseIndex) else ""
                val verseRef = if (verseRefIndex != -1) cursor.getString(verseRefIndex) else ""
                val content = if (contentIndex != -1) cursor.getString(contentIndex) else ""
                
                list.add(
                    Devotional(
                        id = id,
                        title = title,
                        date = date,
                        verse = verse,
                        verseReference = verseRef,
                        content = content
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }

    fun saveProgress(progress: IbrProgress) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_COURSE_ID, progress.courseId)
            put(COLUMN_CHAPTER_ID, progress.chapterId)
            put(COLUMN_LAST_POSITION, progress.lastPositionSeconds)
            put(COLUMN_TOTAL_DURATION, progress.totalDurationSeconds)
            put(COLUMN_IS_COMPLETED, if (progress.isCompleted) 1 else 0)
        }
        db.insertWithOnConflict(
            TABLE_PROGRESS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        db.close()
    }

    fun getAllProgress(): List<IbrProgress> {
        val list = mutableListOf<IbrProgress>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM $TABLE_PROGRESS", null)
        
        if (cursor.moveToFirst()) {
            val courseIdIndex = cursor.getColumnIndex(COLUMN_COURSE_ID)
            val chapterIdIndex = cursor.getColumnIndex(COLUMN_CHAPTER_ID)
            val lastPosIndex = cursor.getColumnIndex(COLUMN_LAST_POSITION)
            val totalDurIndex = cursor.getColumnIndex(COLUMN_TOTAL_DURATION)
            val isCompIndex = cursor.getColumnIndex(COLUMN_IS_COMPLETED)
            
            do {
                val courseId = if (courseIdIndex != -1) cursor.getString(courseIdIndex) else ""
                val chapterId = if (chapterIdIndex != -1) cursor.getString(chapterIdIndex) else ""
                val lastPos = if (lastPosIndex != -1) cursor.getInt(lastPosIndex) else 0
                val totalDur = if (totalDurIndex != -1) cursor.getInt(totalDurIndex) else 0
                val isComp = if (isCompIndex != -1) cursor.getInt(isCompIndex) == 1 else false
                
                list.add(
                    IbrProgress(
                        courseId = courseId,
                        chapterId = chapterId,
                        lastPositionSeconds = lastPos,
                        totalDurationSeconds = totalDur,
                        isCompleted = isComp
                    )
                )
            } while (cursor.moveToNext())
        }
        cursor.close()
        db.close()
        return list
    }
}
