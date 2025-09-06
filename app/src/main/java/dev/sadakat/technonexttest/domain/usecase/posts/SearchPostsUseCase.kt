package dev.sadakat.technonexttest.domain.usecase.posts

import dev.sadakat.technonexttest.domain.model.Post
import dev.sadakat.technonexttest.domain.repository.PostsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import kotlin.math.max

data class SearchConfig(
    val enableFuzzySearch: Boolean = true,
    val enableSynonymSearch: Boolean = true,
    val enableRankedResults: Boolean = true,
    val maxResults: Int = 50,
    val minQueryLength: Int = 2,
    val boostFavorites: Boolean = true
)

data class SearchResult(
    val post: Post,
    val relevanceScore: Double,
    val matchType: MatchType,
    val highlightedTitle: String,
    val highlightedBody: String
)

enum class MatchType {
    EXACT_TITLE, EXACT_BODY, PARTIAL_TITLE, PARTIAL_BODY, FUZZY_MATCH, SYNONYM_MATCH
}

class SearchPostsUseCase @Inject constructor(
    private val postsRepository: PostsRepository
) {

    private val searchHistory = ConcurrentHashMap<String, Int>()

    private val synonymMap = mapOf(
        "happy" to listOf("joyful", "pleased", "content", "cheerful"),
        "sad" to listOf("unhappy", "depressed", "melancholy", "sorrowful"),
        "good" to listOf("great", "excellent", "amazing", "wonderful"),
        "bad" to listOf("terrible", "awful", "horrible", "poor"),
        "big" to listOf("large", "huge", "massive", "enormous"),
        "small" to listOf("tiny", "little", "mini", "compact")
    )

    operator fun invoke(
        query: String, config: SearchConfig = SearchConfig()
    ): Flow<List<Post>> {

        if (query.isBlank() || query.length < config.minQueryLength) {
            return postsRepository.getAllPosts().map { posts ->
                posts.take(config.maxResults)
            }
        }

        searchHistory[query.lowercase()] = searchHistory.getOrDefault(query.lowercase(), 0) + 1

        return postsRepository.getAllPosts().map { posts ->
            performAdvancedSearch(posts, query, config)
        }
    }

    private fun performAdvancedSearch(
        posts: List<Post>, query: String, config: SearchConfig
    ): List<Post> {

        val searchResults = mutableListOf<SearchResult>()
        val normalizedQuery = query.lowercase().trim()

        posts.forEach { post ->
            val relevanceScore = calculateRelevanceScore(post, normalizedQuery, config)

            if (relevanceScore > 0.0) {
                val matchType = determineMatchType(post, normalizedQuery, config)
                val highlightedTitle = highlightMatches(post.title, query)
                val highlightedBody = highlightMatches(post.body.take(200), query)

                searchResults.add(
                    SearchResult(
                        post = post,
                        relevanceScore = relevanceScore,
                        matchType = matchType,
                        highlightedTitle = highlightedTitle,
                        highlightedBody = highlightedBody
                    )
                )
            }
        }

        return searchResults.sortedWith(compareByDescending<SearchResult> { it.relevanceScore }.thenByDescending { if (it.post.isFavorite && config.boostFavorites) 1 else 0 }
            .thenBy { it.matchType.ordinal }).take(config.maxResults).map { it.post }
    }

    private fun calculateRelevanceScore(
        post: Post, query: String, config: SearchConfig
    ): Double {
        var score = 0.0
        val queryWords = query.split(" ").filter { it.isNotBlank() }

        queryWords.forEach { word ->
            if (post.title.lowercase().contains(word)) {
                score += if (post.title.lowercase() == word) 10.0 else 5.0
            }

            if (post.body.lowercase().contains(word)) {
                score += 2.0
            }

            if (config.enableFuzzySearch) {
                score += calculateFuzzyScore(post.title.lowercase(), word)
                score += calculateFuzzyScore(post.body.lowercase(), word) * 0.5
            }

            if (config.enableSynonymSearch) {
                score += calculateSynonymScore(post, word)
            }
        }

        if (post.isFavorite && config.boostFavorites) {
            score *= 1.5
        }

        val lengthFactor = max(0.1, 1.0 - (post.body.length / 1000.0))
        score *= lengthFactor

        return score
    }

    private fun calculateFuzzyScore(text: String, word: String): Double {
        val words = text.split(" ")
        return words.maxOfOrNull { textWord ->
            val similarity = calculateLevenshteinSimilarity(textWord, word)
            if (similarity > 0.7) similarity * 2.0 else 0.0
        } ?: 0.0
    }

    private fun calculateSynonymScore(post: Post, word: String): Double {
        var score = 0.0
        synonymMap[word]?.forEach { synonym ->
            if (post.title.lowercase().contains(synonym)) {
                score += 1.5
            }
            if (post.body.lowercase().contains(synonym)) {
                score += 0.8
            }
        }
        return score
    }

    private fun calculateLevenshteinSimilarity(str1: String, str2: String): Double {
        val longer = if (str1.length > str2.length) str1 else str2
        val shorter = if (str1.length > str2.length) str2 else str1

        if (longer.isEmpty()) return 1.0

        val distance = levenshteinDistance(longer, shorter)
        return (longer.length - distance) / longer.length.toDouble()
    }

    private fun levenshteinDistance(str1: String, str2: String): Int {
        val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }

        for (i in 0..str1.length) dp[i][0] = i
        for (j in 0..str2.length) dp[0][j] = j

        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + if (str1[i - 1] == str2[j - 1]) 0 else 1
                )
            }
        }

        return dp[str1.length][str2.length]
    }

    private fun determineMatchType(post: Post, query: String, config: SearchConfig): MatchType {
        return when {
            post.title.lowercase() == query -> MatchType.EXACT_TITLE
            post.body.lowercase().contains(query) -> MatchType.EXACT_BODY
            post.title.lowercase().contains(query) -> MatchType.PARTIAL_TITLE
            post.body.lowercase().contains(query) -> MatchType.PARTIAL_BODY
            config.enableSynonymSearch && containsSynonyms(post, query) -> MatchType.SYNONYM_MATCH
            else -> MatchType.FUZZY_MATCH
        }
    }

    private fun containsSynonyms(post: Post, query: String): Boolean {
        return synonymMap[query.lowercase()]?.any { synonym ->
            post.title.lowercase().contains(synonym) || post.body.lowercase().contains(synonym)
        } ?: false
    }

    private fun highlightMatches(text: String, query: String): String {
        val queryWords = query.lowercase().split(" ").filter { it.isNotBlank() }
        var highlighted = text

        queryWords.forEach { word ->
            val regex = Regex("(?i)($word)")
            highlighted = highlighted.replace(regex, "<mark>$1</mark>")
        }

        return highlighted
    }

    fun getSearchAnalytics(): SearchAnalytics {
        val totalSearches = searchHistory.values.sum()
        val topQueries = searchHistory.toList().sortedByDescending { it.second }.take(10)
            .map { SearchQuery(it.first, it.second) }

        return SearchAnalytics(
            totalSearches = totalSearches,
            uniqueQueries = searchHistory.size,
            topQueries = topQueries
        )
    }

    fun getSuggestions(partialQuery: String): List<String> {
        return searchHistory.keys.filter { it.startsWith(partialQuery.lowercase()) }
            .sortedByDescending { searchHistory[it] ?: 0 }.take(5)
    }

    fun clearSearchHistory() {
        searchHistory.clear()
    }
}

data class SearchAnalytics(
    val totalSearches: Int, val uniqueQueries: Int, val topQueries: List<SearchQuery>
)

data class SearchQuery(
    val query: String, val count: Int
)