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

package com.microsoft.azure.storage.table;

import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.microsoft.azure.storage.Constants;
import com.microsoft.azure.storage.StorageExtendedErrorInformation;

/***
 * RESERVED FOR INTERNAL USE. A class to help parse the error details from an input stream, specific to tables
 */
public final class TableStorageErrorDeserializer {

    /**
     * Gets the Extended Error information.
     * 
     * @return the Extended Error information.
     * 
     * @param reader
     *            the input stream to read error details from.
     * @param format
     *            The {@link TablePayloadFormat} to use for parsing
     * @throws IOException
     *             if an error occurs while accessing the stream with Json.
     */
    public static StorageExtendedErrorInformation getExtendedErrorInformation(final Reader reader,
            final TablePayloadFormat format) throws JsonParseException, IOException {

        JsonFactory jsonFactory = new JsonFactory();
        JsonParser parser = jsonFactory.createParser(reader);
        try {
            return parseJsonResponse(parser);
        }
        finally {
            parser.close();
        }
    }

    /**
     * Parses the error exception details from the Json-formatted response.
     * 
     * @param parser
     *            the {@link JsonParser} to use for parsing
     * @throws IOException
     *             if an error occurs while accessing the stream with Json.
     * @throws JsonParseException
     *             if an error occurs while parsing the stream.
     */
    private static HashMap<String, String[]> parseJsonErrorException(JsonParser parser) throws JsonParseException,
            IOException {
        HashMap<String, String[]> additionalDetails = new HashMap<String, String[]>();

        parser.nextToken();
        ODataUtilities.assertIsStartObjectJsonToken(parser);

        parser.nextToken();
        ODataUtilities.assertIsFieldNameJsonToken(parser);

        while (parser.getCurrentToken() != JsonToken.END_OBJECT) {
            if (parser.getCurrentName().equals(TableConstants.ErrorConstants.ERROR_MESSAGE)) {
                parser.nextToken();
                additionalDetails.put(TableConstants.ErrorConstants.ERROR_MESSAGE,
                        new String[] { parser.getValueAsString() });
            }
            else if (parser.getCurrentName().equals(TableConstants.ErrorConstants.ERROR_EXCEPTION_TYPE)) {
                parser.nextToken();
                additionalDetails.put(TableConstants.ErrorConstants.ERROR_EXCEPTION_TYPE,
                        new String[] { parser.getValueAsString() });
            }
            else if (parser.getCurrentName().equals(TableConstants.ErrorConstants.ERROR_EXCEPTION_STACK_TRACE)) {
                parser.nextToken();
                additionalDetails
                        .put(Constants.ERROR_EXCEPTION_STACK_TRACE, new String[] { parser.getValueAsString() });
            }
            parser.nextToken();
        }

        return additionalDetails;
    }

    /**
     * Parses the extended error information from the Json-formatted response.
     * 
     * @throws IOException
     *             if an error occurs while accessing the stream with Json.
     * @throws JsonParseException
     *             if an error occurs while parsing the stream.
     */
    private static StorageExtendedErrorInformation parseJsonResponse(JsonParser parser) throws JsonParseException,
            IOException {
        final StorageExtendedErrorInformation errorInfo = new StorageExtendedErrorInformation();

        if (!parser.hasCurrentToken()) {
            parser.nextToken();
        }

        ODataUtilities.assertIsStartObjectJsonToken(parser);

        parser.nextToken();
        ODataUtilities.assertIsFieldNameJsonToken(parser);
        ODataUtilities.assertIsExpectedFieldName(parser, "odata.error");

        // start getting extended error information
        parser.nextToken();
        ODataUtilities.assertIsStartObjectJsonToken(parser);

        // get code
        parser.nextValue();
        ODataUtilities.assertIsExpectedFieldName(parser, TableConstants.ErrorConstants.ERROR_CODE);
        errorInfo.setErrorCode(parser.getValueAsString());

        // get message
        parser.nextToken();
        ODataUtilities.assertIsFieldNameJsonToken(parser);
        ODataUtilities.assertIsExpectedFieldName(parser, TableConstants.ErrorConstants.ERROR_MESSAGE);

        parser.nextToken();
        ODataUtilities.assertIsStartObjectJsonToken(parser);

        parser.nextValue();
        ODataUtilities.assertIsExpectedFieldName(parser, "lang");

        parser.nextValue();
        ODataUtilities.assertIsExpectedFieldName(parser, "value");
        errorInfo.setErrorMessage(parser.getValueAsString());

        parser.nextToken();
        ODataUtilities.assertIsEndObjectJsonToken(parser);

        parser.nextToken();

        // get innererror if it exists
        if (parser.getCurrentToken() == JsonToken.FIELD_NAME) {
            ODataUtilities.assertIsExpectedFieldName(parser, TableConstants.ErrorConstants.INNER_ERROR);
            errorInfo.getAdditionalDetails().putAll(parseJsonErrorException(parser));
            parser.nextToken();
        }

        // end code object
        ODataUtilities.assertIsEndObjectJsonToken(parser);

        // end odata.error object
        parser.nextToken();
        ODataUtilities.assertIsEndObjectJsonToken(parser);

        return errorInfo;
    }
}
