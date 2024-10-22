package org.zotero.android.sync

import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Before
import org.junit.Test

class UrlDetectorTest {
    private val sut = UrlDetector()

    @Before
    fun setUp() {
    }

    @Test
    fun `returns true for valid http domain`() {
        runBlocking {
            val result = sut.isUrl("http://zotero.org")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid http domain and port`() {
        runBlocking {
            val result = sut.isUrl("http://zotero.org:80")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid http domain, port and path`() {
        runBlocking {
            val result = sut.isUrl("http://zotero.org:80/getinvolved")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid http domain, port, path and params`() {
        runBlocking {
            val result = sut.isUrl("http://zotero.org:80/getinvolved?username=test")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid http domain, port, path, params and anchor`() {
        runBlocking {
            val result = sut.isUrl("http://zotero.org:80/getinvolved?username=test#someanchor")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid http subdomain, port, path, params and anchor`() {
        runBlocking {
            val result =
                sut.isUrl("http://forums.zotero.org:80/discussion/comment/477051#Comment_477051")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid https domain`() {
        runBlocking {
            val result = sut.isUrl("https://zotero.org")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid https domain and port`() {
        runBlocking {
            val result = sut.isUrl("https://zotero.org:80")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid https domain, port and path`() {
        runBlocking {
            val result = sut.isUrl("https://zotero.org:80/getinvolved")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid https domain, port, path and params`() {
        runBlocking {
            val result = sut.isUrl("https://zotero.org:80/getinvolved?username=test")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid https domain, port, path, params and anchor`() {
        runBlocking {
            val result = sut.isUrl("https://zotero.org:80/getinvolved?username=test#someanchor")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns true for valid https subdomain, port, path, params and anchor`() {
        runBlocking {
            val result =
                sut.isUrl("https://forums.zotero.org:80/discussion/comment/477051#Comment_477051")
            result shouldBeEqualTo true
        }
    }

    @Test
    fun `returns false for invalid domain and path separator`() {
        runBlocking {
            val result = sut.isUrl("https://forums.zotero.org\\abc")
            result shouldBeEqualTo false
        }
    }

    @Test
    fun `returns false for the url with an no schema`() {
        runBlocking {
            val result = sut.isUrl("forums.zotero.org/thread")
            result shouldBeEqualTo false
        }
    }

}