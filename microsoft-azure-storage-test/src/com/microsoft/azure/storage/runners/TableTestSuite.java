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

import com.microsoft.azure.storage.table.TableBatchOperationTests;
import com.microsoft.azure.storage.table.TableClientTests;
import com.microsoft.azure.storage.table.TableDateTests;
import com.microsoft.azure.storage.table.TableEscapingTests;
import com.microsoft.azure.storage.table.TableODataTests;
import com.microsoft.azure.storage.table.TableOperationTests;
import com.microsoft.azure.storage.table.TableQueryTests;
import com.microsoft.azure.storage.table.TableSerializerTests;
import com.microsoft.azure.storage.table.TableTests;

public class TableTestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite("TableTestSuite");
        suite.addTestSuite(TableBatchOperationTests.class);
        suite.addTestSuite(TableClientTests.class);
        suite.addTestSuite(TableDateTests.class);
        suite.addTestSuite(TableEscapingTests.class);
        suite.addTestSuite(TableODataTests.class);
        suite.addTestSuite(TableOperationTests.class);
        suite.addTestSuite(TableQueryTests.class);
        suite.addTestSuite(TableSerializerTests.class);
        suite.addTestSuite(TableTests.class);
        return suite;
    }
}
