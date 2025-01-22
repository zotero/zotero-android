

class PDFWorker {
	constructor(config) {
		this.config = config;
		this._worker = null;
		this._lastPromiseID = 0;
		this._waitingPromises = {};
		this._queue = [];
		this._processingQueue = false;
	}

	async _processQueue() {
		this._init();
		if (this._processingQueue) {
			return;
		}
		this._processingQueue = true;
		let item;
		while ((item = this._queue.shift())) {
			if (item) {
				let [fn, resolve, reject] = item;
				try {
					resolve(await fn());
				}
				catch (e) {
					reject(e);
				}
			}
		}
		this._processingQueue = false;
	}

	async _enqueue(fn, isPriority) {
		return new Promise((resolve, reject) => {
			if (isPriority) {
				this._queue.unshift([fn, resolve, reject]);
			}
			else {
				this._queue.push([fn, resolve, reject]);
			}
			this._processQueue();
		});
	}

	async _query(action, data, transfer) {
		return new Promise((resolve, reject) => {
			this._lastPromiseID++;
			this._waitingPromises[this._lastPromiseID] = { resolve, reject };
			this._worker.postMessage({ id: this._lastPromiseID, action, data }, transfer);
		});
	}

	_init() {
		if (this._worker) return;
		this._worker = new Worker(this.config.pdfWorkerURL);
		this._worker.addEventListener('message', async (event) => {
			let message = event.data;
			if (message.responseID) {
				let { resolve, reject } = this._waitingPromises[message.responseID];
				delete this._waitingPromises[message.responseID];
				if (message.data) {
					resolve(message.data);
				}
				else {
					reject(new Error(JSON.stringify(message.error)));
				}
				return;
			}
			if (message.id) {
				let respData = null;
				try {
					if (message.action === 'FetchBuiltInCMap') {
					    Zotero.debug("FetchBuiltInCMap");
						const response = await fetch(this.config.pdfReaderCMapsURL + message.data + '.bcmap');
						const arrayBuffer = await response.arrayBuffer();
						respData = {
							compressionType: 1,
							cMapData: new Uint8Array(arrayBuffer)
						};
					}
				}
				catch (e) {
					console.log('Failed to fetch CMap data:');
					console.log(e);
				}

				try {
					if (message.action === 'FetchStandardFontData') {
					    Zotero.debug("FetchStandardFontData");
						const response = await fetch(this.config.pdfReaderStandardFontsURL + message.data);
						const arrayBuffer = await response.arrayBuffer();
						respData = new Uint8Array(arrayBuffer);
					}
				}
				catch (e) {
					console.log('Failed to fetch standard font data:');
					console.log(e);
				}

				this._worker.postMessage({ responseID: event.data.id, data: respData });
			}
		});
		this._worker.addEventListener('error', (event) => {
			console.log(`PDF Web Worker error (${event.filename}:${event.lineno}): ${event.message}`);
		});
	}

	/**
	 * Get data for recognizer-server
	 *
	 * @param {ArrayBuffer} buf PDF file
	 * @param {Boolean} [isPriority]
	 * @param {String} [password]
	 * @returns {Promise}
	 */
	async getRecognizerData(buf, isPriority, password) {
		return this._enqueue(async () => {
			try {
				var result = await this._query('getRecognizerData', { buf, password }, [buf]);
			}
			catch (e) {
				let error = new Error(`Worker 'getRecognizerData' failed: ${JSON.stringify({ error: e.message })}`);
				try {
					error.name = JSON.parse(e.message).name;
				}
				catch (e) {
					console.log(e);
				}
				console.log(error);
				throw error;
			}
			return result;
		}, isPriority);
	}
}

window.pdfWorker = function(pdfWorkerURL, pdfReaderCMapsURL, pdfReaderStandardFontsURL) {
    const pdfWorker = new PDFWorker({ pdfWorkerURL, pdfReaderCMapsURL, pdfReaderStandardFontsURL });
    return pdfWorker;
 }