function log(message) {
  sendToPort("logHandler", message);
}
let collator = new Intl.Collator(['en-US'], {
    numeric: true,
    sensitivity: 'base'
});
var Zotero = {};
Zotero.debug = (s) => log(s);
Zotero.locale = navigator.language;
Zotero.localeCompare = (a, b) => collator.compare(a, b);

async function getCit(encodedItemsCsl, encodedItemsData, encodedStyleXml, localeId, encodedLocaleXml, format, setToBody, messageId) {
    const styleXml = decodeBase64(encodedStyleXml);
    const localeXml = decodeBase64(encodedLocaleXml);
    const itemsData = JSON.parse(decodeBase64(encodedItemsData));
    const itemsCsl = JSON.parse(decodeBase64(encodedItemsCsl));
    const citation = getCitation(itemsData, itemsCsl, styleXml, localeXml, localeId, format);
    if (setToBody) {
        document.body.innerHTML = citation;
        sendToPort("heightHandler", document.body.scrollHeight);
    }
    sendToPort(citationHandler, {result: citation, id: messageId});
};

async function getBib(encodedItemsCsl, encodedStyleXml, localeId, encodedLocaleXml, format, messageId) {
    const styleXml = decodeBase64(encodedStyleXml);
    const localeXml = decodeBase64(encodedLocaleXml);
    const itemsCsl = JSON.parse(decodeBase64(encodedItemsCsl));
    const bibliography = getBibliography(itemsCsl, styleXml, localeXml, localeId, format);
    sendToPort("bibliographyHandler", {result: bibliography, id: messageId});
};

async function convertItemsToCSL(encodedItemsJson, encodedSchemaJson, encodedDateFormatsJson, messageId) {
    let schemaJson = JSON.parse(decodeBase64(encodedSchemaJson));
    Zotero.Schema.init(schemaJson);
    let dateFormatsJson = JSON.parse(decodeBase64(encodedDateFormatsJson));
    Zotero.Date.init(dateFormatsJson)

    var itemsJson = JSON.parse(decodeBase64(encodedItemsJson));
    let csls = itemsJson.map(Zotero.Utilities.Item.itemToCSLJSON);
    sendToPort("cslHandler", {result: csls, id: messageId});
};

function decodeBase64(base64) {
    const text = window.atob(base64);
    const length = text.length;
    const bytes = new Uint8Array(length);
    for (let i = 0; i < length; i++) {
        bytes[i] = text.charCodeAt(i);
    }
    const decoder = new TextDecoder();
    return decoder.decode(bytes);
}

window.addEventListener('DOMContentLoaded', function() {
    Zotero.Debug.init(1);
});
