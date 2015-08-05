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
package com.microsoft.azure.storage.runners;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.microsoft.azure.storage.blob.BlobOutputStreamTests;
import com.microsoft.azure.storage.blob.CloudAppendBlobTests;
import com.microsoft.azure.storage.blob.CloudBlobClientTests;
import com.microsoft.azure.storage.blob.CloudBlobContainerTests;
import com.microsoft.azure.storage.blob.CloudBlobDirectoryTests;
import com.microsoft.azure.storage.blob.CloudBlockBlobTests;
import com.microsoft.azure.storage.blob.CloudPageBlobTests;
import com.microsoft.azure.storage.blob.LeaseTests;
import com.microsoft.azure.storage.blob.SasTests;

public class BlobTestSuite extends TestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite("BlobTestSuite");
        suite.addTestSuite(BlobOutputStreamTests.class);
        suite.addTestSuite(CloudBlobClientTests.class);
        suite.addTestSuite(CloudBlobContainerTests.class);
        suite.addTestSuite(CloudBlobDirectoryTests.class);
        suite.addTestSuite(CloudAppendBlobTests.class);
        suite.addTestSuite(CloudBlockBlobTests.class);
        suite.addTestSuite(CloudPageBlobTests.class);
        suite.addTestSuite(LeaseTests.class);
        suite.addTestSuite(SasTests.class);
        return suite;
    }
}
