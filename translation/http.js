/*
	***** BEGIN LICENSE BLOCK *****
	
	Copyright © 2021 Corporation for Digital Scholarship
                     Vienna, Virginia, USA
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
	
	***** END LICENSE BLOCK *****
*/

/**
 * Functions for performing HTTP requests, both via XMLHTTPRequest and using a hidden browser
 * @namespace
 */
Zotero.HTTP = new function() {
	this.StatusError = function(xmlhttp, url) {
		this.message = `HTTP request to ${url} rejected with status ${xmlhttp.status}`;
		this.status = xmlhttp.status;
		try {
			this.responseText = typeof xmlhttp.responseText == 'string' ? xmlhttp.responseText : undefined;
		} catch (e) {}
	};
	this.StatusError.prototype = Object.create(Error.prototype);

	this.TimeoutError = function(ms) {
		this.message = `HTTP request has timed out after ${ms}ms`;
	};
	this.TimeoutError.prototype = Object.create(Error.prototype);
	
	/**
	 * Get a promise for a HTTP request
	 *
	 * @param {String} method The method of the request ("GET", "POST", "HEAD", or "OPTIONS")
	 * @param {String}	url				URL to request
	 * @param {Object} [options] Options for HTTP request:<ul>
	 *         <li>body - The body of a POST request</li>
	 *         <li>headers - Object of HTTP headers to send with the request</li>
	 *         <li>debug - Log response text and status code</li>
	 *         <li>logBodyLength - Length of request body to log</li>
	 *         <li>timeout - Request timeout specified in milliseconds [default 15000]</li>
	 *         <li>responseType - The response type of the request from the XHR spec</li>
	 *         <li>responseCharset - The charset the response should be interpreted as</li>
	 *         <li>successCodes - HTTP status codes that are considered successful, or FALSE to allow all</li>
	 *     </ul>
	 * @return {Promise<XMLHttpRequest>} A promise resolved with the XMLHttpRequest object if the
	 *     request succeeds, or rejected if the browser is offline or a non-2XX status response
	 *     code is received (or a code not in options.successCodes if provided).
	 */
	this.request = async function(method, url, options = {}) {
		// Default options
		options = Object.assign({
			body: null,
			headers: {},
			debug: false,
			logBodyLength: 1024,
			timeout: 15000,
			responseType: '',
			responseCharset: null,
			successCodes: null,
            url: url,
            method: method
		}, options);

        try {
            var response = await Zotero.Messaging.sendMessage("requestHandler", options);
            var status = response.status;
            var responseText = response.responseText;
            var headers = response.headers;
            var responseURL = response.url;
        } catch (err) {
            status = err["status"];
            headers = {};
            responseText = err["responseText"];
            responseURL = "";
        }

        let invalidDefaultStatus = options.successCodes === null && (status < 200 || status >= 300);
        let invalidStatus = Array.isArray(options.successCodes) && !options.successCodes.includes(status);
        if (invalidDefaultStatus || invalidStatus) {
            throw new Zotero.HTTP.StatusError({status, responseText}, url);
        }

        let headerString = Object.keys(headers).map(key => `${key}: ${headers[key]}`).join("\n");
        Object.keys(headers).forEach(key => headers[key.toLowerCase()] = headers[key]);

        var response = responseText;

        switch (options.responseType) {
        case "document":
            let parser = new DOMParser();
            var contentType = headers["content-type"];
            if (contentType != 'application/xml' && contentType != 'text/xml') {
                contentType = 'text/html';
            }
            let doc = parser.parseFromString(responseText, contentType);
            response = doc;
            break;

        case "json":
            response = JSON.parse(responseText);
            break;

        default: break;
        }

        return {
            status: status,
            responseText: responseText,
            response: response,
            responseURL: responseURL,
            responseHeaders: headerString,
            getAllResponseHeaders: () => headerString,
            getResponseHeader: name => headers[name.toLowerCase()]
        };
	};

    /**
     * Load one or more documents
     *
     * This should stay in sync with the equivalent function in the client
     *
     * @param {String|String[]} urls - URL(s) of documents to load
     * @param {Function} processor - Callback to be executed for each document loaded
     * @param {CookieJar} [cookieSandbox] Cookie sandbox object
     * @return {Promise<Array>} - A promise for an array of results from the processor runs
     */
    this.processDocuments = async function (urls, processor, options = {}) {
        // Handle old signature: urls, processor, onDone, onError
        if (arguments.length > 3) {
            Zotero.debug("Zotero.HTTP.processDocuments() now takes only 2 arguments -- update your code");
            var onDone = arguments[3];
            var onError = arguments[4];
        }

        var cookieSandbox = options.cookieSandbox;
        var headers = options.headers;

        if (typeof urls == "string") urls = [urls];
        var funcs = urls.map(url => () => {
            return Zotero.HTTP.request(
                "GET",
                url,
                {
                    responseType: 'document',
                    cookieSandbox,
                    headers
                }
            )
            .then((req) => {
                return processor(Zotero.HTTP.wrapDocument(req.response, url), req.responseURL);
            });
        });

        // Run processes serially
        // TODO: Add some concurrency?
        var f;
        var results = [];
        while (f = funcs.shift()) {
            try {
                results.push(await f());
            }
            catch (e) {
                if (onError) {
                    onError(e);
                }
                throw e;
            }
        }

        // Deprecated
        if (onDone) {
            onDone();
        }

        return results;
    }

	/**
	* Send an HTTP GET request via XMLHTTPRequest
	*
	* @deprecated Use {@link Zotero.HTTP.request}
	* @param {String}			url				URL to request
	* @param {Function} 		onDone			Callback to be executed upon request completion
	* @param {String}			responseCharset	
	* @param {N/A}				cookieSandbox	Not used in Connector
	* @param {Object}			headers			HTTP headers to include with the request
	* @return {Boolean} True if the request was sent, or false if the browser is offline
	*/
	this.doGet = function(url, onDone, responseCharset, cookieSandbox, headers) {
		Zotero.debug('Zotero.HTTP.doGet is deprecated. Use Zotero.HTTP.request');
		this.request('GET', url, {responseCharset, headers})
		.then(onDone, function(e) {
			onDone({status: e.status, responseText: e.responseText});
			throw (e);
		});
		return true;
	};
	
	/**
	* Send an HTTP POST request via XMLHTTPRequest
	*
	* @deprecated Use {@link Zotero.HTTP.request}
	* @param {String}			url URL to request
	* @param {String|Object[]}	body Request body
	* @param {Function}			onDone Callback to be executed upon request completion
	* @param {String}			headers Request HTTP headers
	* @param {String}			responseCharset	
	* @return {Boolean} True if the request was sent, or false if the browser is offline
	*/
	this.doPost = function(url, body, onDone, headers, responseCharset) {
		Zotero.debug('Zotero.HTTP.doPost is deprecated. Use Zotero.HTTP.request');
		this.request('POST', url, {body, responseCharset, headers})
		.then(onDone, function(e) {
			onDone({status: e.status, responseText: e.responseText});
			throw (e);
		});
		return true;	
	};
	
	
	/**
	 * Adds a ES6 Proxied location attribute
	 * @param doc
	 * @param docUrl
	 */
	this.wrapDocument = function(doc, docURL) {
        // Add <base> if it doesn't exist, so relative URLs resolve
        if (!doc.getElementsByTagName('base').length) {
            let head = doc.head;
            let base = doc.createElement('base');
            base.href = docURL;
            head.appendChild(base);
        }

        // Add 'location' and 'evaluate'
        var location = new URL(docURL);
        location.toString = () => docURL;

        var wrappedDoc = new Proxy(doc, {
            get: function (t, prop) {
                if (prop === 'location') {
                    return location;
                }
                else if (prop == 'evaluate') {
                    // If you pass the document itself into doc.evaluate as the second argument
                    // it fails, because it receives a proxy, which isn't of type `Node` for some reason.
                    // Native code magic.
                    return function() {
                        if (arguments[1] == wrappedDoc) {
                            arguments[1] = t;
                        }
                        return t.evaluate.apply(t, arguments)
                    }
                }
                else {
                    if (typeof t[prop] == 'function') {
                        return t[prop].bind(t);
                    }
                    return t[prop];
                }
            }
        });
        return wrappedDoc;
	};
	
	
	/**
	 * Adds request handlers to the XMLHttpRequest and returns a promise that resolves when
	 * the request is complete. xmlhttp.send() still needs to be called, this just attaches the
	 * handler
	 *
	 * See {@link Zotero.HTTP.request} for parameters
	 * @private
	 */
	this._attachHandlers = function(url, xmlhttp, options) {
		var deferred = Zotero.Promise.defer();
		xmlhttp.onload = () => deferred.resolve(xmlhttp);
		xmlhttp.onerror = xmlhttp.onabort = function() {
			var e = new Zotero.HTTP.StatusError(xmlhttp, url);
			if (options.successCodes === false) {
				deferred.resolve(xmlhttp);
			} else {
				deferred.reject(e);
			}
		};
		xmlhttp.ontimeout = function() {
			var e = new Zotero.HTTP.TimeoutError(xmlhttp.timeout);
			Zotero.logError(e);
			deferred.reject(e);
		};
		return deferred.promise;
	};
}
