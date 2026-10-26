package org.zotero.android.attachmentdownloader

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.ZoteroNoRedirectApi
import org.zotero.android.api.network.CustomResult
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.webdav.WebDavSessionStorage
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class AttachmentDownloadOperationTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val zoteroApi = mockk<ZoteroApi>()
    private val zoteroNoRedirectApi = mockk<ZoteroNoRedirectApi>()
    private val results = Collections.synchronizedList(mutableListOf<CustomResult<Unit>>())

    private lateinit var file: File

    @Before
    fun setUp() {
        file = File(folder.newFolder("KEY"), "document.pdf")
        file.writeText("old content")
        coEvery { zoteroNoRedirectApi.getRequestHeadersApi(any()) } returns Response.success<Void>(null)
    }

    @Test
    fun `failed request keeps the existing local file`() {
        coEvery { zoteroApi.downloadFile(any()) } returns Response.error(404, "Not Found".toResponseBody())

        runOperation()

        file.readText() shouldBeEqualTo "old content"
        directoryContent() shouldBeEqualTo listOf("document.pdf")
        results.single() shouldBeInstanceOf CustomResult.GeneralError::class
    }

    @Test
    fun `interrupted download keeps the existing local file and removes partial download`() {
        coEvery { zoteroApi.downloadFile(any()) } returns Response.success(
            bodyOf(object : Source {
                private var isFirstRead = true

                override fun read(sink: Buffer, byteCount: Long): Long {
                    if (isFirstRead) {
                        isFirstRead = false
                        sink.writeUtf8("part")
                        return 4
                    }
                    throw IOException("connection reset")
                }

                override fun timeout() = Timeout.NONE

                override fun close() {}
            })
        )

        runOperation()

        file.readText() shouldBeEqualTo "old content"
        directoryContent() shouldBeEqualTo listOf("document.pdf")
        results.single() shouldBeInstanceOf CustomResult.GeneralError.CodeError::class
    }

    @Test
    fun `failed request does not leave anything behind for a file that was never downloaded`() {
        file.delete()
        coEvery { zoteroApi.downloadFile(any()) } returns Response.error(404, "Not Found".toResponseBody())

        runOperation()

        directoryContent() shouldBeEqualTo emptyList()
        results.single() shouldBeInstanceOf CustomResult.GeneralError::class
    }

    @Test
    fun `successful download replaces the existing local file`() {
        coEvery { zoteroApi.downloadFile(any()) } returns Response.success("new content".toResponseBody())

        runOperation()

        file.readText() shouldBeEqualTo "new content"
        directoryContent() shouldBeEqualTo listOf("document.pdf")
        results.single() shouldBeInstanceOf CustomResult.GeneralSuccess::class
    }

    @Test
    fun `cancelled download keeps the existing local file and removes partial download`() {
        val secondReadStarted = CountDownLatch(1)
        val release = CountDownLatch(1)
        coEvery { zoteroApi.downloadFile(any()) } returns Response.success(
            bodyOf(object : Source {
                private var isFirstRead = true

                override fun read(sink: Buffer, byteCount: Long): Long {
                    if (isFirstRead) {
                        isFirstRead = false
                        sink.writeUtf8("part")
                        return 4
                    }
                    secondReadStarted.countDown()
                    release.await(5, TimeUnit.SECONDS)
                    return -1
                }

                override fun timeout() = Timeout.NONE

                override fun close() {}
            })
        )
        val operation = operation()

        val job = CoroutineScope(Dispatchers.IO).launch { operation.start(this) }
        secondReadStarted.await(5, TimeUnit.SECONDS) shouldBeEqualTo true
        operation.cancel()
        release.countDown()
        runBlocking { job.join() }

        file.readText() shouldBeEqualTo "old content"
        directoryContent() shouldBeEqualTo listOf("document.pdf")
        results.single() shouldBeInstanceOf CustomResult.GeneralError.CodeError::class
    }

    private fun runOperation() {
        val operation = operation()
        runBlocking { operation.start(this) }
    }

    private fun operation(): AttachmentDownloadOperation {
        val sessionStorage = mockk<WebDavSessionStorage>()
        every { sessionStorage.isEnabled } returns false
        return AttachmentDownloadOperation(
            file = file,
            download = AttachmentDownloader.Download(
                key = "KEY",
                libraryId = LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
            ),
            userId = 1L,
            zoteroApi = zoteroApi,
            zoteroNoRedirectApi = zoteroNoRedirectApi,
            unzipper = mockk(),
            webDavController = mockk(),
            sessionStorage = sessionStorage,
        ).apply {
            finishedDownload = { results.add(it) }
        }
    }

    private fun directoryContent(): List<String> {
        return file.parentFile!!.list()!!.sorted()
    }

    private fun bodyOf(source: Source): ResponseBody = object : ResponseBody() {
        override fun contentType() = null

        override fun contentLength() = 1024L

        override fun source(): BufferedSource = source.buffer()
    }
}