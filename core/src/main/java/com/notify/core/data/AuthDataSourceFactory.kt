package com.notify.core.data

import android.content.Context
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.DefaultHttpDataSource

/** Media3 data source factory that injects the current Notify session token
 *  on every request, so ExoPlayer streams work against private instances. */
class AuthDataSourceFactory(
    private val tokenProvider: () -> String?,
    userAgent: String
) : HttpDataSource.Factory {

    private val base = DefaultHttpDataSource.Factory()
        .setUserAgent(userAgent)
        .setAllowCrossProtocolRedirects(true)
        .setConnectTimeoutMs(20_000)
        // Long read timeout: tracks that are still downloading are live-
        // streamed as the file grows, and a Soulseek peer can take a while to
        // produce the first byte (or stall mid-track). A shorter timeout would
        // kill the stream and skip the track for no reason.
        .setReadTimeoutMs(120_000)

    override fun createDataSource(): HttpDataSource {
        val dataSource = base.createDataSource()
        // Set the token on the *individual* data source, not the shared base
        // factory. Mutating the base's default request properties would leak
        // the previous instance's Authorization header onto later requests
        // (e.g. after a logout or an instance switch to one without a token).
        tokenProvider()?.let { token ->
            dataSource.setRequestProperty("Authorization", "Bearer $token")
        }
        return dataSource
    }

    override fun setDefaultRequestProperties(properties: MutableMap<String, String>): HttpDataSource.Factory {
        base.setDefaultRequestProperties(properties)
        return this
    }

    companion object {
        fun create(context: Context, tokenProvider: () -> String?): AuthDataSourceFactory {
            return AuthDataSourceFactory(tokenProvider, context.packageName)
        }
    }
}
