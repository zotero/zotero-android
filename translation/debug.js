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

Zotero.Debug = new function () {
	var _console, _store, _level, _lastTime, _output = [];
	var _slowTime = false;
	var _consoleViewer = false;
	
	/**
	 * Initialize debug logging
	 */
	this.init = function (enabled=true) {
		this.enabled = enabled;
	}
	
	this.log = function (message, level, maxDepth, stack) {
		if (!this.enabled) {
			return;
		}
		
		if (typeof message != 'string') {
			message = Zotero.Utilities.varDump(message, 0, maxDepth);
		}
		
		if (!level) {
			level = 3;
		}
		
		// If level above debug.level value, don't display
		if (level > _level) {
			return;
		}
		
		var deltaStr = '';
		var deltaStrStore = '';
		var delta = 0;
		var d = new Date();
		if (_lastTime) {
			delta = d - _lastTime;
		}
		_lastTime = d;
		var slowPrefix = "";
		var slowSuffix = "";
		if (_slowTime && delta > _slowTime) {
			slowPrefix = "\x1b[31;40m";
			slowSuffix = "\x1b[0m";
		}
		
		delta = ("" + delta).padStart(7, "0");
		
		deltaStr = "(" + slowPrefix + "+" + delta + slowSuffix + ")";
		if (_store) {
			deltaStrStore = "(+" + delta + ")";
		}
		
		if (stack === true) {
			stack = new Error().stack.substr('Error'.length);
		}
		
		if (stack) {
			message += '\n' + this.stackToString(stack);
		}
		
		var output = '(' + level + ')' + deltaStr + ': ' + message;
        sendToPort("logHandler", output);

		if (_store) {
			if (Math.random() < 1/1000) {
				// Remove initial lines if over limit
				var overage = this.count() - Zotero.Prefs.get('debug.store.limit');
				if (overage > 0) {
					_output.splice(0, Math.abs(overage));
				}
			}
			_output.push('(' + level + ')' + deltaStrStore + ': ' + message);
		}
	}
	
	
	this.get = Zotero.Promise.method(function(maxChars, maxLineLength) {
		var output = _output;
		var total = output.length;
		
		if (total == 0) {
			return "";
		}
		
		if (maxLineLength) {
			for (var i=0, len=output.length; i<len; i++) {
				if (output[i].length > maxLineLength) {
					output[i] = Zotero.Utilities.ellipsize(output[i], maxLineLength, false, true);
				}
			}
		}
		
		output = output.join('\n\n');
		
		if (maxChars) {
			output = output.substr(maxChars * -1);
			// Cut at two newlines
			let matches = output.match(/^[\n]*\n\n/);
			if (matches) {
				output = output.substr(matches[0].length);
			}
		}

		return output;
	});
	
	
	this.setStore = function (enable) {
		if (enable) {
			this.clear();
		}
		_store = enable;
		this.updateEnabled();
		this.storing = _store;
	}
	
	
	this.updateEnabled = function () {
		this.enabled = _console || _consoleViewer || _store;
	};
	
	
	this.count = function () {
		return _output.length;
	}
	
	
	this.clear = function () {
		_output = [];
	}
	
	/**
	 * Format a stack trace for output in the same way that Error.stack does
	 * @param {Components.stack} stack
	 * @param {Integer} [lines=5] Number of lines to format
	 */
	this.stackToString = function (stack, lines) {
		if (!lines) lines = 5;
		var str = '';
		while(stack && lines--) {
			str += '\n  ' + (stack.name || '') + '@' + stack.filename
				+ ':' + stack.lineNumber;
			stack = stack.caller;
		}
		return this.filterStack(str).substr(1);
	};
	
	
	/**
	 * Strip Bluebird lines from a stack trace
	 *
	 * @param {String} stack
	 */
	this.filterStack = function (stack) {
		return stack.split(/\n/).filter(line => line.indexOf('zotero/bluebird') == -1).join('\n');
	}
}

if (typeof process === 'object' && process + '' === '[object process]'){
	module.exports = Zotero.Debug;
}
