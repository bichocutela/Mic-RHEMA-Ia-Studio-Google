package com.aistudio.micrhema

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import androidx.compose.runtime.mutableStateListOf

object LocalDataManager {
    private const val PREFS_NAME = "micrhema_data_prefs"

    fun loadAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        
        try {
            // Pastor Name
            val pName = prefs.getString("pastorNameState", null)
            if (pName != null) pastorNameState.value = pName
            
            // Pastor Title
            val pTitle = prefs.getString("pastorTitleState", null)
            if (pTitle != null) pastorTitleState.value = pTitle
            
            // Mission Tagline
            val mTagline = prefs.getString("missionTaglineState", null)
            if (mTagline != null) missionTaglineState.value = mTagline
            
            // Rhema Meaning
            val rMeaning = prefs.getString("rhemaMeaningState", null)
            if (rMeaning != null) rhemaMeaningState.value = rMeaning

            // CarouselItems
            val carouselJson = prefs.getString("carouselItemsState", null)
            if (carouselJson != null) {
                val type = object : TypeToken<List<CarouselItem>>() {}.type
                val list: List<CarouselItem> = gson.fromJson(carouselJson, type)
                carouselItemsState.clear()
                carouselItemsState.addAll(list)
            }

            // PrayerRequests
            val prayerJson = prefs.getString("prayerRequestsState", null)
            if (prayerJson != null) {
                val type = object : TypeToken<List<PrayerRequest>>() {}.type
                val list: List<PrayerRequest> = gson.fromJson(prayerJson, type)
                prayerRequestsState.clear()
                prayerRequestsState.addAll(list)
            }

            // Devotionals (only if not empty? Wait, devotionals are loaded from Firestore, but local fallback is good)
            val devJson = prefs.getString("devotionalsState", null)
            if (devJson != null) {
                val type = object : TypeToken<List<Devotional>>() {}.type
                val list: List<Devotional> = gson.fromJson(devJson, type)
                devotionalsState.clear()
                devotionalsState.addAll(list)
            }

            // ChurchServices
            val servicesJson = prefs.getString("weeklyServicesState", null)
            if (servicesJson != null) {
                val type = object : TypeToken<List<ChurchService>>() {}.type
                val list: List<ChurchService> = gson.fromJson(servicesJson, type)
                weeklyServicesState.clear()
                weeklyServicesState.addAll(list)
            }

            // Events
            val eventsJson = prefs.getString("eventsState", null)
            if (eventsJson != null) {
                val type = object : TypeToken<List<ChurchEvent>>() {}.type
                val list: List<ChurchEvent> = gson.fromJson(eventsJson, type)
                eventsState.clear()
                eventsState.addAll(list)
            }

            // MemberRequests
            val membersJson = prefs.getString("memberRequestsState", null)
            if (membersJson != null) {
                val type = object : TypeToken<List<MemberRequest>>() {}.type
                val list: List<MemberRequest> = gson.fromJson(membersJson, type)
                memberRequestsState.clear()
                memberRequestsState.addAll(list)
            }
            
            // IbrCourses
            val ibrJson = prefs.getString("ibrCoursesState", null)
            if (ibrJson != null) {
                val type = object : TypeToken<List<IbrCourse>>() {}.type
                val list: List<IbrCourse> = gson.fromJson(ibrJson, type)
                ibrCoursesState.clear()
                ibrCoursesState.addAll(list)
            }
            
            // ContentBooks
            val booksJson = prefs.getString("contentBooksState", null)
            if (booksJson != null) {
                val type = object : TypeToken<List<ContentBook>>() {}.type
                val list: List<ContentBook> = gson.fromJson(booksJson, type)
                contentBooksState.clear()
                contentBooksState.addAll(list)
            }
            
            // ContentAudios
            val audiosJson = prefs.getString("contentAudiosState", null)
            if (audiosJson != null) {
                val type = object : TypeToken<List<ContentAudio>>() {}.type
                val list: List<ContentAudio> = gson.fromJson(audiosJson, type)
                contentAudiosState.clear()
                contentAudiosState.addAll(list)
            }
            
            // ContentVideos
            val videosJson = prefs.getString("contentVideosState", null)
            if (videosJson != null) {
                val type = object : TypeToken<List<ContentVideo>>() {}.type
                val list: List<ContentVideo> = gson.fromJson(videosJson, type)
                contentVideosState.clear()
                contentVideosState.addAll(list)
            }

            // ContentAlbums
            val albumsJson = prefs.getString("contentAlbumsState", null)
            if (albumsJson != null) {
                val type = object : TypeToken<List<ContentPhotoAlbum>>() {}.type
                val list: List<ContentPhotoAlbum> = gson.fromJson(albumsJson, type)
                contentAlbumsState.clear()
                contentAlbumsState.addAll(list)
            }

            val appTabsJson = prefs.getString("appTabsState", null)
            if (appTabsJson != null) {
                val type = object : com.google.gson.reflect.TypeToken<List<AppTab>>() {}.type
                val list: List<AppTab> = gson.fromJson(appTabsJson, type)
                appTabsState.clear()
                appTabsState.addAll(list)
                
                // Add Bible tab if it doesn't exist (migration)
                if (appTabsState.none { it.systemRoute == "bible" }) {
                    val ibrIdx = appTabsState.indexOfFirst { it.systemRoute == "ibr" }
                    val insertIdx = if (ibrIdx != -1) ibrIdx + 1 else appTabsState.size
                    appTabsState.add(insertIdx, AppTab("bible_tab", "Bíblia", "MenuBook", false, true, false, insertIdx, TabContentType.SYSTEM, "bible"))
                    // Reorder
                    appTabsState.forEachIndexed { index, tab ->
                        val updated = tab.copy(order = index)
                        appTabsState[index] = updated
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("LocalDataManager", "Error loading data", e)
        }
    }

    fun saveAll(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = prefs.edit()
        val gson = Gson()
        
        try {
            editor.putString("pastorNameState", pastorNameState.value)
            editor.putString("pastorTitleState", pastorTitleState.value)
            editor.putString("missionTaglineState", missionTaglineState.value)
            editor.putString("rhemaMeaningState", rhemaMeaningState.value)
            
            editor.putString("carouselItemsState", gson.toJson(carouselItemsState.toList()))
            editor.putString("prayerRequestsState", gson.toJson(prayerRequestsState.toList()))
            editor.putString("devotionalsState", gson.toJson(devotionalsState.toList()))
            editor.putString("weeklyServicesState", gson.toJson(weeklyServicesState.toList()))
            editor.putString("eventsState", gson.toJson(eventsState.toList()))
            editor.putString("memberRequestsState", gson.toJson(memberRequestsState.toList()))
            editor.putString("ibrCoursesState", gson.toJson(ibrCoursesState.toList()))
            
            editor.putString("contentBooksState", gson.toJson(contentBooksState.toList()))
            editor.putString("contentAudiosState", gson.toJson(contentAudiosState.toList()))
            editor.putString("contentVideosState", gson.toJson(contentVideosState.toList()))
            editor.putString("contentAlbumsState", gson.toJson(contentAlbumsState.toList()))
            editor.putString("appTabsState", gson.toJson(appTabsState.toList()))
            
            editor.apply()
        } catch (e: Exception) {
            Log.e("LocalDataManager", "Error saving data", e)
        }
    }
}
