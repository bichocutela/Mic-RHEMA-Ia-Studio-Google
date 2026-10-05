package com.aistudio.micrhema

import org.junit.Assert.*
import org.junit.Test

class GoogleDriveServiceTest {
    @Test fun officeCompatibilityLinkDownloadsOriginal() {
        assertEquals("https://drive.google.com/uc?export=download&id=example_123",
            GoogleDriveService.getDirectDownloadLink("https://docs.google.com/document/d/example_123/edit?usp=drivesdk&rtpof=true&sd=true"))
    }

    @Test fun nativeGoogleDocExportsDocxWithOriginalFallback() {
        val candidates = GoogleDriveService.documentDownloadCandidates("https://docs.google.com/document/d/example_123/edit")
        assertEquals(listOf("https://docs.google.com/document/d/example_123/export?format=docx", "https://drive.google.com/uc?export=download&id=example_123"), candidates)
    }

    @Test fun explicitPdfExportIsPreserved() {
        val url = "https://docs.google.com/document/d/example_123/export?format=pdf"
        assertEquals(url, GoogleDriveService.getDirectDownloadLink(url))
    }

    @Test fun protectedLinkResourceKeyIsPreserved() {
        assertEquals("https://drive.google.com/uc?export=download&id=example_123&resourcekey=0-key",
            GoogleDriveService.getDirectDownloadLink("https://drive.google.com/file/d/example_123/view?resourcekey=0-key"))
    }

    @Test fun nonGoogleHostsAndEmbeddedGoogleStringsAreUnchanged() {
        for (url in listOf("https://docs.google.com.example.org/document/d/abc/edit", "https://example.org/?url=https://docs.google.com/document/d/abc/edit", "https://example.org/book.doc")) {
            assertEquals(url, GoogleDriveService.getDirectDownloadLink(url))
        }
    }

    @Test fun queryIdDownloadWorksAndOfficeCandidatesAreDistinct() {
        assertEquals("https://drive.google.com/uc?export=download&id=example_123", GoogleDriveService.getDirectDownloadLink("https://drive.google.com/open?id=example_123"))
        assertEquals(1, GoogleDriveService.documentDownloadCandidates("https://docs.google.com/document/d/example_123/edit?rtpof=true").size)
    }
}
