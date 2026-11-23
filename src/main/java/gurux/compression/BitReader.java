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
import java.io.InputStream;

/** Strict LSB-first stream reader with one byte of lookahead. */
final class BitReader {
    private final InputStream input;
    private int buffer;
    private int available;
    private long position;

    BitReader(InputStream input) { this.input = input; }

    int peek() throws IOException {
        if (available == 0) {
            buffer = input.read();
            if (buffer < 0) return -1;
            available = 8;
        }
        return buffer & 1;
    }

    int read(int count) throws IOException {
        while (available < count) {
            int next = input.read();
            if (next < 0) throw new IOException(Messages.get("truncated", position + available));
            buffer |= next << available;
            available += 8;
        }
        int result = buffer & ((1 << count) - 1);
        buffer >>>= count;
        available -= count;
        position += count;
        return result;
    }

    void align() throws IOException {
        int padding = (int)((8 - position % 8) % 8);
        if (padding != 0 && read(padding) != 0) throw new IOException(Messages.get("padding"));
    }

    int getPosition() { return (int)Math.min(Integer.MAX_VALUE, position); }

    int extension(gurux.compression.enums.MaximumStringLength maximum) throws IOException {
        if (read(1) == 1) return 1;
        int shortLength = read(2);
        if (shortLength != 0) return shortLength + 1;
        if (read(1) == 0) return read(3) + 5;
        int length = read(maximum.getExtensionBits()) + 13;
        if (length > maximum.getValue() - 2) throw new IOException(Messages.get("extension"));
        return length;
    }
}
