/*
 * Copyright (C) 2022 THL A29 Limited, a Tencent company. All rights reserved.
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

 /*
 * @test
 * @bug 8293491
 * @summary Avoid unexpected deoptimization in loop exit due to incorrect branch profiling
 * @requires vm.compiler2.enabled & vm.compMode != "Xcomp"
 * @requires vm.opt.DeoptimizeALot != true
 * @library /test/lib
 * @build jdk.test.whitebox.WhiteBox
 * @run driver jdk.test.lib.helpers.ClassFileInstaller jdk.test.whitebox.WhiteBox
 * @run main/othervm -Xbootclasspath/a:.
 *                   -XX:-TieredCompilation -XX:+PrintCompilation
 *                   -XX:+UnlockDiagnosticVMOptions -XX:+WhiteBoxAPI
 *                   -XX:CompileCommand=quiet
 *                   -XX:CompileCommand=compileonly,compiler.profiling.TestUnexpectedLoopExitDeopt::test
 *                   compiler.profiling.TestUnexpectedLoopExitDeopt
 */

package compiler.profiling;

import jdk.test.lib.Asserts;
import jdk.test.whitebox.WhiteBox;

public class TestUnexpectedLoopExitDeopt {
    private static final WhiteBox WB = WhiteBox.getWhiteBox();
    private static final int N = 20000000;
    private static int d[] = new int[N];

    private static void run() {
        System.out.println(test(d));
    }

    private static int test(int[] a) {
        int sum = 0;
        for(int i = 0; i < a.length; i++) {
            sum += a[i];
        }
        return sum;
    }

    public static void main(String[] args) throws Exception {
        run();
        Asserts.assertEQ(0, WB.getDeoptCount(), "Unexpected deoptimization detected.");
    }
}
