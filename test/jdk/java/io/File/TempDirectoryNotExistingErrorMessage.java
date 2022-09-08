/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/* @test
 * @bug 8290313
 * @library /test/lib
 * @summary Produce warning when user specified java.io.tmpdir directory doesn't exist
 */

import jdk.test.lib.process.ProcessTools;

import java.io.File;

public class TempDirectoryNotExistingErrorMessage {

    public static void main(String ... args) throws Exception{

        String userDir = System.getProperty("user.home");

        if (args.length != 0) {
            File tmpFile = File.createTempFile("prefix", ".suffix");
        } else {

            String timeStamp = System.currentTimeMillis() + "";
            String tempDir = userDir + "\\non-existing-" + timeStamp;
            String errorMsg1 = "WARNING: java.io.tmpdir location does not exist";

            test(errorMsg1, "-Djava.io.tmpdir=" + tempDir, "TempDirectoryNotExistingErrorMessage", "runTest");

            test(errorMsg1, "-Djava.io.tmpdir=" + tempDir, "-Djava.security.manager=allow",
                    "TempDirectoryNotExistingErrorMessage", "runTest");
        }
    }

    private static void test( String errorMsg, String ... options) throws Exception {
        ProcessTools.executeTestJvm(options)
                .shouldContain(errorMsg);
    }
}