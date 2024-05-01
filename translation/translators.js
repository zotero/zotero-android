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

// Enumeration of types of translators
window.TRANSLATOR_TYPES = {"import":1, "export":2, "web":4, "search":8};

window.TRANSLATOR_CACHING_PROPERTIES = TRANSLATOR_REQUIRED_PROPERTIES.concat(["browserSupport", "targetAll"]);

/**
 * Singleton to handle loading and caching of translators
 * @namespace
 */
Zotero.Translators = Object.assign(Zotero.Translators, new function() {
	this._cache = {"import":[], "export":[], "web":[], "search":[]};
	this._initialized = false;
	
	/**
	 * Initializes translator cache, loading all relevant translators into memory
	 * @param {Zotero.Translate[]} [translators] List of translators. If not specified, it will be
	 *                                           retrieved from storage.
	 */
	this.init = async function(translators) {
		this._cache = {"import":[], "export":[], "web":[], "search":[]};
		_translators = {};
		
		// Build caches
		for(var i=0; i<translators.length; i++) {
			try {
				var translator = new Zotero.Translator(translators[i]);
				_translators[translator.translatorID] = translator;
				
				for(var type in TRANSLATOR_TYPES) {
					if(translator.translatorType & TRANSLATOR_TYPES[type]) {
						this._cache[type].push(translator);
					}
				}
			} catch(e) {
				Zotero.logError(e);
				try {
					Zotero.logError("Could not load translator "+JSON.stringify(translators[i]));
				} catch(e) {}
			}
		}
		
		// Sort by priority
		var cmp = function (a, b) {
			if (a.priority > b.priority) {
				return 1;
			}
			else if (a.priority < b.priority) {
				return -1;
			}
		}
		for(var type in this._cache) {
			this._cache[type].sort(cmp);
		}
		this._initialized = true;
	}

	/**
	 * Gets the translator code that corresponds to a given ID
	 * @param {Zotero.Translator} translator
	 * @return {String} translator code
	 */
	this.getCodeForTranslator = Zotero.Promise.method(async function (translator) {
		if (translator.code) return translator.code;
		let code = await Zotero.Repo.getTranslatorCode(translator.translatorID);
		translator.code = code;
		return code;
	});
	
	/**
	 * Gets the translator that corresponds to a given ID
	 *
	 * @param {String} id The ID of the translator
	 */
	this.get = async function (id) {
		if (!this._initialized) await Zotero.Translators.init();
		var translator = _translators[id];
		if (!translator) {
			return false;
		}
		
		// only need to get code if it is of some use
		if (translator.runMode === Zotero.Translator.RUN_MODE_IN_BROWSER
				&& !translator.hasOwnProperty("code")) {
				
			translator.code = await Zotero.Translators.getCodeForTranslator(translator);
			return translator;
		} else {
			return translator;
		}
	};
	
	/**
	 * Gets all translators for a specific type of translation
	 * @param {String} type The type of translators to get (import, export, web, or search)
	 */
	this.getAllForType = async function (type) {
		if(!this._initialized) await Zotero.Translators.init();
		var translators = this._cache[type].slice(0);
		var codeGetter = new Zotero.Translators.CodeGetter(translators);
		await codeGetter.getAll();
		return translators;
	};
});
