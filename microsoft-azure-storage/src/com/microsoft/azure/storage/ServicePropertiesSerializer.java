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
import java.util.Iterator;

import org.xmlpull.v1.XmlSerializer;

import com.microsoft.azure.storage.core.SR;
import com.microsoft.azure.storage.core.Utility;

/**
 * RESERVED FOR INTERNAL USE. A class used to serialize ServiceProperties to a byte array.
 */
final class ServicePropertiesSerializer {

    /**
     * Writes the contents of the ServiceProperties to the stream in xml format.
     * 
     * @return a byte array of the content to write to the stream.
     * @throws IOException
     *             if there is an error writing the service properties.
     * @throws IllegalStateException
     *             if there is an error writing the service properties.
     * @throws IllegalArgumentException
     *             if there is an error writing the service properties.
     */
    public static byte[] serializeToByteArray(final ServiceProperties properties) throws IllegalArgumentException,
            IllegalStateException, IOException {
        final StringWriter outWriter = new StringWriter();
        final XmlSerializer xmlw = Utility.getXmlSerializer(outWriter);

        // default is UTF8
        xmlw.startDocument(Constants.UTF8_CHARSET, true);
        xmlw.startTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.STORAGE_SERVICE_PROPERTIES_ELEMENT);

        if (properties.getLogging() != null) {
            // Logging Properties
            writeLoggingProperties(xmlw, properties.getLogging());
        }

        if (properties.getHourMetrics() != null) {
            // Hour Metrics
            writeMetricsProperties(xmlw, properties.getHourMetrics(), Constants.AnalyticsConstants.HOUR_METRICS_ELEMENT);
        }

        if (properties.getMinuteMetrics() != null) {
            // Minute Metrics
            writeMetricsProperties(xmlw, properties.getMinuteMetrics(),
                    Constants.AnalyticsConstants.MINUTE_METRICS_ELEMENT);
        }

        if (properties.getCors() != null) {
            // CORS Properties
            writeCorsProperties(xmlw, properties.getCors());
        }

        // Default Service Version
        if (properties.getDefaultServiceVersion() != null) {
            Utility.serializeElement(xmlw, Constants.AnalyticsConstants.DEFAULT_SERVICE_VERSION,
                    properties.getDefaultServiceVersion());
        }

        // end StorageServiceProperties
        xmlw.endTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.STORAGE_SERVICE_PROPERTIES_ELEMENT);

        // end doc
        xmlw.endDocument();

        return outWriter.toString().getBytes(Constants.UTF8_CHARSET);
    }

    /**
     * Writes the retention policy to the XMLStreamWriter.
     * 
     * @param xmlw
     *            the XMLStreamWriter to write to.
     * @param val
     *            the nullable Integer indicating if the retention policy is enabled, and how long
     * @throws IOException
     *             if there is an error writing the service properties.
     * @throws IllegalStateException
     *             if there is an error writing the service properties.
     * @throws IllegalArgumentException
     *             if there is an error writing the service properties.
     */
    private static void writeRetentionPolicy(final XmlSerializer xmlw, final Integer val)
            throws IllegalArgumentException, IllegalStateException, IOException {
        xmlw.startTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.RETENTION_POLICY_ELEMENT);

        // Enabled
        Utility.serializeElement(xmlw, Constants.AnalyticsConstants.ENABLED_ELEMENT, val != null ? Constants.TRUE
                : Constants.FALSE);

        if (val != null) {
            // Days
            Utility.serializeElement(xmlw, Constants.AnalyticsConstants.DAYS_ELEMENT, val.toString());
        }

        // End Retention Policy
        xmlw.endTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.RETENTION_POLICY_ELEMENT);
    }

    /**
     * Writes the given CORS properties to the XMLStreamWriter.
     * 
     * @param xmlw
     *            the XMLStreamWriter to write to.
     * @param cors
     *            the CORS Properties to be written.
     * @throws IOException
     *             if there is an error writing the service properties.
     * @throws IllegalStateException
     *             if there is an error writing the service properties.
     * @throws IllegalArgumentException
     *             if there is an error writing the service properties.
     */
    private static void writeCorsProperties(final XmlSerializer xmlw, final CorsProperties cors)
            throws IllegalArgumentException, IllegalStateException, IOException {
        Utility.assertNotNull("CorsRules", cors.getCorsRules());

        // CORS
        xmlw.startTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.CORS_ELEMENT);

        for (CorsRule rule : cors.getCorsRules()) {
            if (rule.getAllowedOrigins().isEmpty() || rule.getAllowedMethods().isEmpty()
                    || rule.getMaxAgeInSeconds() < 0) {
                throw new IllegalArgumentException(SR.INVALID_CORS_RULE);
            }

            xmlw.startTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.CORS_RULE_ELEMENT);

            Utility.serializeElement(xmlw, Constants.AnalyticsConstants.ALLOWED_ORIGINS_ELEMENT,
                    joinToString(rule.getAllowedOrigins(), ","));

            Utility.serializeElement(xmlw, Constants.AnalyticsConstants.ALLOWED_METHODS_ELEMENT,
                    joinToString(rule.getAllowedMethods(), ","));

            Utility.serializeElement(xmlw, Constants.AnalyticsConstants.EXPOSED_HEADERS_ELEMENT,
                    joinToString(rule.getExposedHeaders(), ","));

            Utility.serializeElement(xmlw, Constants.AnalyticsConstants.ALLOWED_HEADERS_ELEMENT,
                    joinToString(rule.getAllowedHeaders(), ","));

            Utility.serializeElement(xmlw, Constants.AnalyticsConstants.MAX_AGE_IN_SECONDS_ELEMENT,
                    Integer.toString(rule.getMaxAgeInSeconds()));

            xmlw.endTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.CORS_RULE_ELEMENT);
        }

        // end CORS
        xmlw.endTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.CORS_ELEMENT);
    }

    /**
     * Writes the given metrics properties to the XMLStreamWriter.
     * 
     * @param xmlw
     *            the XMLStreamWriter to write to.
     * @param metrics
     *            the metrics properties to be written.
     * @param metricsName
     *            the type of metrics properties to be written (Hour or Minute)
     * @throws IOException
     *             if there is an error writing the service properties.
     * @throws IllegalStateException
     *             if there is an error writing the service properties.
     * @throws IllegalArgumentException
     *             if there is an error writing the service properties.
     */
    private static void writeMetricsProperties(final XmlSerializer xmlw, final MetricsProperties metrics,
            final String metricsName) throws IllegalArgumentException, IllegalStateException, IOException {
        Utility.assertNotNull("metrics.Configuration", metrics.getMetricsLevel());

        // Metrics
        xmlw.startTag(Constants.EMPTY_STRING, metricsName);

        // Version
        Utility.serializeElement(xmlw, Constants.AnalyticsConstants.VERSION_ELEMENT, metrics.getVersion());

        // Enabled
        Utility.serializeElement(xmlw, Constants.AnalyticsConstants.ENABLED_ELEMENT,
                metrics.getMetricsLevel() != MetricsLevel.DISABLED ? Constants.TRUE : Constants.FALSE);

        if (metrics.getMetricsLevel() != MetricsLevel.DISABLED) {
            // Include APIs
            Utility.serializeElement(xmlw, Constants.AnalyticsConstants.INCLUDE_APIS_ELEMENT,
                    metrics.getMetricsLevel() == MetricsLevel.SERVICE_AND_API ? Constants.TRUE : Constants.FALSE);
        }

        // Retention Policy
        writeRetentionPolicy(xmlw, metrics.getRetentionIntervalInDays());

        // end Metrics
        xmlw.endTag(Constants.EMPTY_STRING, metricsName);
    }

    /**
     * Writes the given logging properties to the XMLStreamWriter.
     * 
     * @param xmlw
     *            the XMLStreamWriter to write to.
     * @param cors
     *            the logging properties to be written.
     * @throws IOException
     *             if there is an error writing the service properties.
     * @throws IllegalStateException
     *             if there is an error writing the service properties.
     * @throws IllegalArgumentException
     *             if there is an error writing the service properties.
     */
    private static void writeLoggingProperties(final XmlSerializer xmlw, final LoggingProperties logging)
            throws IllegalArgumentException, IllegalStateException, IOException {
        Utility.assertNotNull("logging.LogOperationTypes", logging.getLogOperationTypes());

        // Logging
        xmlw.startTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.LOGGING_ELEMENT);

        // Version
        Utility.serializeElement(xmlw, Constants.AnalyticsConstants.VERSION_ELEMENT, logging.getVersion());

        // Delete
        Utility.serializeElement(xmlw, Constants.AnalyticsConstants.DELETE_ELEMENT, logging.getLogOperationTypes()
                .contains(LoggingOperations.DELETE) ? Constants.TRUE : Constants.FALSE);

        // Read
        Utility.serializeElement(xmlw, Constants.AnalyticsConstants.READ_ELEMENT, logging.getLogOperationTypes()
                .contains(LoggingOperations.READ) ? Constants.TRUE : Constants.FALSE);

        // Write
        Utility.serializeElement(xmlw, Constants.AnalyticsConstants.WRITE_ELEMENT, logging.getLogOperationTypes()
                .contains(LoggingOperations.WRITE) ? Constants.TRUE : Constants.FALSE);

        // Retention Policy
        writeRetentionPolicy(xmlw, logging.getRetentionIntervalInDays());

        // end Logging
        xmlw.endTag(Constants.EMPTY_STRING, Constants.AnalyticsConstants.LOGGING_ELEMENT);
    }

    /**
     * Concatenate an Iterable<?> set of items with a delimiter between each
     * 
     * @param iterable
     *            the set of items to concatenate
     * @param delimiter
     *            the character to put between each item
     * @return the concatenated string
     */
    private static String joinToString(Iterable<?> iterable, String delimiter) {
        StringBuilder builder = new StringBuilder();
        Iterator<?> iter = iterable.iterator();
        while (iter.hasNext()) {
            builder.append(iter.next());
            if (iter.hasNext()) {
                builder.append(delimiter);
            }
        }
        return builder.toString();
    }
}
