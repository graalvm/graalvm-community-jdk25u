/*
 * Copyright (c) 2026, 2026, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package com.oracle.svm.test.sci;

import com.oracle.svm.core.NeverInlineTrivial;

/**
 * Test that single-callsite inlining respects fallback size threshold. Deep nested inlining. Each
 * method does busy work to increase it's node size.
 */
public class SciFallbackTest {
    public static void main(String[] args) {
        System.out.println("Result: " + chainLink1(16));
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining so SCI handles this")
    private static int chainLink1(int v) {
        // Enough computation to have non-trivial graph size
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return chainLink2(result);
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining")
    private static int chainLink2(int v) {
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return chainLink3(result);
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining")
    private static int chainLink3(int v) {
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return chainLink4(result);
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining")
    private static int chainLink4(int v) {
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return chainLink5(result);
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining")
    private static int chainLink5(int v) {
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return chainLink6(result);
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining")
    private static int chainLink6(int v) {
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return chainLink7(result);
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining")
    private static int chainLink7(int v) {
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return chainLink8(result);
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining")
    private static int chainLink8(int v) {
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return chainLink9(result);
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining")
    private static int chainLink9(int v) {
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return chainLink10(result);
    }

    @NeverInlineTrivial(reason = "Prevent trivial inlining")
    private static int chainLink10(int v) {
        int result = v;
        for (int i = 0; i < 10; i++) {
            result = result * 3 + i;
            if (result > 1000) {
                result = result % 1000;
            }
        }
        for (int i = 0; i < 11; i++) {
            result = result * 4 + i;
            if (result > 1001) {
                result = result % 1001;
            }
        }
        return result;
    }
}
