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

function fetchLocal(url) {
    return new Promise(function(resolve, reject) {
        var xhr = new XMLHttpRequest
        xhr.responseType = 'arraybuffer';
        xhr.onload = function() {
        resolve(this.response);
        }
        xhr.onerror = function() {
          reject(new TypeError('Local request failed:' + url));
        }
        xhr.open('GET', url)
        xhr.send(null)
  })
}

const pick = (object, pickKeys) => {
	if(typeof(pickKeys) === 'function') {
		return Object.entries(object)
			.reduce((aggr, [key, value]) => {
				if(pickKeys(key)) { aggr[key] = value; }
				return aggr;
		}, {});
	}
	if(!Array.isArray(pickKeys)) {
		pickKeys = [pickKeys];
	}

	return Object.entries(object)
		.reduce((aggr, [key, value]) => {
			if(pickKeys.includes(key)) { aggr[key] = value; }
			return aggr;
	}, {});
}