/* global CSL */

function getCiteproc(itemsCSL, styleXML, localeXML, lang, format) {
	const retrieveLocale = () => localeXML;
	const itemsLookup = itemsCSL.reduce((acc, item) => { acc[item.id] = item; return acc }, {});
	const retrieveItem = itemId => itemsLookup[itemId];
	const styleMeta = parseStyle(styleXML);

	if(styleMeta.parentStyleId) {
		// normally we'd fetch parent style here but for brevity
		throw new Error("Only independent styles are supported");
	}

	const citeproc = new CSL.Engine(
		{ retrieveLocale, retrieveItem, uppercase_subtitles: styleMeta.isUppercaseSubtitlesStyle },
		styleXML, lang
	);
    citeproc.setOutputFormat(format);
	citeproc.updateItems(itemsCSL.map(item => item.id));

	return { citeproc, styleMeta };
}

function getCitation(citationItems, itemsCSL, styleXML, localeXML, lang, format) { //eslint-disable-line no-unused-vars
	const { citeproc } = getCiteproc(itemsCSL, styleXML, localeXML, lang, format);
    const citation = { citationItems, properties: {} };
	return citeproc.previewCitationCluster(citation, [], [], format);
}

/* Entry function that produced bibliography based on the inputs provided */
function getBibliography(itemsCSL, styleXML, localeXML, lang, format) { //eslint-disable-line no-unused-vars
	const { citeproc, styleMeta } = getCiteproc(itemsCSL, styleXML, localeXML, lang, format);

	if(styleMeta.hasBibliography) {
		const bib = citeproc.makeBibliography();
		return format === 'html' ?
			formatBib(bib) :
			bib[0].bibstart+bib[1].join('')+bib[0].bibend;
	} else {
		// It is possible for style not include bibliography. Both Zbib and Zotero generates a
		// fallback in a form of a formatted list of citations in that case.
		const citations = [];
		itemsCSL.forEach(item => {
			const outList = citeproc.appendCitationCluster({
				'citationItems': [{ 'id': item.id }],
				'properties': {}
			}, true);
			outList.forEach(listItem => {
				citations[listItem[0]] = listItem[1];
			});
		});
		return format === 'html' ?
			`<ol><li>${citations.join('</li><li>')}</li></ol>` :
			citations.join("\r\n");
	}
}

/* Three functions below parse style XML to extract some vital info, namely: if style depends on
 another (parent) style and if style matches a few hardcoded APA-like styles that need extra
 treatment.*/
function getStyleId(styleURL) {
	const matches = styleURL.match(/https?:\/\/www\.zotero\.org\/styles\/([\w-]*)/i);
	return matches[0];
}

// Sentence-case styles that capitalize subtitles like APA
function isUppercaseSubtitlesStyle(styleId) {
	// https://github.com/zotero/zotero/blob/52932b6eb03f72b5fb5591ba52d8e0f4c2ef825f/chrome/content/zotero/xpcom/style.js#L696
	return !!styleId.match(/^apa($|-)|^academy-of-management($|-)|^(freshwater-science)/);
}

function parseStyle(citationStyleXml) {
	const parser = new DOMParser();
	const xmlDoc = parser.parseFromString(citationStyleXml, 'application/xml');

	const styleURL = xmlDoc.querySelector('info > id').textContent;
	const styleId = getStyleId(styleURL);
	const hasBibliography = xmlDoc.querySelector('style > bibliography') !== null;
	const parentStyleURL = xmlDoc.querySelector('info > link[rel="independent-parent"]')?.getAttribute('href')
	const parentStyleId = parentStyleURL ? getStyleId(parentStyleURL) : null;


	return {
		styleId,
		hasBibliography,
		parentStyleId,
		isUppercaseSubtitlesStyle: isUppercaseSubtitlesStyle(styleId) || (parentStyleId && isUppercaseSubtitlesStyle(parentStyleId))
	};
}

/* Finally function below formats bibliography according to rules provided by citproc */
// adapted from: https://github.com/zotero/zotero/blob/553d2b00d86f852e051a9d76474993cd0575f7cd/chrome/content/zotero/xpcom/cite.js#L140-L274
// zbib version: https://github.com/zotero/bib-web/blob/c6ee3d59a3c8f40247f466e94f5b26d88bc1846b/src/js/cite.js
function formatBib(bib) {
	var output = [
		bib[0].bibstart,
		...bib[1],
		bib[0].bibend
	];

	var maxOffset = parseInt(bib[0].maxoffset, 10);
	var entrySpacing = parseInt(bib[0].entryspacing, 10);
	var lineSpacing = parseInt(bib[0].linespacing, 10);
	var hangingIndent = bib[0].hangingindent;
	var secondFieldAlign = bib[0]['second-field-align'];

	// Validate input
	if(Number.isNaN(maxOffset)) {
		throw 'Invalid maxoffset';
	}
	if(Number.isNaN(entrySpacing)) {
		throw 'Invalid entryspacing';
	}
	if(Number.isNaN(lineSpacing)) {
		throw 'Invalid linespacing';
	}

	const container = document.createElement('div');
	container.innerHTML = output.join('');
	const bibBody = container.firstChild;
	const leftMarginDivs = container.querySelectorAll('.csl-left-margin');
	const rightInlineDivs = container.querySelectorAll('.csl-right-inline');
	const indentDivs = container.querySelectorAll('.csl-indent');
	const isMultiField = !!leftMarginDivs.length;
	// Padding on the label column, which we need to include when
	// calculating offset of right column
	const rightPadding = .5;

	// One of the characters is usually a period, so we can adjust this down a bit
	maxOffset = Math.max(1, maxOffset - 2);

	// Force a minimum line height
	if(lineSpacing <= 1.35) {
		lineSpacing = 1.35;
	}

	var style = bibBody.getAttribute('style') || '';
	style += 'line-height: ' + lineSpacing + '; ';

	if(hangingIndent) {
		if (isMultiField && !secondFieldAlign) {
			throw ('second-field-align=false and hangingindent=true combination is not currently supported');
		}
		// If only one field, apply hanging indent on root
		else if (!isMultiField) {
			style += 'margin-left: 2em; text-indent:-2em;';
		}
	}

	if(style) {
		bibBody.setAttribute('style', style);
	}

	// csl-entry
	const cslEntries = container.querySelectorAll('.csl-entry');
	for(var i=0, n=cslEntries.length; i<n; i++) {
		const cslEntry = cslEntries[i];
		let divStyle = cslEntry.getAttribute('style') || '';

		if(isMultiField) {
			divStyle += 'clear: left; ';
		}

		if(entrySpacing && i !== n - 1) {
			divStyle += 'margin-bottom: ' + entrySpacing + 'em;';
		}

		if(divStyle) {
			cslEntry.setAttribute('style', divStyle);
		}
	}

	// div.csl-left-margin
	for (let leftMarginDiv of leftMarginDivs) {
		let divStyle = leftMarginDiv.getAttribute('style') || '';

		divStyle = 'float: left; padding-right: ' + rightPadding + 'em;';

		// Right-align the labels if aligning second line, since it looks
		// better and we don't need the second line of text to align with
		// the left edge of the label
		if (secondFieldAlign) {
			divStyle += 'text-align: right; width: ' + maxOffset + 'em;';
		}

		leftMarginDiv.setAttribute('style', divStyle);
	}

	// div.csl-right-inline
	for (let rightInlineDiv of rightInlineDivs) {
		let divStyle = rightInlineDiv.getAttribute('style') || '';
		divStyle = 'margin: 0 .4em 0 ' + (secondFieldAlign ? maxOffset + rightPadding : '0') + 'em;';

		if (hangingIndent) {
			divStyle += 'padding-left: 2em; text-indent:-2em;';
		}

		rightInlineDiv.setAttribute('style', divStyle);
	}

	// div.csl-indent
	for (let indentDiv of indentDivs) {
		indentDiv.setAttribute('style', 'margin: .5em 0 0 2em; padding: 0 0 .2em .5em; border-left: 5px solid #ccc;');
	}

	return container.innerHTML;
}
