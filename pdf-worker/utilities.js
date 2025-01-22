/*
    ***** BEGIN LICENSE BLOCK *****
    
    Copyright © 2009 Center for History and New Media
                     George Mason University, Fairfax, Virginia, USA
                     http://zotero.org
    
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
    
	
	Utilities based in part on code taken from Piggy Bank 2.1.1 (BSD-licensed)
	
    ***** END LICENSE BLOCK *****
*/

(function() {

function movedToUtilitiesInternal(fnName) {
	return function () {
		if (Zotero.Utilities && Zotero.Utilities.Internal) {
			Zotero.debug(`Zotero.Utilities.${fnName}() is deprecated -- use Zotero.Utilities.Internal.${fnName}() instead`);
			return Zotero.Utilities.Internal[fnName].apply(Zotero.Utilities.Internal, arguments);
		} else {
			throw new Error(`Zotero.Utilities.${fnName}() is only available in the zotero-client codebase`)
		}
	}
}

/**
 * @class Functions for text manipulation and other miscellaneous purposes
 */
var Utilities = {
	/**
	 * Returns a function which will execute `fn` with provided arguments after `delay` milliseconds and not more
	 * than once, if called multiple times. See
	 * http://stackoverflow.com/questions/24004791/can-someone-explain-the-debounce-function-in-javascript
	 * @param fn {Function} function to debounce
	 * @param delay {Integer} number of milliseconds to delay the function execution
	 * @returns {Function}
	 */
	debounce: function(fn, delay=500) {
		var timer = null;
		return function () {
			let args = arguments;
			clearTimeout(timer);
			timer = setTimeout(function () {
				fn.apply(this, args);
			}.bind(this), delay);
		};
	},

	/**
	 *  Creates and returns a new, throttled version of the
	 *  passed function, that, when invoked repeatedly,
	 *  will only actually call the original function at most
	 *  once per every wait milliseconds
	 *
	 *  By default, throttle will execute the function as soon
	 *  as you call it for the first time, and, if you call it
	 *  again any number of times during the wait period, as soon
	 *  as that period is over. If you'd like to disable the
	 *  leading-edge call, pass {leading: false}, and if you'd
	 *  like to disable the execution on the trailing-edge,
	 *  pass {trailing: false}. See
	 *  https://underscorejs.org/#throttle
	 *  https://github.com/jashkenas/underscore/blob/master/underscore.js
	 *  (c) 2009-2018 Jeremy Ashkenas, DocumentCloud and Investigative Reporters & Editors
	 *  Underscore may be freely distributed under the MIT license.
	 *
	 *  @param {Function} func Function to throttle
	 *  @param {Integer} wait Wait period in milliseconds
	 *  @param {Boolean} [options.leading] Call at the beginning of the wait period
	 *  @param {Boolean} [options.trailing] Call at the end of the wait period
	 */
	throttle: function (func, wait, options) {
		var context, args, result;
		var timeout = null;
		var previous = 0;
		if (!options) options = {};
		var later = function () {
			previous = options.leading === false ? 0 : Date.now();
			timeout = null;
			result = func.apply(context, args);
			if (!timeout) context = args = null;
		};
		return function () {
			var now = Date.now();
			if (!previous && options.leading === false) previous = now;
			var remaining = wait - (now - previous);
			context = this;
			args = arguments;
			if (remaining <= 0 || remaining > wait) {
				if (timeout) {
					clearTimeout(timeout);
					timeout = null;
				}
				previous = now;
				result = func.apply(context, args);
				if (!timeout) context = args = null;
			}
			else if (!timeout && options.trailing !== false) {
				timeout = setTimeout(later, remaining);
			}
			return result;
		};
	},

	/**
	 * Fixes author name capitalization.
	 * Splits names into space-separated parts and only changes parts either in all uppercase
	 * or all lowercase.
	 *
	 * JOHN -> John
	 * GUTIÉRREZ-ALBILLA -> Gutiérrez-Albilla
	 * O'NEAL -> O'Neal
	 * o'neal -> O'Neal
	 * O'neal -> O'neal
	 * John MacGregor O'NEILL -> John MacGregor O'Neill
	 * martha McMiddlename WASHINGTON -> Martha McMiddlename Washington
	 *
	 * @param {String} string Uppercase author name
	 * @return {String} Title-cased author name
	 */
	capitalizeName: function (string) {
		if (!(typeof string === 'string')) {
			return string;
		}
		return string.split(' ')
			.map((part) => {
				if (part.toUpperCase() === part || part.toLowerCase() === part) {
					return Utilities.XRegExp.replace(
						part.toLowerCase(),
						Utilities.XRegExp('(^|[^\\pL])\\pL', 'g'),
						m => m.toUpperCase()
					);
				}
				else {
					return part;
				}
			})
			.join(' ');
	},

	/**
	 * Cleans extraneous punctuation off a creator name and parse into first and last name
	 *
	 * @param {String} author Creator string
	 * @param {String} type Creator type string (e.g., "author" or "editor")
	 * @param {Boolean} useComma Whether the creator string is in inverted (Last, First) format
	 * @return {Object} firstName, lastName, and creatorType
	 */
	cleanAuthor: function(author, type, useComma) {
		var allCaps = 'A-Z' +
			'\u0400-\u042f';		//cyrilic

		var allCapsRe = new RegExp('^[' + allCaps + ']+$');
		var initialRe = new RegExp('^-?[' + allCaps + ']$');

		if(typeof(author) != "string") {
			throw new Error("cleanAuthor: author must be a string");
		}

		author = author.replace(/^[\s\u00A0\.\,\/\[\]\:]+/, '')
			.replace(/[\s\u00A0\.\,\/\[\]\:]+$/, '')
			.replace(/[\s\u00A0]+/, ' ');

		if(useComma) {
			// Add spaces between periods
			author = author.replace(/\.([^ ])/, ". $1");

			var splitNames = author.split(/[,،] ?/);
			if(splitNames.length > 1) {
				var lastName = splitNames[0];
				var firstName = splitNames[1];
			} else {
				var lastName = author;
			}
		} else {
			// Don't parse "Firstname Lastname [Country]" as "[Country], Firstname Lastname"
			var spaceIndex = author.length;
			do {
				spaceIndex = author.lastIndexOf(" ", spaceIndex-1);
				var lastName = author.substring(spaceIndex + 1);
				var firstName = author.substring(0, spaceIndex);
			} while (!Utilities.XRegExp('\\pL').test(lastName[0]) && spaceIndex > 0)
		}

		if(firstName && allCapsRe.test(firstName) &&
			firstName.length < 4 &&
			(firstName.length == 1 || lastName.toUpperCase() != lastName)) {
			// first name is probably initials
			var newFirstName = "";
			for(var i=0; i<firstName.length; i++) {
				newFirstName += " "+firstName[i]+".";
			}
			firstName = newFirstName.substr(1);
		}

		//add periods after all the initials
		if(firstName) {
			var names = firstName.replace(/^[\s\.]+/,'')
				.replace(/[\s\,]+$/,'')
				//remove spaces surronding any dashes
				.replace(/\s*([\u002D\u00AD\u2010-\u2015\u2212\u2E3A\u2E3B])\s*/,'-')
				.split(/(?:[\s\.]+|(?=-))/);
			var newFirstName = '';
			for(var i=0, n=names.length; i<n; i++) {
				newFirstName += names[i];
				if(initialRe.test(names[i])) newFirstName += '.';
				newFirstName += ' ';
			}
			firstName = newFirstName.replace(/ -/g,'-').trim();
		}

		return {firstName:firstName, lastName:lastName, creatorType:type};
	},

	/**
	 * Removes leading and trailing whitespace from a string
	 * @type String
	 */
	trim: function(/**String*/ s) {
		if (typeof(s) != "string") {
			throw new Error("trim: argument must be a string");
		}

		s = s.replace(/^\s+/, "");
		return s.replace(/\s+$/, "");
	},

	/**
	 * Cleans whitespace off a string and replaces multiple spaces with one
	 * @type String
	 */
	trimInternal: function(/**String*/ s) {
		if (typeof(s) != "string") {
			throw new Error("trimInternal: argument must be a string");
		}

		s = s.replace(/[\xA0\r\n\s]+/g, " ");
		return this.trim(s);
	},

	/**
	 * Cleans any non-word non-parenthesis characters off the ends of a string
	 * @type String
	 */
	superCleanString: function(/**String*/ x) {
		if(typeof(x) != "string") {
			throw new Error("superCleanString: argument must be a string");
		}

		var x = x.replace(/^[\x00-\x27\x29-\x2F\x3A-\x40\x5B-\x60\x7B-\x7F\s]+/, "");
		return x.replace(/[\x00-\x28\x2A-\x2F\x3A-\x40\x5B-\x60\x7B-\x7F\s]+$/, "");
	},

	isHTTPURL: function (url, allowNoScheme = false) {
		// From https://stackoverflow.com/a/3809435
		var noSchemeRE = /^[-a-zA-Z0-9@:%._\+~#=]{1,256}\.[a-zA-Z0-9()]{1,6}\b([-a-zA-Z0-9()@:%_\+.~#?&//=]*)/;
		return /^https?:\/\//.test(url)
			|| (allowNoScheme && !url.startsWith('mailto:') && noSchemeRE.test(url));
	},

	/**
	 * Cleans a http url string
	 * @param url {String}
	 * @params tryHttp {Boolean} Attempt prepending 'http://' to the url
	 * @returns {String}
	 */
	cleanURL: function(url, tryHttp=false) {
		url = url.trim();
		if (!url) return false;

		try {
			return Services.io.newURI(url, null, null).spec; // Valid URI if succeeds
		} catch (e) {
			if (e instanceof Components.Exception
				&& e.result == Components.results.NS_ERROR_MALFORMED_URI
			) {
				if (tryHttp && /\w\.\w/.test(url)) {
					// Assume it's a URL missing "http://" part
					try {
						return Services.io.newURI('http://' + url, null, null).spec;
					} catch (e) {}
				}

				Zotero.debug('cleanURL: Invalid URI: ' + url, 2);
				return false;
			}
			throw e;
		}
	},

	/**
	 * Eliminates HTML tags, replacing &lt;br&gt;s with newlines
	 * @type String
	 */
	cleanTags: function(/**String*/ x) {
		if(typeof(x) != "string") {
			throw new Error("cleanTags: argument must be a string");
		}

		x = x.replace(/<br[^>]*>/gi, "\n");
		x = x.replace(/<\/p>/gi, "\n\n");
		return x.replace(/<[^>]+>/g, "");
	},

	extractIdentifiers: function (text) {
		var identifiers = [];
		var foundIDs = new Set(); // keep track of identifiers to avoid duplicates

		// First look for DOIs
		var ids = text.split(/[\s\u00A0]+/); // whitespace + non-breaking space
		var doi;
		for (let id of ids) {
			if ((doi = Zotero.Utilities.cleanDOI(id)) && !foundIDs.has(doi)) {
				identifiers.push({
					DOI: doi
				});
				foundIDs.add(doi);
			}
		}

		// Then try ISBNs
		if (!identifiers.length) {
			// First try replacing dashes
			let ids = text.replace(/[\u002D\u00AD\u2010-\u2015\u2212]+/g, "") // hyphens and dashes
				.toUpperCase();
			let ISBN_RE = /(?:\D|^)(97[89]\d{10}|\d{9}[\dX])(?!\d)/g;
			let isbn;
			while (isbn = ISBN_RE.exec(ids)) {
				isbn = Zotero.Utilities.cleanISBN(isbn[1]);
				if (isbn && !foundIDs.has(isbn)) {
					identifiers.push({
						ISBN: isbn
					});
					foundIDs.add(isbn);
				}
			}

			// Next try spaces
			if (!identifiers.length) {
				ids = ids.replace(/[ \u00A0]+/g, ""); // space + non-breaking space
				while (isbn = ISBN_RE.exec(ids)) {
					isbn = Zotero.Utilities.cleanISBN(isbn[1]);
					if(isbn && !foundIDs.has(isbn)) {
						identifiers.push({
							ISBN: isbn
						});
						foundIDs.add(isbn);
					}
				}
			}
		}

		// Next try arXiv
		if (!identifiers.length) {
			// arXiv identifiers are extracted without version number
			// i.e. 0706.0044v1 is extracted as 0706.0044,
			// because arXiv OAI API doesn't allow to access individual versions
			let arXiv_RE = /((?:[^A-Za-z]|^)([\-A-Za-z\.]+\/\d{7})(?:(v[0-9]+)|)(?!\d))|((?:\D|^)(\d{4}\.\d{4,5})(?:(v[0-9]+)|)(?!\d))/g;
			let m;
			while ((m = arXiv_RE.exec(text))) {
				let arXiv = m[2] || m[5];
				if (arXiv && !foundIDs.has(arXiv)) {
					identifiers.push({arXiv: arXiv});
					foundIDs.add(arXiv);
				}
			}
		}

		// Next, try ADS Bibcodes
		if (!identifiers.length) {
			// regex as in the ADS Bibcode translator
			let adsBibcode_RE = /\b(\d{4}\D\S{13}[A-Z.:])\b/g;
			let adsBibcode;
			while ((adsBibcode = adsBibcode_RE.exec(text)) && !foundIDs.has(adsBibcode)) {
				identifiers.push({
					adsBibcode: adsBibcode[1]
				});
				foundIDs.add(adsBibcode);
			}
		}

		// Finally, try PMID
		if (!identifiers.length) {
			// PMID; right now, the longest PMIDs are 8 digits, so it doesn't seem like we'll
			// need to discriminate for a fairly long time
			let PMID_RE = /(^|\s|,|:)(\d{1,9})(?=\s|,|$)/g;
			let pmid;
			while ((pmid = PMID_RE.exec(text)) && !foundIDs.has(pmid)) {
				identifiers.push({
					PMID: pmid[2]
				});
				foundIDs.add(pmid);
			}
		}

		return identifiers;
	},	

	/**
	 * Strip info:doi prefix and any suffixes from a DOI
	 * @type String
	 */
	cleanDOI: function(/**String**/ x) {
		if(typeof(x) != "string") {
			throw new Error("cleanDOI: argument must be a string");
		}

		var doi = x.match(/10(?:\.[0-9]{4,})?\/[^\s]*[^\s\.,]/);
		return doi ? doi[0] : null;
	},

	/**
	 * Clean and validate ISBN.
	 * Return isbn if valid, otherwise return false
	 * @param {String} isbn
	 * @param {Boolean} [dontValidate=false] Do not validate check digit
	 * @return {String|Boolean} Valid ISBN or false
	 */
	cleanISBN: function(isbnStr, dontValidate) {
		isbnStr = isbnStr.toUpperCase()
			.replace(/[\x2D\xAD\u2010-\u2015\u2043\u2212]+/g, ''); // Ignore dashes
		var isbnRE = /\b(?:97[89]\s*(?:\d\s*){9}\d|(?:\d\s*){9}[\dX])\b/g,
			isbnMatch;
		while(isbnMatch = isbnRE.exec(isbnStr)) {
			var isbn = isbnMatch[0].replace(/\s+/g, '');

			if (dontValidate) {
				return isbn;
			}

			if(isbn.length == 10) {
				// Verify ISBN-10 checksum
				var sum = 0;
				for (var i = 0; i < 9; i++) {
					sum += isbn[i] * (10-i);
				}
				//check digit might be 'X'
				sum += (isbn[9] == 'X')? 10 : isbn[9]*1;

				if (sum % 11 == 0) return isbn;
			} else {
				// Verify ISBN 13 checksum
				var sum = 0;
				for (var i = 0; i < 12; i+=2) sum += isbn[i]*1;	//to make sure it's int
				for (var i = 1; i < 12; i+=2) sum += isbn[i]*3;
				sum += isbn[12]*1; //add the check digit

				if (sum % 10 == 0 ) return isbn;
			}

			isbnRE.lastIndex = isbnMatch.index + 1; // Retry the same spot + 1
		}

		return false;
	},

	/*
	 * Convert ISBN 10 to ISBN 13
	 * @param {String} isbn ISBN 10 or ISBN 13
	 *   cleanISBN
	 * @return {String} ISBN-13
	 */
	toISBN13: function(isbnStr) {
		var isbn;
		if (!(isbn = Utilities.cleanISBN(isbnStr, true))) {
			throw new Error('ISBN not found in "' + isbnStr + '"');
		}

		if (isbn.length == 13) {
			isbn = isbn.substr(0,12); // Strip off check digit and re-calculate it
		} else {
			isbn = '978' + isbn.substr(0,9);
		}

		var sum = 0;
		for (var i = 0; i < 12; i++) {
			sum += isbn[i] * (i%2 ? 3 : 1);
		}

		var checkDigit = 10 - (sum % 10);
		if (checkDigit == 10) checkDigit = 0;

		return isbn + checkDigit;
	},

	/**
	 * Clean and validate ISSN.
	 * Return issn if valid, otherwise return false
	 */
	cleanISSN: function(/**String*/ issnStr) {
		issnStr = issnStr.toUpperCase()
			.replace(/[\x2D\xAD\u2010-\u2015\u2043\u2212]+/g, ''); // Ignore dashes
		var issnRE = /\b(?:\d\s*){7}[\dX]\b/g,
			issnMatch;
		while (issnMatch = issnRE.exec(issnStr)) {
			var issn = issnMatch[0].replace(/\s+/g, '');

			// Verify ISSN checksum
			var sum = 0;
			for (var i = 0; i < 7; i++) {
				sum += issn[i] * (8-i);
			}
			//check digit might be 'X'
			sum += (issn[7] == 'X')? 10 : issn[7]*1;

			if (sum % 11 == 0) {
				return issn.substring(0,4) + '-' + issn.substring(4);
			}

			issnRE.lastIndex = issnMatch.index + 1; // Retry same spot + 1
		}

		return false;
	},

	/**
	 * Convert plain text to HTML by replacing special characters and replacing newlines with BRs or
	 * P tags
	 * @param {String} str Plain text string
	 * @param {Boolean} singleNewlineIsParagraph Whether single newlines should be considered as
	 *     paragraphs. If true, each newline is replaced with a P tag. If false, double newlines
	 *     are replaced with P tags, while single newlines are replaced with BR tags.
	 * @type String
	 */
	text2html: function (/**String**/ str, /**Boolean**/ singleNewlineIsParagraph) {
		str = Utilities.htmlSpecialChars(str);

		// \n => <p>
		if (singleNewlineIsParagraph) {
			str = '<p>'
				+ str.replace(/\n/g, '</p><p>')
					.replace(/  /g, '&nbsp; ')
				+ '</p>';
		}
		// \n\n => <p>, \n => <br/>
		else {
			str = '<p>'
				+ str.replace(/\n\n/g, '</p><p>')
					.replace(/\n/g, '<br/>')
					.replace(/  /g, '&nbsp; ')
				+ '</p>';
		}
		return str.replace(/<p>\s*<\/p>/g, '<p>&nbsp;</p>');
	},

	/**
	 * Encode special XML/HTML characters
	 * Certain entities can be inserted manually:
	 *   <ZOTEROBREAK/> => <br/>
	 *   <ZOTEROHELLIP/> => &#8230;
	 *
	 * @param {String} str
	 * @return {String}
	 */
	htmlSpecialChars: function(str) {
		if (str && typeof str != 'string') {
			Zotero.debug('#htmlSpecialChars: non-string arguments are deprecated. Update your code',
				1, undefined, true);
			str = str.toString();
		}

		if (!str) return '';

		return str
			.replace(/&/g, '&amp;')
			.replace(/"/g, '&quot;')
			.replace(/'/g, '&apos;')
			.replace(/</g, '&lt;')
			.replace(/>/g, '&gt;')
			.replace(/&lt;ZOTERO([^\/]+)\/&gt;/g, function (str, p1, offset, s) {
				switch (p1) {
				case 'BREAK':
					return '<br/>';
				case 'HELLIP':
					return '&#8230;';
				default:
					return p1;
				}
			});
	},

	/**
	 * Decodes HTML entities within a string, returning plain text
	 * @type String
	 */
	"unescapeHTML":new function() {
		var nsIScriptableUnescapeHTML, node;

		return function(/**String*/ str) {
			// If no tags, no need to unescape
			if(str.indexOf("<") === -1 && str.indexOf("&") === -1) return str;

			if(Zotero.isFx && !Zotero.isBookmarklet) {
				// Create a node and use the textContent property to do unescaping where
				// possible, because this approach preserves line endings in the HTML
				if(node === undefined) {
					node = Utilities.Internal.getDOMDocument().createElement("div");
				}

				node.innerHTML = str;
				return node.textContent.replace(/ {2,}/g, " ");
			} else if(Zotero.isNode) {
				let {JSDOM} = require('jsdom');
				let document = (new JSDOM(str)).window.document;
				return document.documentElement.textContent.replace(/ {2,}/g, " ");
			} else {
				if(!node) node = document.createElement("div");
				node.innerHTML = str;
				return ("textContent" in node ? node.textContent : node.innerText).replace(/ {2,}/g, " ");
			}
		};
	},

	/**
	 * Wrap URLs and DOIs in <a href=""> links in plain text
	 *
	 * Ignore URLs preceded by '>', just in case there are already links
	 * @type String
	 */
	autoLink: function (/**String**/ str) {
		// "http://www.google.com."
		// "http://www.google.com. "
		// "<http://www.google.com>" (and other characters, with or without a space after)
		str = str.replace(/([^>])(https?:\/\/[^\s]+)([\."'>:\]\)](\s|$))/g, '$1<a href="$2">$2</a>$3');
		// "http://www.google.com"
		// "http://www.google.com "
		str = str.replace(/([^">])(https?:\/\/[^\s]+)(\s|$)/g, '$1<a href="$2">$2</a>$3');

		// DOI
		str = str.replace(/(doi:[ ]*)(10\.[^\s]+[0-9a-zA-Z])/g, '$1<a href="http://dx.doi.org/$2">$2</a>');
		return str;
	},

	/**
	 * Parses a text string for HTML/XUL markup and returns an array of parts. Currently only finds
	 * HTML links (&lt;a&gt; tags)
	 *
	 * @return {Array} An array of objects with the following form:<br>
	 * <pre>   {
	 *         type: 'text'|'link',
	 *         text: "text content",
	 *         [ attributes: { key1: val [ , key2: val, ...] }
	 *    }</pre>
	 */
	parseMarkup: function(/**String*/ str) {
		var parts = [];
		var splits = str.split(/(<a [^>]+>[^<]*<\/a>)/);

		for(var i=0; i<splits.length; i++) {
			// Link
			if (splits[i].indexOf('<a ') == 0) {
				var matches = splits[i].match(/<a ([^>]+)>([^<]*)<\/a>/);
				if (matches) {
					// Attribute pairs
					var attributes = {};
					var pairs = matches[1].match(/([^ =]+)="([^"]+")/g);
					for(var j=0; j<pairs.length; j++) {
						var keyVal = pairs[j].split(/=/);
						attributes[keyVal[0]] = keyVal[1].substr(1, keyVal[1].length - 2);
					}

					parts.push({
						type: 'link',
						text: matches[2],
						attributes: attributes
					});
					continue;
				}
			}

			parts.push({
				type: 'text',
				text: splits[i]
			});
		}

		return parts;
	},

	/**
	 * Calculates the Levenshtein distance between two strings
	 * @type Number
	 */
	levenshtein: function (/**String*/ a, /**String**/ b) {
		var aLen = a.length;
		var bLen = b.length;

		var arr = new Array(aLen+1);
		var i, j, cost;

		for (i = 0; i <= aLen; i++) {
			arr[i] = new Array(bLen);
			arr[i][0] = i;
		}

		for (j = 0; j <= bLen; j++) {
			arr[0][j] = j;
		}

		for (i = 1; i <= aLen; i++) {
			for (j = 1; j <= bLen; j++) {
				cost = (a[i-1] == b[j-1]) ? 0 : 1;
				arr[i][j] = Math.min(arr[i-1][j] + 1, Math.min(arr[i][j-1] + 1, arr[i-1][j-1] + cost));
			}
		}

		return arr[aLen][bLen];
	},

	/**
	 * Test if an object is empty
	 *
	 * @param {Object} obj
	 * @type Boolean
	 */
	isEmpty: function (obj) {
		for (var i in obj) {
			return false;
		}
		return true;
	},

	/**
	 * Compares an array with another and returns an array with
	 *	the values from array1 that don't exist in array2
	 *
	 * @param	{Array}		array1
	 * @param	{Array}		array2
	 * @param	{Boolean}	useIndex		If true, return an array containing just
	 *										the index of array2's elements;
	 *										otherwise return the values
	 */
	arrayDiff: function(array1, array2, useIndex) {
		if (!Array.isArray(array1)) {
			throw new Error("array1 is not an array (" + array1 + ")");
		}
		if (!Array.isArray(array2)) {
			throw new Error("array2 is not an array (" + array2 + ")");
		}

		var val, pos, vals = [];
		for (var i=0; i<array1.length; i++) {
			val = array1[i];
			pos = array2.indexOf(val);
			if (pos == -1) {
				vals.push(useIndex ? pos : val);
			}
		}
		return vals;
	},


	/**
	 * Determine whether two arrays are identical
	 *
	 * Modified from http://stackoverflow.com/a/14853974
	 *
	 * @return {Boolean}
	 */
	arrayEquals: function (array1, array2) {
		// If either array is a falsy value, return
		if (!array1 || !array2)
			return false;

		// Compare lengths - can save a lot of time
		if (array1.length != array2.length)
			return false;

		for (var i = 0, l=array1.length; i < l; i++) {
			// Check if we have nested arrays
			if (array1[i] instanceof Array && array2[i] instanceof Array) {
				// Recurse into the nested arrays
				if (!this.arrayEquals(array1[i], array2[i])) {
					return false;
				}
			}
			else if (array1[i] != array2[i]) {
				// Warning - two different object instances will never be equal: {x:20} != {x:20}
				return false;
			}
		}
		return true;
	},


	/**
	 * Return new array with values shuffled
	 *
	 * From http://stackoverflow.com/a/6274398
	 *
	 * @param {Array} arr
	 * @return {Array}
	 */
	arrayShuffle: function (array) {
		var counter = array.length, temp, index;

		// While there are elements in the array
		while (counter--) {
			// Pick a random index
			index = (Math.random() * counter) | 0;

			// And swap the last element with it
			temp = array[counter];
			array[counter] = array[index];
			array[index] = temp;
		}

		return array;
	},


	/**
	 * Return new array with duplicate values removed
	 *
	 * @param	{Array}		array
	 * @return	{Array}
	 */
	arrayUnique: function (arr) {
		return [...new Set(arr)];
	},

	/**
	 * Generate a random integer between min and max inclusive
	 *
	 * @param	{Integer}	min
	 * @param	{Integer}	max
	 * @return	{Integer}
	 */
	rand: function (min, max) {
		return Math.floor(Math.random() * (max - min + 1)) + min;
	},

	/**
	 * Parse a page range
	 *
	 * @param {String} Page range to parse
	 * @return {Integer[]} Start and end pages
	 */
	getPageRange: function(pages) {
		const pageRangeRegexp = /^\s*([0-9]+) ?[-\u2013] ?([0-9]+)\s*$/

		var pageNumbers;
		var m = pageRangeRegexp.exec(pages);
		if(m) {
			// A page range
			pageNumbers = [m[1], m[2]];
		} else {
			// Assume start and end are the same
			pageNumbers = [pages, pages];
		}
		return pageNumbers;
	},

	/**
	 * Pads a number or other string with a given string on the left
	 *
	 * @param {String} string String to pad
	 * @param {String} pad String to use as padding
	 * @length {Integer} length Length of new padded string
	 * @type String
	 */
	lpad: function(string, pad, length) {
		string = string ? string + '' : '';
		while(string.length < length) {
			string = pad + string;
		}
		return string;
	},

	/**
	 * Shorten and add an ellipsis to a string if necessary
	 *
	 * @param {String}	str
	 * @param {Integer}	len
	 * @param {Boolean} [wordBoundary=false]
	 * @param {Boolean} [countChars=false]
	 */
	ellipsize: function (str, len, wordBoundary = false, countChars) {
		if (!len) {
			throw ("Length not specified in Utilities.ellipsize()");
		}
		if (str.length <= len) {
			return str;
		}
		var origLen = str.length;
		let radius = Math.min(len, 5);
		if (wordBoundary) {
			let min = len - radius;
			// If next character is a space, include that so we stop at len
			if (str.charAt(len).match(/\s/)) {
				radius++;
			}
			// Remove trailing characters and spaces, up to radius
			str = str.substr(0, min) + str.substr(min, radius).replace(/\W*\s\S*$/, "");
		}
		else {
			str = str.substr(0, len)
		}
		return str + '\u2026' + (countChars ? ' (' + origLen + ' chars)' : '');
	},


	/**
	 * Return the proper plural form of a string
	 *
	 * For now, this is only used for debug output in English.
	 *
	 * @param {Integer} num
	 * @param {String[]|String} forms - If an array, an array of plural forms (e.g., ['object', 'objects']);
	 *     currently only the two English forms are supported, for 1 and 0/many. If a single string,
	 *     's' is added automatically for 0/many.
	 * @return {String}
	 */
	pluralize: function (num, forms) {
		if (typeof forms == 'string') {
			forms = [forms, forms + 's'];
		}
		return num == 1 ? forms[0] : forms[1];
	},


	/**
	 * Port of PHP's number_format()
	 *
	 * MIT Licensed
	 *
	 * From http://kevin.vanzonneveld.net
	 * +   original by: Jonas Raoni Soares Silva (http://www.jsfromhell.com)
	 * +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
	 * +     bugfix by: Michael White (http://getsprink.com)
	 * +     bugfix by: Benjamin Lupton
	 * +     bugfix by: Allan Jensen (http://www.winternet.no)
	 * +    revised by: Jonas Raoni Soares Silva (http://www.jsfromhell.com)
	 * +     bugfix by: Howard Yeend
	 * *     example 1: number_format(1234.5678, 2, '.', '');
	 * *     returns 1: 1234.57
	 */
	numberFormat: function (number, decimals, dec_point, thousands_sep) {
		var n = number, c = isNaN(decimals = Math.abs(decimals)) ? 2 : decimals;
		var d = dec_point == undefined ? "." : dec_point;
		var t = thousands_sep == undefined ? "," : thousands_sep, s = n < 0 ? "-" : "";
		var i = parseInt(n = Math.abs(+n || 0).toFixed(c)) + "", j = (j = i.length) > 3 ? j % 3 : 0;

		return s + (j ? i.substr(0, j) + t : "") + i.substr(j).replace(/(\d{3})(?=\d)/g, "$1" + t) + (c ? d + Math.abs(n - i).toFixed(c).slice(2) : "");
	},

	/**
	 * Cleans a title, converting it to title case and replacing " :" with ":"
	 *
	 * @param {String} string
	 * @param {Boolean} force Forces title case conversion, even if the capitalizeTitles pref is off
	 * @type String
	 */
	capitalizeTitle: function(string, force) {
		const skipWords = ["but", "or", "yet", "so", "for", "and", "nor", "a", "an",
			"the", "at", "by", "from", "in", "into", "of", "on", "to", "with", "up",
			"down", "as"];

		// this may only match a single character
		const delimiterRegexp = /([ \/\u002D\u00AD\u2010-\u2015\u2212\u2E3A\u2E3B])/;

		string = this.trimInternal(string);
		string = string.replace(/ : /g, ": ");
		if (Zotero.Prefs && !Zotero.Prefs.get('capitalizeTitles') && !force) return string;
		if (!string) return "";

		// split words
		var words = string.split(delimiterRegexp);
		var isUpperCase = string.toUpperCase() == string;

		var newString = "";
		var delimiterOffset = words[0].length;
		var lastWordIndex = words.length-1;
		var previousWordIndex = -1;
		for(var i=0; i<=lastWordIndex; i++) {
			// only do manipulation if not a delimiter character
			if(words[i].length != 0 && (words[i].length != 1 || !delimiterRegexp.test(words[i]))) {
				var upperCaseVariant = words[i].toUpperCase();
				var lowerCaseVariant = words[i].toLowerCase();

				// only use if word does not already possess some capitalization
				if(isUpperCase || words[i] == lowerCaseVariant) {
					if(
						// a skip word
						skipWords.indexOf(lowerCaseVariant.replace(/[^a-zA-Z]+/, "")) != -1
						// not first or last word
						&& i != 0 && i != lastWordIndex
						// does not follow a colon
						&& (previousWordIndex == -1 || words[previousWordIndex][words[previousWordIndex].length-1].search(/[:\?!]/)==-1)
					) {
						words[i] = lowerCaseVariant;
					} else {
						// this is not a skip word or comes after a colon;
						// we must capitalize
						// handle punctuation in the beginning, including multiple, as in "¿Qué pasa?"		
						var punct = words[i].match(/^[\'\"¡¿“‘„«\s]+/);
						punct = punct ? punct[0].length+1 : 1;
						words[i] = words[i].length ? words[i].substr(0, punct).toUpperCase() +
							words[i].substr(punct).toLowerCase() : words[i];
					}
				}

				previousWordIndex = i;
			}

			newString += words[i];
		}

		return newString;
	},

	capitalize: function (str) {
		if (typeof str != 'string') throw new Error("Argument must be a string");
		if (!str) return str; // Empty string
		return str[0].toUpperCase() + str.substr(1);
	},

	/**
	 * Replaces accented characters in a string with ASCII equivalents
	 *
	 * @param {String} str
	 * @param {Boolean} [lowercaseOnly]  Limit conversions to lowercase characters
	 *                                   (for improved performance on lowercase input)
	 * @return {String}
	 *
	 * From http://lehelk.com/2011/05/06/script-to-remove-diacritics/
	 */
	removeDiacritics: function (str, lowercaseOnly) {
		// Short-circuit on the most basic input
		if (/^[a-zA-Z0-9_-]*$/.test(str)) return str;

		var map = this._diacriticsRemovalMap.lowercase;
		for (var i=0, len=map.length; i<len; i++) {
			str = str.replace(map[i].letters, map[i].base);
		}

		if (!lowercaseOnly) {
			var map = this._diacriticsRemovalMap.uppercase;
			for (var i=0, len=map.length; i<len; i++) {
				str = str.replace(map[i].letters, map[i].base);
			}
		}

		return str;
	},

	"_diacriticsRemovalMap": {
		uppercase: [
			{'base':'A', 'letters':/[\u0041\u24B6\uFF21\u00C0\u00C1\u00C2\u1EA6\u1EA4\u1EAA\u1EA8\u00C3\u0100\u0102\u1EB0\u1EAE\u1EB4\u1EB2\u0226\u01E0\u00C4\u01DE\u1EA2\u00C5\u01FA\u01CD\u0200\u0202\u1EA0\u1EAC\u1EB6\u1E00\u0104\u023A\u2C6F]/g},
			{'base':'AA','letters':/[\uA732]/g},
			{'base':'AE','letters':/[\u00C6\u01FC\u01E2]/g},
			{'base':'AO','letters':/[\uA734]/g},
			{'base':'AU','letters':/[\uA736]/g},
			{'base':'AV','letters':/[\uA738\uA73A]/g},
			{'base':'AY','letters':/[\uA73C]/g},
			{'base':'B', 'letters':/[\u0042\u24B7\uFF22\u1E02\u1E04\u1E06\u0243\u0182\u0181]/g},
			{'base':'C', 'letters':/[\u0043\u24B8\uFF23\u0106\u0108\u010A\u010C\u00C7\u1E08\u0187\u023B\uA73E]/g},
			{'base':'D', 'letters':/[\u0044\u24B9\uFF24\u1E0A\u010E\u1E0C\u1E10\u1E12\u1E0E\u0110\u018B\u018A\u0189\uA779]/g},
			{'base':'DZ','letters':/[\u01F1\u01C4]/g},
			{'base':'Dz','letters':/[\u01F2\u01C5]/g},
			{'base':'E', 'letters':/[\u0045\u24BA\uFF25\u00C8\u00C9\u00CA\u1EC0\u1EBE\u1EC4\u1EC2\u1EBC\u0112\u1E14\u1E16\u0114\u0116\u00CB\u1EBA\u011A\u0204\u0206\u1EB8\u1EC6\u0228\u1E1C\u0118\u1E18\u1E1A\u0190\u018E]/g},
			{'base':'F', 'letters':/[\u0046\u24BB\uFF26\u1E1E\u0191\uA77B]/g},
			{'base':'G', 'letters':/[\u0047\u24BC\uFF27\u01F4\u011C\u1E20\u011E\u0120\u01E6\u0122\u01E4\u0193\uA7A0\uA77D\uA77E]/g},
			{'base':'H', 'letters':/[\u0048\u24BD\uFF28\u0124\u1E22\u1E26\u021E\u1E24\u1E28\u1E2A\u0126\u2C67\u2C75\uA78D]/g},
			{'base':'I', 'letters':/[\u0049\u24BE\uFF29\u00CC\u00CD\u00CE\u0128\u012A\u012C\u0130\u00CF\u1E2E\u1EC8\u01CF\u0208\u020A\u1ECA\u012E\u1E2C\u0197]/g},
			{'base':'J', 'letters':/[\u004A\u24BF\uFF2A\u0134\u0248]/g},
			{'base':'K', 'letters':/[\u004B\u24C0\uFF2B\u1E30\u01E8\u1E32\u0136\u1E34\u0198\u2C69\uA740\uA742\uA744\uA7A2]/g},
			{'base':'L', 'letters':/[\u004C\u24C1\uFF2C\u013F\u0139\u013D\u1E36\u1E38\u013B\u1E3C\u1E3A\u0141\u023D\u2C62\u2C60\uA748\uA746\uA780]/g},
			{'base':'LJ','letters':/[\u01C7]/g},
			{'base':'Lj','letters':/[\u01C8]/g},
			{'base':'M', 'letters':/[\u004D\u24C2\uFF2D\u1E3E\u1E40\u1E42\u2C6E\u019C]/g},
			{'base':'N', 'letters':/[\u004E\u24C3\uFF2E\u01F8\u0143\u00D1\u1E44\u0147\u1E46\u0145\u1E4A\u1E48\u0220\u019D\uA790\uA7A4]/g},
			{'base':'NJ','letters':/[\u01CA]/g},
			{'base':'Nj','letters':/[\u01CB]/g},
			{'base':'O', 'letters':/[\u004F\u24C4\uFF2F\u00D2\u00D3\u00D4\u1ED2\u1ED0\u1ED6\u1ED4\u00D5\u1E4C\u022C\u1E4E\u014C\u1E50\u1E52\u014E\u022E\u0230\u00D6\u022A\u1ECE\u0150\u01D1\u020C\u020E\u01A0\u1EDC\u1EDA\u1EE0\u1EDE\u1EE2\u1ECC\u1ED8\u01EA\u01EC\u00D8\u01FE\u0186\u019F\uA74A\uA74C]/g},
			{'base':'OE','letters':/[\u0152]/g},
			{'base':'OI','letters':/[\u01A2]/g},
			{'base':'OO','letters':/[\uA74E]/g},
			{'base':'OU','letters':/[\u0222]/g},
			{'base':'P', 'letters':/[\u0050\u24C5\uFF30\u1E54\u1E56\u01A4\u2C63\uA750\uA752\uA754]/g},
			{'base':'Q', 'letters':/[\u0051\u24C6\uFF31\uA756\uA758\u024A]/g},
			{'base':'R', 'letters':/[\u0052\u24C7\uFF32\u0154\u1E58\u0158\u0210\u0212\u1E5A\u1E5C\u0156\u1E5E\u024C\u2C64\uA75A\uA7A6\uA782]/g},
			{'base':'S', 'letters':/[\u0053\u24C8\uFF33\u1E9E\u015A\u1E64\u015C\u1E60\u0160\u1E66\u1E62\u1E68\u0218\u015E\u2C7E\uA7A8\uA784]/g},
			{'base':'T', 'letters':/[\u0054\u24C9\uFF34\u1E6A\u0164\u1E6C\u021A\u0162\u1E70\u1E6E\u0166\u01AC\u01AE\u023E\uA786]/g},
			{'base':'TZ','letters':/[\uA728]/g},
			{'base':'U', 'letters':/[\u0055\u24CA\uFF35\u00D9\u00DA\u00DB\u0168\u1E78\u016A\u1E7A\u016C\u00DC\u01DB\u01D7\u01D5\u01D9\u1EE6\u016E\u0170\u01D3\u0214\u0216\u01AF\u1EEA\u1EE8\u1EEE\u1EEC\u1EF0\u1EE4\u1E72\u0172\u1E76\u1E74\u0244]/g},
			{'base':'V', 'letters':/[\u0056\u24CB\uFF36\u1E7C\u1E7E\u01B2\uA75E\u0245]/g},
			{'base':'VY','letters':/[\uA760]/g},
			{'base':'W', 'letters':/[\u0057\u24CC\uFF37\u1E80\u1E82\u0174\u1E86\u1E84\u1E88\u2C72]/g},
			{'base':'X', 'letters':/[\u0058\u24CD\uFF38\u1E8A\u1E8C]/g},
			{'base':'Y', 'letters':/[\u0059\u24CE\uFF39\u1EF2\u00DD\u0176\u1EF8\u0232\u1E8E\u0178\u1EF6\u1EF4\u01B3\u024E\u1EFE]/g},
			{'base':'Z', 'letters':/[\u005A\u24CF\uFF3A\u0179\u1E90\u017B\u017D\u1E92\u1E94\u01B5\u0224\u2C7F\u2C6B\uA762]/g},
		],

		lowercase: [
			{'base':'a', 'letters':/[\u0061\u24D0\uFF41\u1E9A\u00E0\u00E1\u00E2\u1EA7\u1EA5\u1EAB\u1EA9\u00E3\u0101\u0103\u1EB1\u1EAF\u1EB5\u1EB3\u0227\u01E1\u00E4\u01DF\u1EA3\u00E5\u01FB\u01CE\u0201\u0203\u1EA1\u1EAD\u1EB7\u1E01\u0105\u2C65\u0250]/g},
			{'base':'aa','letters':/[\uA733]/g},
			{'base':'ae','letters':/[\u00E6\u01FD\u01E3]/g},
			{'base':'ao','letters':/[\uA735]/g},
			{'base':'au','letters':/[\uA737]/g},
			{'base':'av','letters':/[\uA739\uA73B]/g},
			{'base':'ay','letters':/[\uA73D]/g},
			{'base':'b', 'letters':/[\u0062\u24D1\uFF42\u1E03\u1E05\u1E07\u0180\u0183\u0253]/g},
			{'base':'c', 'letters':/[\u0063\u24D2\uFF43\u0107\u0109\u010B\u010D\u00E7\u1E09\u0188\u023C\uA73F\u2184]/g},
			{'base':'d', 'letters':/[\u0064\u24D3\uFF44\u1E0B\u010F\u1E0D\u1E11\u1E13\u1E0F\u0111\u018C\u0256\u0257\uA77A]/g},
			{'base':'dz','letters':/[\u01F3\u01C6]/g},
			{'base':'e', 'letters':/[\u0065\u24D4\uFF45\u00E8\u00E9\u00EA\u1EC1\u1EBF\u1EC5\u1EC3\u1EBD\u0113\u1E15\u1E17\u0115\u0117\u00EB\u1EBB\u011B\u0205\u0207\u1EB9\u1EC7\u0229\u1E1D\u0119\u1E19\u1E1B\u0247\u025B\u01DD]/g},
			{'base':'f', 'letters':/[\u0066\u24D5\uFF46\u1E1F\u0192\uA77C]/g},
			{'base':'g', 'letters':/[\u0067\u24D6\uFF47\u01F5\u011D\u1E21\u011F\u0121\u01E7\u0123\u01E5\u0260\uA7A1\u1D79\uA77F]/g},
			{'base':'h', 'letters':/[\u0068\u24D7\uFF48\u0125\u1E23\u1E27\u021F\u1E25\u1E29\u1E2B\u1E96\u0127\u2C68\u2C76\u0265]/g},
			{'base':'hv','letters':/[\u0195]/g},
			{'base':'i', 'letters':/[\u0069\u24D8\uFF49\u00EC\u00ED\u00EE\u0129\u012B\u012D\u00EF\u1E2F\u1EC9\u01D0\u0209\u020B\u1ECB\u012F\u1E2D\u0268\u0131]/g},
			{'base':'j', 'letters':/[\u006A\u24D9\uFF4A\u0135\u01F0\u0249]/g},
			{'base':'k', 'letters':/[\u006B\u24DA\uFF4B\u1E31\u01E9\u1E33\u0137\u1E35\u0199\u2C6A\uA741\uA743\uA745\uA7A3]/g},
			{'base':'l', 'letters':/[\u006C\u24DB\uFF4C\u0140\u013A\u013E\u1E37\u1E39\u013C\u1E3D\u1E3B\u017F\u0142\u019A\u026B\u2C61\uA749\uA781\uA747]/g},
			{'base':'lj','letters':/[\u01C9]/g},
			{'base':'m', 'letters':/[\u006D\u24DC\uFF4D\u1E3F\u1E41\u1E43\u0271\u026F]/g},
			{'base':'n', 'letters':/[\u006E\u24DD\uFF4E\u01F9\u0144\u00F1\u1E45\u0148\u1E47\u0146\u1E4B\u1E49\u019E\u0272\u0149\uA791\uA7A5]/g},
			{'base':'nj','letters':/[\u01CC]/g},
			{'base':'o', 'letters':/[\u006F\u24DE\uFF4F\u00F2\u00F3\u00F4\u1ED3\u1ED1\u1ED7\u1ED5\u00F5\u1E4D\u022D\u1E4F\u014D\u1E51\u1E53\u014F\u022F\u0231\u00F6\u022B\u1ECF\u0151\u01D2\u020D\u020F\u01A1\u1EDD\u1EDB\u1EE1\u1EDF\u1EE3\u1ECD\u1ED9\u01EB\u01ED\u00F8\u01FF\u0254\uA74B\uA74D\u0275]/g},
			{'base':'oe','letters':/[\u0153]/g},
			{'base':'oi','letters':/[\u01A3]/g},
			{'base':'ou','letters':/[\u0223]/g},
			{'base':'oo','letters':/[\uA74F]/g},
			{'base':'p','letters':/[\u0070\u24DF\uFF50\u1E55\u1E57\u01A5\u1D7D\uA751\uA753\uA755]/g},
			{'base':'q','letters':/[\u0071\u24E0\uFF51\u024B\uA757\uA759]/g},
			{'base':'r','letters':/[\u0072\u24E1\uFF52\u0155\u1E59\u0159\u0211\u0213\u1E5B\u1E5D\u0157\u1E5F\u024D\u027D\uA75B\uA7A7\uA783]/g},
			{'base':'s','letters':/[\u0073\u24E2\uFF53\u00DF\u015B\u1E65\u015D\u1E61\u0161\u1E67\u1E63\u1E69\u0219\u015F\u023F\uA7A9\uA785\u1E9B]/g},
			{'base':'t','letters':/[\u0074\u24E3\uFF54\u1E6B\u1E97\u0165\u1E6D\u021B\u0163\u1E71\u1E6F\u0167\u01AD\u0288\u2C66\uA787]/g},
			{'base':'tz','letters':/[\uA729]/g},
			{'base':'u','letters':/[\u0075\u24E4\uFF55\u00F9\u00FA\u00FB\u0169\u1E79\u016B\u1E7B\u016D\u00FC\u01DC\u01D8\u01D6\u01DA\u1EE7\u016F\u0171\u01D4\u0215\u0217\u01B0\u1EEB\u1EE9\u1EEF\u1EED\u1EF1\u1EE5\u1E73\u0173\u1E77\u1E75\u0289]/g},
			{'base':'v','letters':/[\u0076\u24E5\uFF56\u1E7D\u1E7F\u028B\uA75F\u028C]/g},
			{'base':'vy','letters':/[\uA761]/g},
			{'base':'w','letters':/[\u0077\u24E6\uFF57\u1E81\u1E83\u0175\u1E87\u1E85\u1E98\u1E89\u2C73]/g},
			{'base':'x','letters':/[\u0078\u24E7\uFF58\u1E8B\u1E8D]/g},
			{'base':'y','letters':/[\u0079\u24E8\uFF59\u1EF3\u00FD\u0177\u1EF9\u0233\u1E8F\u00FF\u1EF7\u1E99\u1EF5\u01B4\u024F\u1EFF]/g},
			{'base':'z','letters':/[\u007A\u24E9\uFF5A\u017A\u1E91\u017C\u017E\u1E93\u1E95\u01B6\u0225\u0240\u2C6C\uA763]/g}
		]
	},

	/**
	 * Performs a deep copy of a JavaScript object
	 * @param {Object} obj
	 * @return {Object}
	 */
	deepCopy: function(obj) {
		var obj2 = (Array.isArray(obj) ? [] : {});
		for(var i in obj) {
			if(!obj.hasOwnProperty(i)) continue;

			if(typeof obj[i] === "object" && obj[i] !== null) {
				obj2[i] = Utilities.deepCopy(obj[i]);
			} else {
				obj2[i] = obj[i];
			}
		}
		return obj2;
	},

	/**
	 * Find valid creator types for a given item type
	 *
	 * @param {String} type Item type
	 * @return {String[]} Creator types
	 */
	getCreatorsForType: function(type) {
		if(type === "attachment" || type === "note") return [];
		var types = Zotero.CreatorTypes.getTypesForItemType(Zotero.ItemTypes.getID(type));
		var cleanTypes = new Array();
		for(var i=0; i<types.length; i++) {
			cleanTypes.push(types[i].name);
		}
		return cleanTypes;
	},

	/**
	 * Determine whether a given field is valid for a given item type
	 *
	 * @param {String} field Field name
	 * @param {String} type Item type
	 * @type Boolean
	 */
	fieldIsValidForType: function(field, type) {
		return Zotero.ItemFields.isValidForType(field, Zotero.ItemTypes.getID(type));
	},

	/**
	 * Gets a creator type name, localized to the current locale
	 *
	 * @param {String} type Creator type
	 * @param {String} Localized creator type
	 * @type Boolean
	 */
	getLocalizedCreatorType: function(type) {
		try {
			return Zotero.CreatorTypes.getLocalizedString(type);
		} catch(e) {
			return false;
		}
	},

	/**
	 * Escapes metacharacters in a literal so that it may be used in a regular expression
	 */
	quotemeta: function(literal) {
		if(typeof literal !== "string") {
			throw new Error("Argument "+literal+" must be a string in Utilities.quotemeta()");
		}
		const metaRegexp = /[-[\]{}()*+?.\\^$|,#\s]/g;
		return literal.replace(metaRegexp, "\\$&");
	},

	/**
	 * Evaluate an XPath
	 *
	 * @param {element|element[]} elements The element(s) to use as the context for the XPath
	 * @param {String} xpath The XPath expression
	 * @param {Object} [namespaces] An object whose keys represent namespace prefixes, and whose
	 *                              values represent their URIs
	 * @return {element[]} DOM elements matching XPath
	 */
	xpath: function(elements, xpath, namespaces) {
		var nsResolver = null;
		if(namespaces) {
			nsResolver = function(prefix) {
				return namespaces[prefix] || null;
			};
		}

		if(!("length" in elements)) elements = [elements];

		var results = [];
		for(var i=0, n=elements.length; i<n; i++) {
			// For some reason, if elements is wrapped by an object
			// Xray, we won't be able to unwrap the DOMWrapper around
			// the element. So waive the object Xray.
			var maybeWrappedEl = elements.wrappedJSObject ? elements.wrappedJSObject[i] : elements[i];

			// Firefox 5 hack, so we will preserve Fx5DOMWrappers
			var isWrapped = Zotero.Translate.DOMWrapper && Zotero.Translate.DOMWrapper.isWrapped(maybeWrappedEl);
			var element = isWrapped ? Zotero.Translate.DOMWrapper.unwrap(maybeWrappedEl) : maybeWrappedEl;

			// We waived the object Xray above, which will waive the
			// DOM Xray, so make sure we have a DOM Xray wrapper.
			if(Zotero.isFx) {
				element = new XPCNativeWrapper(element);
			}

			if(element.ownerDocument) {
				var rootDoc = element.ownerDocument;
			} else if(element.documentElement) {
				var rootDoc = element;
			} else if(Zotero.isIE && element.documentElement === null) {
				// IE: documentElement may be null if there is a parse error. In this
				// case, we don't match anything to mimic what would happen with DOMParser
				continue;
			} else {
				throw new Error("First argument must be either element(s) or document(s) in Utilities.xpath(elements, '"+xpath+"')");
			}

			if(!Zotero.isIE || "evaluate" in rootDoc) {
				try {
					// This may result in a deprecation warning in the console due to
					// https://bugzilla.mozilla.org/show_bug.cgi?id=674437
					var xpathObject = rootDoc.evaluate(xpath, element, nsResolver, 5 /*ORDERED_NODE_ITERATOR_TYPE*/, null);
				} catch(e) {
					// rethrow so that we get a stack
					throw new Error(e.name+": "+e.message);
				}

				var newEl;
				while(newEl = xpathObject.iterateNext()) {
					// Firefox 5 hack
					results.push(isWrapped ? Zotero.Translate.DOMWrapper.wrapIn(newEl, maybeWrappedEl) : newEl);
				}
			} else if("selectNodes" in element) {
				// We use JavaScript-XPath in IE for HTML documents, but with an XML
				// document, we need to use selectNodes
				if(namespaces) {
					var ieNamespaces = [];
					for(var j in namespaces) {
						if(!j) continue;
						ieNamespaces.push('xmlns:'+j+'="'+Utilities.htmlSpecialChars(namespaces[j])+'"');
					}
					rootDoc.setProperty("SelectionNamespaces", ieNamespaces.join(" "));
				}
				var nodes = element.selectNodes(xpath);
				for(var j=0; j<nodes.length; j++) {
					results.push(nodes[j]);
				}
			} else {
				throw new Error("XPath functionality not available");
			}
		}

		return results;
	},

	/**
	 * Generates a string from the content of nodes matching a given XPath
	 *
	 * @param {element} node The node representing the document and context
	 * @param {String} xpath The XPath expression
	 * @param {Object} [namespaces] An object whose keys represent namespace prefixes, and whose
	 *                              values represent their URIs
	 * @param {String} [delimiter] The string with which to join multiple matching nodes
	 * @return {String|null} DOM elements matching XPath, or null if no elements exist
	 */
	xpathText: function(node, xpath, namespaces, delimiter) {
		var elements = Utilities.xpath(node, xpath, namespaces);
		if(!elements.length) return null;

		var strings = new Array(elements.length);
		for(var i=0, n=elements.length; i<n; i++) {
			var el = elements[i];
			if(el.wrappedJSObject) el = el.wrappedJSObject;
			if(Zotero.Translate.DOMWrapper) el = Zotero.Translate.DOMWrapper.unwrap(el);
			strings[i] =
				(el.nodeType === 2 /*ATTRIBUTE_NODE*/ && "value" in el) ? el.value
					: "textContent" in el ? el.textContent
					: "innerText" in el ? el.innerText
						: "text" in el ? el.text
							: el.nodeValue;
		}

		return strings.join(delimiter !== undefined ? delimiter : ", ");
	},


	/**
	 * Generate a random string of length 'len' (defaults to 8)
	 **/
	randomString: function(len, chars) {
		if (!chars) {
			chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
		}
		if (!len) {
			len = 8;
		}
		var randomstring = '';
		for (var i=0; i<len; i++) {
			var rnum = Math.floor(Math.random() * chars.length);
			randomstring += chars.substring(rnum,rnum+1);
		}
		return randomstring;
	},

	/**
	 * PHP var_dump equivalent for JS
	 *
	 * Adapted from http://binnyva.blogspot.com/2005/10/dump-function-javascript-equivalent-of.html
	 */
	varDump: function(obj,level,maxLevel,parentObjects,path) {
		// Simple dump
		var type = typeof obj;
		if (type == 'number' || type == 'undefined' || type == 'boolean' || obj === null) {
			if (!level) {
				// When dumping these directly, make sure to distinguish them from regular
				// strings as output by Zotero.debug (i.e. no quotes)
				return '===>' + obj + '<=== (' + type + ')';
			}
			else {
				return '' + obj;
			}
		}
		else if (type == 'string' || typeof obj.toJSON == 'function') {
			return JSON.stringify(obj, false, '    ');
		}
		else if (type == 'symbol') {
			return obj.toString();
		}
		else if (type == 'function') {
			var funcStr = ('' + obj).trim();
			if (!level) {
				// Dump function contents as well if only dumping function
				return funcStr;
			}

			// Display [native code] label for native functions, but make it one line
			if (/^[^{]+{\s*\[native code\]\s*}$/i.test(funcStr)) {
				return funcStr.replace(/\s*(\[native code\])\s*/i, ' $1 ');
			}

			// For non-native functions, display an ellipsis
			return ('' + obj).replace(/{[\s\S]*}/, '{...}');
		}
		else if (type != 'object') {
			return '<<Unknown type: ' + type + '>> ' + obj;
		}

		// Don't descend into global object cache for data objects
		if (Zotero.isClient && typeof obj == 'object' && obj instanceof Zotero.DataObject) {
			maxLevel = 1;
		}

		// More complex dump with indentation for objects
		if (level === undefined) {
			level = 0;
		}

		if (maxLevel === undefined) {
			maxLevel = 5;
		}

		var objType = Object.prototype.toString.call(obj);

		if (level > maxLevel) {
			return objType + " <<Maximum depth reached>>";
		}

		// The padding given at the beginning of the line.
		var level_padding = "";
		for (var j=0; j<level+1; j++) {
			level_padding += "    ";
		}

		//Special handling for Error or Exception
		var isException = Zotero.isFx && !Zotero.isBookmarklet && obj instanceof Components.interfaces.nsIException;
		var isError = obj instanceof Error;
		if (!isException && !isError && obj.message !== undefined && obj.stack !== undefined) {
			isError = true;
		}

		if (isError || isException) {
			var header = '';
			if (isError) {
				header = (obj.constructor && obj.constructor.name) ? obj.constructor.name : 'Error';
			} else {
				header = (obj.name ? obj.name + ' ' : '') + 'Exception';
			}

			let msg = (obj.message ? ('' + obj.message).replace(/^/gm, level_padding).trim() : '');
			if (obj.stack) {
				let stack = obj.stack.trim().replace(/^(?=.)/gm, level_padding);
				if (Utilities.Internal) {
					stack = Utilities.Internal.filterStack(stack);
				}

				msg += '\n\n';

				// At least with Zotero.HTTP.UnexpectedStatusException, the stack contains "Error:"
				// and the message in addition to the trace. I'm not sure what's causing that
				// (Bluebird?), but fix it here.
				if (stack.startsWith('Error:')) {
					msg += stack.replace('Error: ' + obj.message + '\n', '');
				}
				else {
					msg += stack;
				}
			}

			return header + ': ' + msg;
		}

		// Only dump single level for Node objects (including document)
		if (Zotero.isFx && !Zotero.isBookmarklet) {
			if (Zotero.platformMajorVersion > 60) {
				if (obj instanceof Node || obj instanceof Window) {
					level = maxLevel;
				}
			}
			else if (obj instanceof Components.interfaces.nsIDOMNode
					|| obj instanceof Components.interfaces.nsIDOMWindow) {
				level = maxLevel;
			}
		}
		// Do the same for cross-context Node and Window objects
		if (obj.hasOwnProperty('nodeType') || objType === '[object Window]') {
			level = maxLevel;
		}

		// Recursion checking
		if(!parentObjects) {
			parentObjects = [obj];
			path = ['ROOT'];
		}

		var dumpedText;
		var rawProps = false;
		var isArray = objType == '[object Array]';
		var dumpKey = key => (typeof key == 'symbol'
			? key.toString()
			: JSON.stringify(key));
		if (isArray) {
			dumpedText = '[';
		}
		else if (objType == '[object Set]') {
			dumpedText = 'Set {';
			obj = Array.from(obj);
		}
		else if (objType == '[object Map]') {
			dumpedText = 'Map {';
			rawProps = true;
			let newObj = {};
			for (let [key, value] of obj.entries()) {
				newObj[dumpKey(key)] = value;
			}
			obj = newObj;
		}
		else if (objType == '[object Object]') {
			dumpedText = '{';
		}
		else {
			dumpedText = objType + ' {';
		}
		for (var prop in obj) {
			dumpedText += '\n' + level_padding
				+ (rawProps ? prop : dumpKey(prop))
				+ ": ";

			try {
				var value = obj[prop];
			} catch(e) {
				dumpedText += "<<Access Denied>>";
				continue;
			}

			// Check for recursion
			if (typeof(value) == 'object') {
				var i = parentObjects.indexOf(value);
				if(i != -1) {
					var parentName = path.slice(0,i+1).join('->');
					dumpedText += "<<Reference to parent object " + parentName + " >>";
					continue;
				}
			}

			try {
				dumpedText += Utilities.varDump(value,level+1,maxLevel,parentObjects.concat([value]),path.concat([prop]));
			} catch(e) {
				dumpedText += "<<Error processing property: " + e.message + " (" + value + ")>>";
			}
		}

		var lastChar = dumpedText.charAt(dumpedText.length - 1);
		if (lastChar != '[' && lastChar != '{') {
			dumpedText += '\n' + level_padding.substr(4);
		}
		dumpedText += isArray ? ']' : '}';

		return dumpedText;
	},

	/**
	 * Adds a string to a given array at a given offset, converted to UTF-8
	 * @param {String} string The string to convert to UTF-8
	 * @param {Array|Uint8Array} array The array to which to add the string
	 * @param {Integer} [offset] Offset at which to add the string
	 */
	stringToUTF8Array: function(string, array, offset) {
		if(!offset) offset = 0;
		var n = string.length;
		for(var i=0; i<n; i++) {
			var val = string.charCodeAt(i);
			if(val >= 128) {
				if(val >= 2048) {
					array[offset] = (val >>> 12) | 224;
					array[offset+1] = ((val >>> 6) & 63) | 128;
					array[offset+2] = (val & 63) | 128;
					offset += 3;
				} else {
					array[offset] = ((val >>> 6) | 192);
					array[offset+1] = (val & 63) | 128;
					offset += 2;
				}
			} else {
				array[offset++] = val;
			}
		}
	},

	/**
	 * Gets the byte length of the UTF-8 representation of a given string
	 * @param {String} string
	 * @return {Integer}
	 */
	getStringByteLength: function(string) {
		var length = 0, n = string.length;
		for(var i=0; i<n; i++) {
			var val = string.charCodeAt(i);
			if(val >= 128) {
				if(val >= 2048) {
					length += 3;
				} else {
					length += 2;
				}
			} else {
				length += 1;
			}
		}
		return length;
	},	

	semverCompare(a, b) {
		a = a.split('.').map(version => isNaN(parseInt(version)) ? version : parseInt(version));
		b = b.split('.').map(version => isNaN(parseInt(version)) ? version : parseInt(version));
		for (let i = 0; i < a.length && i < b.length; i++) {
			if (a[i] < b[i]) return -1;
			else if (a[i] > b[i]) return 1;
		}
		return a.length - b.length; // E.g. 5.0.0 < 5.0.0.1
	},
	
	allowedKeyChars: "23456789ABCDEFGHIJKLMNPQRSTUVWXYZ",

	/**
	 * Generates a valid object key for the server API
	 */
	generateObjectKey: function generateObjectKey() {
		return Zotero.Utilities.randomString(8, Zotero.Utilities.allowedKeyChars);
	},

	/**
	 * Check if an object key is in a valid format
	 */
	isValidObjectKey: function(key) {
		if (!Zotero.Utilities.objectKeyRegExp) {
			Zotero.Utilities.objectKeyRegExp = new RegExp('^[' + Zotero.Utilities.allowedKeyChars + ']{8}$');
		}
		return Zotero.Utilities.objectKeyRegExp.test(key);
	},

	itemTypeExists: function(type) {
		Zotero.debug(`Zotero.Utilities.itemTypeExists() is deprecated -- use Zotero.Utilities.Item.itemTypeExists() instead`);
		return Zotero.Utilities.Item.itemTypeExists(type);
	},
	
	itemToCSLJSON: function(zoteroItem) {
		Zotero.debug(`Zotero.Utilities.itemToCSLJSON() is deprecated -- use Zotero.Utilities.Item.itemToCSLJSON() instead`);
		return Zotero.Utilities.Item.itemToCSLJSON(zoteroItem);
	},
	itemFromCSLJSON: function(item, cslItem) {
		Zotero.debug(`Zotero.Utilities.itemFromCSLJSON() is deprecated -- use Zotero.Utilities.Item.itemFromCSLJSON() instead`);
		return Zotero.Utilities.Item.itemFromCSLJSON(item, cslItem);
	},

	assignProps: movedToUtilitiesInternal("assignProps"),
	parseURL: movedToUtilitiesInternal("parseURL"),
	resolveIntermediateURL: movedToUtilitiesInternal("resolveIntermediateURL"),
	determineAttachmentIcon: movedToUtilitiesInternal('determineAttachmentIcon'),

	/**
	 * Walk the DOM and the contents of JSON data attributes in the HTML representation
	 * of a note, calling visitor functions that might modify it and returning
	 * the resulting HTML.
	 *
	 * Elements are visited in depth-first order. First the element itself is visited,
	 * then its data attributes, then the URIs in its JSON attributes, then its subtree.
	 *
	 * @param {String} note Note HTML
	 * @param {Object} visitors
	 * @param {Function} [visitors.visitContainer]
	 * @param {Function} [visitors.visitAnnotation]
	 * @param {Function} [visitors.visitCitation]
	 * @param {Function} [visitors.visitOtherElement]
	 * @param {Function} [visitors.visitDataAttribute]
	 * @param {Function} [visitors.visitURI] Return a replacement for the passed URI
	 * @return {String} Potentially modified note HTML
	 */
	walkNoteDOM: function (note, visitors) {
		function visit(elem) {
			if (elem.hasAttribute('data-schema-version')) {
				visitors.visitContainer?.(elem);
			}
			else if (elem.hasAttribute('data-annotation')) {
				visitors.visitAnnotation?.(elem);
			}
			else if (elem.hasAttribute('data-citation')) {
				visitors.visitCitation?.(elem);
			}
			else {
				visitors.visitOtherElement?.(elem);
			}

			if (visitors.visitDataAttribute || visitors.visitURI) {
				for (let attr of ['data-citation', 'data-citation-items', 'data-annotation']) {
					if (elem.hasAttribute(attr)) {
						let json;
						try {
							json = JSON.parse(decodeURIComponent(elem.getAttribute(attr)));
						}
						catch (e) {
							continue;
						}

						visitors.visitDataAttribute?.(attr, json);
						if (visitors.visitURI) visitURIs(json);

						elem.setAttribute(attr, JSON.stringify(json));
					}
				}
			}

			for (let child of elem.children) {
				visit(child);
			}
		}

		function visitURIs(json) {
			if (Array.isArray(json)) {
				json.forEach(visitURIs);
			}
			else if (typeof json === 'object') {
				for (let [key, value] of Object.entries(json)) {
					if (key == 'id' || key == 'attachmentURI') {
						json[key] = visitors.visitURI(value);
					}
					else if (key == 'uris') {
						json[key] = value.map(visitors.visitURI)
					}
					else {
						visitURIs(value);
					}
				}
			}
		}

		let wrappedNote = '<div id="note-body">' + note + '</div>';
		let doc;
		if (Zotero.isNode) {
			let { JSDOM } = require('jsdom');
			doc = new JSDOM(wrappedNote).window.document;
		}
		else {
			let parser;
			try {
				parser = new DOMParser();
			}
			catch {
				// Fx60 defines DOMParser but throws an error when you construct
				// it from this context
				parser = Cc['@mozilla.org/xmlextras/domparser;1'].createInstance(Ci.nsIDOMParser);
			}
			doc = parser.parseFromString(wrappedNote, 'text/html');
		}

		let root = doc.getElementById('note-body');
		for (let child of root.children) {
			visit(child);
		}
		return root.innerHTML;
	},

	// /**
	//  * Provides unicode support and other additional features for regular expressions
	//  * See https://github.com/slevithan/xregexp for usage
	//  */
	XRegExp: typeof XRegExp !== "undefined" ? XRegExp : null
}

if (!Utilities.XRegExp) {
	if (typeof module != 'undefined') {
		Utilities.XRegExp = require('./xregexp-all');
	}
}

if (typeof module != 'undefined') {
	module.exports = Utilities;
} else if (typeof Zotero != 'undefined') {
	Zotero.Utilities = Utilities;
}

})();
