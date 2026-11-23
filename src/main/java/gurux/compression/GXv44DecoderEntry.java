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

/** Immutable, detached decoder string diagnostic snapshot. */
public final class GXv44DecoderEntry {
    private final int codeword;
    private final int position;
    private final int length;
    private final String value;

    GXv44DecoderEntry(int codeword, int position, int length, String value) {
        this.codeword = codeword;
        this.position = position;
        this.length = length;
        this.value = value;
    }

    /** Returns dictionary codeword.
     * @return Dictionary codeword.
     */
    public int getCodeword() { return codeword; }

    /** Returns inclusive end position in history.
     * @return Inclusive end position in history.
     */
    public int getPosition() { return position; }

    /** Returns complete string length.
     * @return Complete string length.
     */
    public int getLength() { return length; }

    /** Returns detached diagnostic text.
     * @return Detached diagnostic text.
     */
    public String getValue() { return value; }

    /** Returns inclusive start position of this dictionary string.
     * @return Inclusive start position of this dictionary string.
     */
    public int getStartPos() { return position - length + 1; }

    /** Returns reference-style dictionary coordinates and diagnostic text.
     * @return A developer-readable description of this string
     */
    @Override public String toString() {
        return "{Codeword:" + codeword + " Position:" + position + " Length:" + length + " Value:" + value + "}";
    }
}
