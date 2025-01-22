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
    
    ***** END LICENSE BLOCK *****
*/

// NB: IDK IF THIS CONFIG DOES ANYTHING. Taken from TS
const config = new Map(Object.entries({
	"deproxifyURLs": false, // Automatically try deproxified versions of URLs
	"translators": {
		"CrossrefREST.email": "" // Pass an email to Crossref REST API to utilize the faster servers pool
	}
}));

// For fetching translators
const ZOTERO_CONFIG = {
	REPOSITORY_URL: 'https://www.zotero.org/repo/',
	REPOSITORY_CHECK_INTERVAL: 86400, // 24 hours
	REPOSITORY_RETRY_INTERVAL: 3600, // 1 hour
	REPOSITORY_CHANNEL: 'trunk',
};

var Zotero = new function() {
	// Do not download attachments via JS
	this.isConnector = true;
	this.locale = 'en-US';
	
	this.version = "5.0.78";
	
	/**
	 * Debug logging function
	 *
	 * Uses prefs e.z.debug.log and e.z.debug.level (restart required)
	 *
	 * Defaults to log level 3 if level not provided
	 */
	this.debug = function(message, level) {
		Zotero.Debug.log(message, level);
	}
	
	/**
	 * Log a JS error to the Mozilla JS error console.
	 * @param {Exception} err
	 */
	this.logError = function(err) {
		// Firefox uses this
		Zotero.debug(err);
	}
	
	this.setTimeout = setTimeout;
}

Zotero.Prefs = new function(){
	var tempStore = {};
	
	this.get = function(pref) {
		if (tempStore.hasOwnProperty(pref)) return tempStore[pref];
		if (config.has(pref)) return config.get(pref);
	};

	/**
	 * @param pref
	 * @param value
	 */
	this.set = function(pref, value) {
		tempStore[pref] = value;
	};

	/**
	 * @param pref
	 */
	this.clear = function(pref) {
		delete tempStore[pref];
	}
}
