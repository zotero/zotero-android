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

const pdfReaderCMapsURL =  "file:///data/data/org.zotero.android.debug/files/pdf-worker/cmaps/";
const pdfReaderStandardFontsURL = "file:///data/data/org.zotero.android.debug/files/pdf-worker/standard_fonts/";
const pdfWorkerURL = "file:///data/data/org.zotero.android.debug/files/pdf-worker/worker.js";

function recognizePdf(pdfFileUrl) {
    fetchLocal(pdfFileUrl).then(function(pdfBytes) {
        const pdfWorker = window.pdfWorker(pdfWorkerURL, pdfReaderCMapsURL, pdfReaderStandardFontsURL);
        const recognizerInputData = pdfWorker.getRecognizerData(pdfBytes);
        recognizerInputData.then(function(data) {
            sendToPort("recognizePdfData", data);
         });
     });
    }

window.addEventListener('DOMContentLoaded', function() {
    Zotero.Debug.init(1);
});
