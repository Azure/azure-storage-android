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

public class AllTestSuite {
    public static Test suite() {
        TestSuite suite = new TestSuite("AllTestSuite");
        suite.addTest(CoreTestSuite.suite());
        suite.addTest(BlobTestSuite.suite());
        suite.addTest(FileTestSuite.suite());
        suite.addTest(QueueTestSuite.suite());
        suite.addTest(TableTestSuite.suite());
        suite.addTest(AnalyticsTestSuite.suite());
        return suite;
    }
}
