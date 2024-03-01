/*
    ***** BEGIN LICENSE BLOCK *****

    Copyright © 2019 Center for History and New Media
                     George Mason University, Fairfax, Virginia, USA
                     http://zotero.org

    This file is part of Zotero.

    Zotero is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Zotero is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Zotero.  If not, see <http://www.gnu.org/licenses/>.

    ***** END LICENSE BLOCK *****
*/

const MESSAGE_TIMEOUT = 5 * 60 * 1000;

Zotero.Messaging = new function() {
	var _responseListeners = {};

    this.receiveResponse = async function(id, encodedPayload) {
    	var payload = {};
    	if (encodedPayload) {
    		payload = JSON.parse(decodeURIComponent(escape(window.atob(encodedPayload))));
    	}

        let callback = _responseListeners[id];

        if (!callback) return;

        let response = callback(payload);
        // await for the response for error handling
        if (response && response.then) {
            await response;
        }
    };

    this.sendMessage = async function(handler, payload) {
        try {
            var deferred = Zotero.Promise.defer();
            var messageId = Math.floor(Math.random()*1e12);
            var resolved = false;

            function responseCallback(payload) {
                resolved = true;

                if (payload && payload["error"]) {
                    var errJSON = payload["error"];
                    let e = new Error(errJSON.message);
                    for (let key in errJSON) e[key] = errJSON[key];
                    Zotero.debug("Callback error: " + JSON.stringify(errJSON));
                    deferred.reject(e);
                }

                deferred.resolve(payload);
            }

            _responseListeners[messageId] = responseCallback;

            const fullPayload = {
                messageId: messageId,
                payload: payload
            }

           sendToPort(handler, fullPayload);
//         Make sure we don't slowly gobble up memory with callbacks
//         The drawback is that Google Docs users will timeout in MESSAGE_TIMEOUT
//         (at the time of writing this is 5min)
            var timeout = setTimeout(function() {
                if (!resolved) {
                    deferred.reject(new Error(`Message ${messageName} response timed out`));
                }
                delete _responseListeners[messageId];
            }, MESSAGE_TIMEOUT);
            var response = await deferred.promise;
        }

        finally {
            clearTimeout(timeout);
        }

        return response;
    };
}
