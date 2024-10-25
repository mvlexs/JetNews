/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.jetnews.data.posts.impl

import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import androidx.core.content.ContextCompat.getSystemService
import com.example.jetnews.data.Result
import com.example.jetnews.data.posts.PostsRepository
import com.example.jetnews.model.Post
import com.example.jetnews.model.PostsFeed
import com.example.jetnews.ui.home.HomeUiState
import com.example.jetnews.utils.addOrRemove
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.net.UnknownHostException


/**
 * Implementation of PostsRepository that returns a hardcoded list of
 * posts with resources after some delay in a background thread.
 */


class HttpPostsRepository : PostsRepository {



    val apiUrl = "https://srv.valo-dev.de/public/jetnews/posts.json "

    // for now, store these in memory
    private val favorites = MutableStateFlow<Set<String>>(setOf())

    private val postsFeed = MutableStateFlow<PostsFeed?>(null)

    // Used to make suspend functions that read and update state safe to call from any thread

    override suspend fun getPost(postId: String?): Result<Post> {

        return withContext(Dispatchers.IO) {
            try {
                val client = HttpClient {

                    install(ContentNegotiation){
                        json()
                    }

                }

                val posts: PostsFeed = client.get(apiUrl).body()
                Log.d("DebugMalik",posts.toString())

                val post = posts.allPosts.find { it.id == postId }

                if (post == null) {
                    Result.Error(IllegalArgumentException("Post not found"))
                } else {
                    Result.Success(post)
                }
            } catch (e: Exception) {
                Log.d("ErrorMalik",e.toString())
                Result.Error(e)
            }
        }
    }

    override suspend fun getPostsFeed(): Result<PostsFeed> {
        return withContext(Dispatchers.IO) {

            try {
                val client = HttpClient {

                    install(ContentNegotiation){
                        json()
                    }

                }


                val posts: PostsFeed = client.get(apiUrl).body()

                postsFeed.update { posts }
                Result.Success(posts)
            } catch (e: Exception) {
                Log.d("ErrorMalik",e.toString())
                Result.Error(e)
            }


        }
    }

    override fun observeFavorites(): Flow<Set<String>> = favorites
    override fun observePostsFeed(): Flow<PostsFeed?> = postsFeed

    override suspend fun toggleFavorite(postId: String) {
        favorites.update {
            it.addOrRemove(postId)
        }
    }
}
