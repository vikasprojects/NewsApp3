package com.example.newsapp3.repository

import androidx.room.RoomDatabase
import com.example.newsapp3.api.RetrofitInterface
import com.example.newsapp3.db.ArticleDatabase
import com.example.newsapp3.models.Article

class NewsRepository(val db: ArticleDatabase) {
    suspend fun getHeadlines(countryCode: String, pageNumber: Int) =
        RetrofitInterface.api.getHeadlines(countryCode, pageNumber)


    suspend fun searchNews(searchQuery: String, pageNumber: Int) =
        RetrofitInterface.api.searchForNews(searchQuery, pageNumber)


    suspend fun insertArticle(article: Article) {
        db.getarticleDao().insertArticle(article)
    }

    fun getFavouritesNews() = db.getarticleDao().getAllArticles()

    suspend fun deleteNews(article: Article) = db.getarticleDao().deleteArticle(article)
}