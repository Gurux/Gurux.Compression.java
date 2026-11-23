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

import gurux.compression.enums.Operation;

/** Mutable encoder dictionary segment, private to one codec instance. */
final class EncoderEntry {
    final int codeword;
    final int position;
    final int length;
    int downIndex;
    int sideIndex;
    Operation type = Operation.ORDINAL;
    String value;

    EncoderEntry(int codeword, int position, int length, String value) {
        this.codeword = codeword;
        this.position = position;
        this.length = length;
        this.value = value;
    }

    GXv44Node snapshot() {
        return new GXv44Node(codeword, position, length, downIndex, sideIndex, type, value);
    }
}
