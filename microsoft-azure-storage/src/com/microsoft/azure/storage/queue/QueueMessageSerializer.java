/**
 * Copyright Microsoft Corporation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.microsoft.azure.storage.queue;

import java.io.IOException;
import java.io.StringWriter;

import org.xmlpull.v1.XmlSerializer;

import com.microsoft.azure.storage.Constants;
import com.microsoft.azure.storage.core.Utility;

/**
 * RESERVED FOR INTERNAL USE. A class used to serialize message requests to a byte array.
 */
final class QueueMessageSerializer {

    /**
     * Generates the message request body from a string containing the message.
     * The message must be encodable as UTF-8. To be included in a web request,
     * this message request body must be written to the output stream of the web
     * request.
     * 
     * @param message
     *            A <code>String<code> containing the message to wrap in a message request body.
     * 
     * @return An array of <code>byte</code> containing the message request body
     *         encoded as UTF-8.
     * @throws IOException
     *             if there is an error writing the queue message.
     * @throws IllegalStateException
     *             if there is an error writing the queue message.
     * @throws IllegalArgumentException
     *             if there is an error writing the queue message.
     */
    public static byte[] generateMessageRequestBody(final String message) throws IllegalArgumentException,
            IllegalStateException, IOException {
        final StringWriter outWriter = new StringWriter();
        final XmlSerializer xmlw = Utility.getXmlSerializer(outWriter);

        // default is UTF8
        xmlw.startDocument(Constants.UTF8_CHARSET, true);
        xmlw.startTag(Constants.EMPTY_STRING, QueueConstants.QUEUE_MESSAGE_ELEMENT);

        Utility.serializeElement(xmlw, QueueConstants.MESSAGE_TEXT_ELEMENT, message);

        // end QueueMessage_ELEMENT
        xmlw.endTag(Constants.EMPTY_STRING, QueueConstants.QUEUE_MESSAGE_ELEMENT);

        // end doc
        xmlw.endDocument();

        return outWriter.toString().getBytes(Constants.UTF8_CHARSET);
    }
}
