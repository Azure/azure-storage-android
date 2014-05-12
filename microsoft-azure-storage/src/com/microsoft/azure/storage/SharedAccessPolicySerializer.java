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
package com.microsoft.azure.storage;

import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlSerializer;

import com.microsoft.azure.storage.core.SR;
import com.microsoft.azure.storage.core.Utility;

/**
 * RESERVED FOR INTERNAL USE. A class used to serialize SharedAccessPolicies to a byte array.
 */
public final class SharedAccessPolicySerializer {
    /**
     * RESERVED FOR INTERNAL USE. Writes a collection of shared access policies to the specified stream in XML format.
     * 
     * @param <T>
     * 
     * @param sharedAccessPolicies
     *            A collection of shared access policies
     * @param outWriter
     *            a sink to write the output to.
     * @throws IOException
     *             if there is an error writing the shared access identifiers.
     * @throws IllegalStateException
     *             if there is an error writing the shared access identifiers.
     * @throws IllegalArgumentException
     *             if there is an error writing the shared access identifiers.
     */
    public static <T extends SharedAccessPolicy> void writeSharedAccessIdentifiersToStream(
            final HashMap<String, T> sharedAccessPolicies, final StringWriter outWriter)
            throws IllegalArgumentException, IllegalStateException, IOException {
        Utility.assertNotNull("sharedAccessPolicies", sharedAccessPolicies);
        Utility.assertNotNull("outWriter", outWriter);

        final XmlSerializer xmlw = Utility.getXmlSerializer(outWriter);

        if (sharedAccessPolicies.keySet().size() > Constants.MAX_SHARED_ACCESS_POLICY_IDENTIFIERS) {
            final String errorMessage = String.format(SR.TOO_MANY_SHARED_ACCESS_POLICY_IDENTIFIERS,
                    sharedAccessPolicies.keySet().size(), Constants.MAX_SHARED_ACCESS_POLICY_IDENTIFIERS);

            throw new IllegalArgumentException(errorMessage);
        }

        // default is UTF8
        xmlw.startDocument(Constants.UTF8_CHARSET, true);
        xmlw.startTag(Constants.EMPTY_STRING, Constants.SIGNED_IDENTIFIERS_ELEMENT);

        for (final Entry<String, T> entry : sharedAccessPolicies.entrySet()) {
            final SharedAccessPolicy policy = entry.getValue();
            xmlw.startTag(Constants.EMPTY_STRING, Constants.SIGNED_IDENTIFIER_ELEMENT);

            // Set the identifier
            Utility.serializeElement(xmlw, Constants.ID, entry.getKey());

            xmlw.startTag(Constants.EMPTY_STRING, Constants.ACCESS_POLICY);

            // Set the Start Time
            Utility.serializeElement(xmlw, Constants.START, Utility
                    .getUTCTimeOrEmpty(policy.getSharedAccessStartTime()).toString());

            // Set the Expiry Time
            Utility.serializeElement(xmlw, Constants.EXPIRY,
                    Utility.getUTCTimeOrEmpty(policy.getSharedAccessExpiryTime()).toString());

            // Set the Permissions
            Utility.serializeElement(xmlw, Constants.PERMISSION, policy.permissionsToString());

            // end AccessPolicy
            xmlw.endTag(Constants.EMPTY_STRING, Constants.ACCESS_POLICY);
            // end SignedIdentifier
            xmlw.endTag(Constants.EMPTY_STRING, Constants.SIGNED_IDENTIFIER_ELEMENT);
        }

        // end SignedIdentifiers
        xmlw.endTag(Constants.EMPTY_STRING, Constants.SIGNED_IDENTIFIERS_ELEMENT);
        // end doc
        xmlw.endDocument();
    }
}
