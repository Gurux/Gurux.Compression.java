// --------------------------------------------------------------------------
// Gurux Ltd
//
// Copyright (c) Gurux Ltd.
//
// This file is part of Gurux Device Framework.
//
// Commit: $Format:%H$
// Date:   $Format:%cI$
//
// This software is available under either of the following licenses:
//
// 1. GNU General Public License, version 2 only (GPL-2.0-only).
//    See the LICENSE file included with this distribution.
//
// 2. A commercial license granted by Gurux Ltd under a separate written
//    agreement. Use under this option requires a valid commercial license
//    and is governed by the terms of that agreement.
//
// If you have not obtained a commercial license from Gurux Ltd,
// use of this software is governed by GPL-2.0-only.
//
// Commercial licensing: https://www.gurux.fi/
//
// SPDX-License-Identifier: GPL-2.0-only OR LicenseRef-Gurux-Commercial
// --------------------------------------------------------------------------
package gurux.compression;

import java.nio.charset.StandardCharsets;

/** Formats bounded dictionary slices without exposing caller-owned storage. */
final class DiagnosticValue {
    private DiagnosticValue() { }
    static String format(byte[] input, int start, int count, boolean hex) {
        count = Math.max(0, Math.min(count, input.length - start));
        if (!hex) {
            for (int i = 0; i < count; ++i) {
                int b = input[start + i] & 255;
                if (b > 127 || b == 0 || b == 10 || b == 13) { hex = true; break; }
            }
        }
        if (!hex) return new String(input, start, count, StandardCharsets.US_ASCII);
        char[] digits = "0123456789ABCDEF".toCharArray();
        StringBuilder result = new StringBuilder(2 + count * 2);
        result.append("0x");
        for (int i = 0; i < count; ++i) {
            int b = input[start + i] & 255;
            result.append(digits[b >>> 4]).append(digits[b & 15]);
        }
        return result.toString();
    }
}
