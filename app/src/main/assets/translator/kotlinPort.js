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

//
//	port.onmessage = function (f) {
//        var message = JSON.parse(f.data);
//        console.log('DES', message);
//        if (message.action === 'initSchemaAndDateFormats') {
//            console.log('initSchemaAndDateFormats call started', message);
//            var schemaData = message.schemaData;
//            var dateFormatData = message.dateFormatData;
//            console.log('dateFormatData = ', dateFormatData);
//            initSchemaAndDateFormats(schemaData, dateFormatData)
//            console.log('initSchemaAndDateFormats call finished', message);
//            port.postMessage("initSchemaAndDateFormats success");
//        }
//        if (message.action === 'initTranslators') {
//           console.log('initTranslators call started', message);
//            var encodedTranslators = message.encodedTranslators;
//            console.log('encodedTranslators = ', encodedTranslators);
//            initTranslators(encodedTranslators)
//            console.log('initTranslators call finished', message);
//            port.postMessage("initTranslators success");
//        }
//
//          if (message.action === 'translate') {
//               console.log('translate call started', message);
//               var url = message.url
//               var encodedHtml = message.encodedHtml
//               var encodedFrames = message.encodedFrames
//                console.log('encodedFrames = ', encodedFrames);
//                translate(url,encodedHtml, encodedFrames)
//                console.log('translate call finished', message);
//                port.postMessage("translate success");
//            }
//
//              if (message.action === 'receiveResponse') {
//                   console.log('receiveResponse call started', message);
//                   var messageId = message.messageId
//                   var encodedPayload = message.encodedPayload
//                    console.log('messageId = ', messageId);
//                    Zotero.Messaging.receiveResponse(messageId, encodedPayload)
//                    console.log('translate call finished');
//                    port.postMessage("receiveResponse success");
//               }
//
//	}
};