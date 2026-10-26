package org.zotero.android.attachmentdownloader

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.zotero.android.api.ZoteroApi
import org.zotero.android.api.ZoteroNoRedirectApi
import org.zotero.android.database.objects.Attachment
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.files.FileStore
import org.zotero.android.sync.LibraryIdentifier
import org.zotero.android.webdav.WebDavSessionStorage
import retrofit2.Response
import java.io.File
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

class AttachmentDownloaderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val zoteroApi = mockk<ZoteroApi>()
    private val updates = LinkedBlockingQueue<AttachmentDownloader.Update>()

    private lateinit var file: File
    private lateinit var sut: AttachmentDownloader

    @Before
    fun setUp() {
        file = File(folder.newFolder("KEY"), "document.pdf")
        file.writeText("old content")

        val zoteroNoRedirectApi = mockk<ZoteroNoRedirectApi>()
        coEvery { zoteroNoRedirectApi.getRequestHeadersApi(any()) } returns Response.success<Void>(null)

        val fileStore = mockk<FileStore>()
        every { fileStore.attachmentFile(any(), any(), any()) } returns file
        every { fileStore.isPdf(any()) } returns true

        val eventStream = mockk<AttachmentDownloaderEventStream>()
        every { eventStream.emitAsync(any()) } answers { updates.add(firstArg()) }

        val sessionStorage = mockk<WebDavSessionStorage>()
        every { sessionStorage.isEnabled } returns false

        sut = AttachmentDownloader(
            zoteroApi = zoteroApi,
            zoteroNoRedirectApi = zoteroNoRedirectApi,
            fileStorage = fileStore,
            dbWrapperMain = mockk(relaxed = true),
            attachmentDownloaderEventStream = eventStream,
            dispatcher = Dispatchers.IO,
            unzipper = mockk(),
            webDavController = mockk(),
            sessionStorage = sessionStorage,
        )
        sut.init(userId = 1L)
    }

    @Test
    fun `failed re-download of a remotely changed attachment reports local copy as ready and local copy is still there`() {
        coEvery { zoteroApi.downloadFile(any()) } returns Response.error(404, "Not Found".toResponseBody())

        sut.downloadIfNeeded(attachment = changedRemotelyAttachment(), parentKey = null)

        awaitFinalUpdate().kind shouldBeEqualTo AttachmentDownloader.Update.Kind.ready
        file.readText() shouldBeEqualTo "old content"
    }

    @Test
    fun `failed re-download does not report ready when local copy is gone`() {
        coEvery { zoteroApi.downloadFile(any()) } coAnswers {
            file.delete()
            Response.error<ResponseBody>(404, "Not Found".toResponseBody())
        }

        sut.downloadIfNeeded(attachment = changedRemotelyAttachment(), parentKey = null)

        awaitFinalUpdate().kind shouldBeInstanceOf AttachmentDownloader.Update.Kind.failed::class
    }

    private fun changedRemotelyAttachment() = Attachment(
        type = Attachment.Kind.file(
            filename = file.name,
            contentType = "application/pdf",
            location = Attachment.FileLocation.localAndChangedRemotely,
            linkType = Attachment.FileLinkType.importedFile
        ),
        title = file.name,
        key = "KEY",
        libraryId = LibraryIdentifier.custom(RCustomLibraryType.myLibrary),
    )

    private fun awaitFinalUpdate(): AttachmentDownloader.Update {
        while (true) {
            val update = updates.poll(5, TimeUnit.SECONDS) ?: error("Downloader did not finish in time")
            if (update.kind !is AttachmentDownloader.Update.Kind.progress) {
                return update
            }
        }
    }
}