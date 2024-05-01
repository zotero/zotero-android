var port;

function sendToPort(handlerName, message) {
    const obj = {
      handlerName: handlerName,
      message: message,
    };
    port.postMessage(JSON.stringify(obj));
}

onmessage = function(e) {

     if (e.data == 'initPort') {
	     port = e.ports[0];
     }
};