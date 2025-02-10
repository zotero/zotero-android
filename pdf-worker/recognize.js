
function sendRecognizeStage(stageId, stageData) {
   sendToPort("recognizeStage", {"stageId": stageId, "stageData": stageData});
}

const retrieveMetadata = (pdfFileUrl, pdfFileName) => {
	return async () => {
		try {
			const recognizerData = await getRecognizerData(pdfFileUrl, pdfFileName)();
			await recognizePDF(recognizerData)();
		} catch (error) {
		    const errorMessage = error?.message ?? 'Failed to recognize document';
		    sendRecognizeStage("ERROR_RECOGNIZE_DOCUMENT", errorMessage);
		}
	}
}

// extract metadata from the PDF and send it to the recognizer server
const getRecognizerData = (pdfFileUrl, pdfFileName) => {
	return async () => {
        const data = await fetchLocal(pdfFileUrl);
	    const pdfWorker = window.pdfWorker(pdfWorkerURL, pdfReaderCMapsURL, pdfReaderStandardFontsURL);
		const recognizerInputData = await pdfWorker.getRecognizerData(data);
		recognizerInputData.fileName = pdfFileName;

		const containingTextPages = recognizerInputData.pages.reduce((acc, page) => {
			if (page?.[2]?.length) {
				acc++;
			}
			return acc;
		}, 0);

		if (containingTextPages === 0) {
			// TODO
			throw new Error('PDF does not contain any text');
		}

		const url = `${recognizerUrl}/recognize`;
		const recognizerResponse = await fetch(url, {
			method: 'POST',
			mode: 'cors',
			headers: { 'content-type': 'application/json', },
			body: JSON.stringify(recognizerInputData)
		});
		if (recognizerResponse.ok) {
			return await recognizerResponse.json();
		} else {
			throw new Error('Failed to recognize document');
		}
	}
}


// create item based on data returned from recognizer
const recognizePDF = (recognizerData) => {
	return async () => {
		let identifierPrefix = '';
		let idenfitierValue = '';
		if (recognizerData.arxiv) {
			identifierPrefix = 'arxiv';
			idenfitierValue = recognizerData.arxiv;
		} else if (recognizerData.doi) {
			identifierPrefix = 'DOI';
			idenfitierValue = recognizerData.doi;
		} else if (recognizerData.isbn) {
			identifierPrefix = 'ISBN';
			idenfitierValue = recognizerData.isbn;
		}

		if (identifierPrefix && idenfitierValue) {
			sendRecognizeStage("FINISHED_RECOGNIZE_GOT_ITEM_AND_IDENTIFIER", {"identifier": `${identifierPrefix}: ${idenfitierValue}`, "recognizerData": recognizerData});
			return;
//			try {
//				const translatedItem = await (getItemFromIdentifier(`${identifierPrefix}: ${idenfitierValue}`))();
//				if (translatedItem) {
//					if (!translatedItem.abstractNote && recognizerData.abstract) {
//						translatedItem.abstractNote = recognizerData.abstract;
//					}
//					if (!translatedItem.language && recognizerData.language) {
//						translatedItem.language = recognizerData.language;
//					}
//					if (translatedItem.tags) {
//						translatedItem.tags = translatedItem.tags.map(tag => {
//							if (typeof tag === 'string') {
//								return { tag, type: 1 };
//							}
//							tag.type = 1;
//							return tag;
//						});
//					}
//					return translatedItem;
//				}
//			} catch (e) {
//				// if this fails (e.g. translation server returns 500), we log the error and continue with the fallback using the recognizer data
//				console.error(`Failed to translate identfier (${identifierPrefix}:${idenfitierValue}):\n${e}\n\nFalling back to recognizer data: ` + e);
//			}
		}

		// no identifier found, or translation failed
		if (recognizerData.title) {
			let type = 'journalArticle';

			if (recognizerData.type === 'book-chapter') {
				type = 'bookSection';
			}
			const newItem = {
				itemType: type,
				creators: recognizerData.authors.map(author => ({
					creatorType: 'author', ...pick(author, ['firstName', 'lastName'])
				})),
				title: recognizerData.title,
				abstractNote: recognizerData.abstract,
				date: recognizerData.year,
				libraryCatalog: 'Zotero',
				...pick(recognizerData, ['pages', 'volume', 'url', 'language']),
				...(type === 'journalArticle' ? { issue: recognizerData.issue, issn: recognizerData.ISSN, publicationTitle: recognizerData.container } : {}),
				...(type === 'bookSection' ? { bookTitle: recognizerData.container, publisher: recognizerData.publisher } : {}),
			};
             sendRecognizeStage("FINISHED_RECOGNIZE_NO_IDENTIFIER_USING_FALLBACK_ITEM", newItem);
			 return;
		}
        sendRecognizeStage("FINISHED_RECOGNIZE_GOT_NOTHING", {});
	}
}

export { retrieveMetadata };

window.retrieveMetadata = function (pdfFileUrl, pdfFileName) {
    (async function () {
        await retrieveMetadata(pdfFileUrl, pdfFileName)();
    })();
}
