package org.zotero.android.api.mappers

import com.google.gson.JsonParser
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldBeInstanceOf
import org.amshove.kluent.shouldNotBeNull
import org.junit.Test
import org.zotero.android.database.objects.RCustomLibraryType
import org.zotero.android.speech.data.ReadAloudResumePosition
import org.zotero.android.sync.LibraryIdentifier

class LastReadAloudPositionResponseMapperTest {

    private val sut = LastReadAloudPositionResponseMapper()

    private val json = JsonParser.parseString(
        """
        {
            "lastReadAloudPosition_u_EB5U2HD8": {
                "value": {
                    "pageIndex": 69,
                    "rects": [[138, 611, 427.3000000000001, 620.3]]
                },
                "version": 27577
            },
            "lastReadAloudPosition_u_AHM6G78W": {
                "value": {
                    "type": "FragmentSelector",
                    "value": "epubcfi(/6/14!/4/2/16/18/6/1,:0,:94)",
                    "conformsTo": "http://www.idpf.org/epub/linking/cfi/epub-cfi.html"
                },
                "version": 27586
            }
        }
        """
    ).asJsonObject

    @Test
    fun `parses page rects and reader source positions`() {
        val positions = sut.fromJson(json).positions
        positions.size shouldBeEqualTo 2

        val pdf = positions.firstOrNull { it.key == "EB5U2HD8" }.shouldNotBeNull()
        pdf.libraryId shouldBeEqualTo LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
        pdf.version shouldBeEqualTo 27577
        val pdfPosition = pdf.value
        pdfPosition shouldBeInstanceOf ReadAloudResumePosition.Pdf::class
        pdfPosition as ReadAloudResumePosition.Pdf
        pdfPosition.pageIndex shouldBeEqualTo 69
        pdfPosition.rects.size shouldBeEqualTo 1
        val rect = pdfPosition.rects.first()
        rect.x shouldBeEqualTo 138.0
        rect.y shouldBeEqualTo 611.0
        (rect.x + rect.width) shouldBeEqualTo 427.3000000000001
        (rect.y + rect.height) shouldBeEqualTo 620.3

        val reader = positions.firstOrNull { it.key == "AHM6G78W" }.shouldNotBeNull()
        reader.libraryId shouldBeEqualTo LibraryIdentifier.custom(RCustomLibraryType.myLibrary)
        reader.version shouldBeEqualTo 27586
        val readerPosition = reader.value
        readerPosition shouldBeInstanceOf ReadAloudResumePosition.Reader::class
        readerPosition as ReadAloudResumePosition.Reader
        readerPosition.source["type"].asString shouldBeEqualTo "FragmentSelector"
        readerPosition.source["value"].asString shouldBeEqualTo "epubcfi(/6/14!/4/2/16/18/6/1,:0,:94)"
    }
}