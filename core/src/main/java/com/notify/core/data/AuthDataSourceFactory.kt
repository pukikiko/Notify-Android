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
        tokenProvider()?.let { token ->
            base.setDefaultRequestProperties(mapOf("Authorization" to "Bearer $token"))
        }
        return base.createDataSource()
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
