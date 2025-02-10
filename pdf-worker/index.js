/*
    ***** BEGIN LICENSE BLOCK *****

    Copyright © 2025 Corporation for Digital Scholarship
                     Vienna, Virginia, USA
                     https://www.zotero.org

    This file is part of Zotero.

    Zotero is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Zotero is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with Zotero.  If not, see <http://www.gnu.org/licenses/>.

    ***** END LICENSE BLOCK *****
*/

var pdfReaderCMapsURL;
var pdfReaderStandardFontsURL;

var pdfWorkerURL;

const recognizerUrl = "https://services.zotero.org/recognizer";

function recognizePdf(isDebug, pdfFileUrl, pdfFileName) {
    if (isDebug) {
        pdfReaderCMapsURL = "file:///data/data/org.zotero.android.debug/files/pdf-worker/cmaps/";
        pdfReaderStandardFontsURL = "file:///data/data/org.zotero.android.debug/files/pdf-worker/standard_fonts/";

        pdfWorkerURL = "file:///data/data/org.zotero.android.debug/files/pdf-worker/worker.js";
    } else {
        pdfReaderCMapsURL = "file:///data/data/org.zotero.android/files/pdf-worker/cmaps/";
        pdfReaderStandardFontsURL = "file:///data/data/org.zotero.android/files/pdf-worker/standard_fonts/";

        pdfWorkerURL = "file:///data/data/org.zotero.android/files/pdf-worker/worker.js";
    }

    window.retrieveMetadata(pdfFileUrl, pdfFileName);
}

window.addEventListener('DOMContentLoaded', function() {
    Zotero.Debug.init(1);
});
