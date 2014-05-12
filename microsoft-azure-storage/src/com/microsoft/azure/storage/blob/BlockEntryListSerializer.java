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
package com.microsoft.azure.storage.blob;

import java.io.IOException;
import java.io.StringWriter;

import org.xmlpull.v1.XmlSerializer;

import com.microsoft.azure.storage.Constants;
import com.microsoft.azure.storage.OperationContext;
import com.microsoft.azure.storage.core.Utility;

/**
 * RESERVED FOR INTERNAL USE. A class used to serialize a block list to a byte array.
 */
final class BlockEntryListSerializer {

    /**
     * Writes a Block List and returns the corresponding UTF8 bytes.
     * 
     * @param blockList
     *            the Iterable of BlockEntry to write
     * @param opContext
     *            a tracking object for the request
     * @return a byte array of the UTF8 bytes representing the serialized block list.
     * @throws IOException
     *             if there is an error writing the block list.
     * @throws IllegalStateException
     *             if there is an error writing the block list.
     * @throws IllegalArgumentException
     *             if there is an error writing the block list.
     */
    public static byte[] writeBlockListToStream(final Iterable<BlockEntry> blockList, final OperationContext opContext)
            throws IllegalArgumentException, IllegalStateException, IOException {

        final StringWriter outWriter = new StringWriter();
        final XmlSerializer xmlw = Utility.getXmlSerializer(outWriter);

        // default is UTF8
        xmlw.startDocument(Constants.UTF8_CHARSET, true);
        xmlw.startTag(Constants.EMPTY_STRING, BlobConstants.BLOCK_LIST_ELEMENT);

        for (final BlockEntry block : blockList) {

            if (block.getSearchMode() == BlockSearchMode.COMMITTED) {
                Utility.serializeElement(xmlw, BlobConstants.COMMITTED_ELEMENT, block.getId());
            }
            else if (block.getSearchMode() == BlockSearchMode.UNCOMMITTED) {
                Utility.serializeElement(xmlw, BlobConstants.UNCOMMITTED_ELEMENT, block.getId());
            }
            else if (block.getSearchMode() == BlockSearchMode.LATEST) {
                Utility.serializeElement(xmlw, BlobConstants.LATEST_ELEMENT, block.getId());
            }
        }

        // end BlockListElement
        xmlw.endTag(Constants.EMPTY_STRING, BlobConstants.BLOCK_LIST_ELEMENT);

        // end doc
        xmlw.endDocument();

        return outWriter.toString().getBytes(Constants.UTF8_CHARSET);
    }
}
