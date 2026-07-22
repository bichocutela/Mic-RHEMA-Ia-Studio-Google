package com.aistudio.micrhema

import android.content.Context
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File

object ExoPlayerCache {
    private var cache: SimpleCache? = null

    fun getCache(context: Context): SimpleCache {
        if (cache == null) {
            val cacheDir = File(context.cacheDir, "media_cache")
            val evictor = LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024) // 100MB
            val databaseProvider = StandaloneDatabaseProvider(context)
            cache = SimpleCache(cacheDir, evictor, databaseProvider)
        }
        return cache!!
    }

    fun getCacheDataSourceFactory(context: Context): CacheDataSource.Factory {
        val simpleCache = getCache(context)
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
        
        return CacheDataSource.Factory()
            .setCache(simpleCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    }
}
