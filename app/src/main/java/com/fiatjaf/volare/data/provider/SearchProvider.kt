package com.fiatjaf.volare.data.provider

import com.fiatjaf.volare.data.room.dao.MainEventDao
import com.fiatjaf.volare.data.room.view.SimplePostView

class SearchProvider(
    private val topicProvider: TopicProvider,
    private val mainEventDao: MainEventDao,
) {
    private val maxTopicSearchResult = 5
    private val maxPostSearchResult = 15

    suspend fun getTopicSuggestions(text: String): List<String> {
        val stripped = text.stripSearchText()
        return topicProvider.getAllTopics()
            .asSequence()
            .filter { it.contains(other = stripped, ignoreCase = true) }
            .sortedBy { it.length }
            .distinctBy { it.lowercase() }
            .take(maxTopicSearchResult)
            .toList()
    }

    suspend fun getProfileSuggestions(text: String): List<backend.Profile> {
        val trimmed = text.trim()
        // TODO: call backend (should be ok to just list everything and match by text (against name, nip05?)
        //  -- but filter out muted profiles and sort by what?)
        // limit to 10 results?
        return listOf()
    }

    suspend fun getPostSuggestions(text: String): List<SimplePostView> {
        val stripped = text.stripSearchText()

        return mainEventDao.getPostsByContent(content = stripped, limit = maxPostSearchResult)
    }

    fun getStrippedSearchText(text: String) = text.stripSearchText()

    private fun String.stripSearchText(): String {
        return this.trim().dropWhile { it == '#' || it.isWhitespace() }.trim().lowercase()
    }
}
