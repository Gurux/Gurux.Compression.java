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

import java.io.IOException;
import java.io.OutputStream;

/** Writes LSB-first fields directly to caller-owned output. */
final class BitWriter {
    private final OutputStream output;
    private final StringBuilder trace;
    private int buffer;
    private int used;

    BitWriter(OutputStream output, boolean tracing) {
        this.output = output;
        trace = tracing ? new StringBuilder() : null;
    }

    void write(int value, int count) throws IOException {
        for (int i = 0; i < count; ++i) {
            int bit = (value >>> i) & 1;
            buffer |= bit << used++;
            if (trace != null) trace.append(bit);
            if (used == 8) {
                output.write(buffer);
                buffer = 0;
                used = 0;
            }
        }
    }

    void align() throws IOException {
        if (used != 0) write(0, 8 - used);
    }

    String bits() { return trace == null ? null : trace.toString(); }

    void extension(int length, int longWidth) throws IOException {
        if (length == 1) write(1, 1);
        else if (length <= 4) {
            write(0, 1);
            write(length - 1, 2);
        } else if (length <= 12) {
            write(0, 4);
            write(length - 5, 3);
        } else {
            write(0, 3);
            write(1, 1);
            write(length - 13, longWidth);
        }
    }
}
