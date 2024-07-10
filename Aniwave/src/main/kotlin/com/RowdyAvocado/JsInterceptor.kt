package com.RowdyAvocado

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.RowdyAvocado.AniwavePlugin.Companion.postFunction
import com.lagradost.cloudstream3.AcraApplication.Companion.context
import com.lagradost.cloudstream3.USER_AGENT
import com.lagradost.cloudstream3.utils.Coroutines
import com.lagradost.cloudstream3.utils.Coroutines.main
import com.lagradost.nicehttp.requestCreator
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

// Credits
// https://github.com/jmir1/aniyomi-extensions/blob/master/src/en/nineanime/src/eu/kanade/tachiyomi/animeextension/en/nineanime/JsInterceptor.kt
class JsInterceptor(private val serverid: String, private val lang: String) : Interceptor {

    private val handler by lazy { Handler(Looper.getMainLooper()) }
    class JsObject(var payload: String = "") {
        @JavascriptInterface
        fun passPayload(passedPayload: String) {
            payload = passedPayload
        }
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        // val mess = if (serverid == "41") "Vidstream" else if (serverid == "28") "Mcloud" else ""
        val request = chain.request()
        return runBlocking {
            val fixedRequest = resolveWithWebView(request)
            return@runBlocking chain.proceed(fixedRequest ?: request)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(request: Request): Request? {
        val latch = CountDownLatch(1)

        var webView: WebView? = null

        val origRequestUrl = request.url.toString()

        fun destroyWebView() {
            Coroutines.main {
                webView?.stopLoading()
                webView?.destroy()
                webView = null
                println("Destroyed webview")
            }
        }

        // JavaSrcipt gets the Dub or Sub link of vidstream
        val jsScript =
                """
                (function() {
                  var click = document.createEvent('MouseEvents');
                  click.initMouseEvent('click', true, true);
                  document.querySelector('div[data-type="$lang"] ul li[data-sv-id="$serverid"]').dispatchEvent(click);
                })();
        """
        val headers =
                request.headers
                        .toMultimap()
                        .mapValues { it.value.getOrNull(0) ?: "" }
                        .toMutableMap()

        var newRequest: Request? = null

        handler.postFunction {
            val webview = WebView(context!!)
            webView = webview
            with(webview.settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                useWideViewPort = false
                loadWithOverviewMode = false
                userAgentString = USER_AGENT
                blockNetworkImage = true
                webview.webViewClient =
                        object : WebViewClient() {
                            override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                            ): WebResourceResponse? {
                                if (serverid == "41") {
                                    if (!request?.url.toString().contains("vidstream") &&
                                                    !request?.url.toString().contains("vizcloud")
                                    )
                                            return null
                                }
                                if (serverid == "28") {
                                    if (!request?.url.toString().contains("mcloud")) return null
                                }

                                if (request?.url.toString().contains(Regex("list.m3u8|/simple/"))) {
                                    newRequest =
                                            requestCreator(
                                                    "GET",
                                                    request?.url.toString(),
                                                    headers =
                                                            mapOf(
                                                                    "referer" to
                                                                            "/orp.maertsdiv//:sptth".reversed()
                                                            )
                                            )
                                    latch.countDown()
                                    return null
                                }
                                return super.shouldInterceptRequest(view, request)
                            }
                            override fun onPageFinished(view: WebView?, url: String?) {
                                view?.evaluateJavascript(jsScript) {}
                            }
                        }
                webView?.loadUrl(origRequestUrl, headers)
            }
        }

        latch.await(30, TimeUnit.SECONDS)
        handler.postFunction {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
            // context.let { Toast.makeText(it, "Success!", Toast.LENGTH_SHORT).show()}
        }

        var loop = 0
        val totalTime = 60000L

        val delayTime = 100L

        while (loop < totalTime / delayTime) {
            if (newRequest != null) return newRequest
            loop += 1
        }

        println("Web-view timeout after ${totalTime / 1000}s")
        destroyWebView()
        return newRequest
    }
}
