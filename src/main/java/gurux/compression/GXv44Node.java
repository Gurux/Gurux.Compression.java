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

/** Immutable, detached encoder segment diagnostic snapshot. */
public final class GXv44Node {
    private final int codeword;
    private final int position;
    private final int length;
    private final int downIndex;
    private final int sideIndex;
    private final Operation type;
    private final String value;

    GXv44Node(int codeword, int position, int length, int downIndex, int sideIndex, Operation type, String value) {
        this.codeword = codeword;
        this.position = position;
        this.length = length;
        this.downIndex = downIndex;
        this.sideIndex = sideIndex;
        this.type = type;
        this.value = value;
    }

    /** Returns dictionary codeword.
     * @return Dictionary codeword.
     */
    public int getCodeword() { return codeword; }

    /** Returns start offset of the encoder segment.
     * @return Start offset of the encoder segment.
     */
    public int getPosition() { return position; }

    /** Returns number of bytes in the segment.
     * @return Number of bytes in the segment.
     */
    public int getLength() { return length; }

    /** Returns child codeword, or zero.
     * @return Child codeword, or zero.
     */
    public int getDownIndex() { return downIndex; }

    /** Returns sibling codeword, or zero.
     * @return Sibling codeword, or zero.
     */
    public int getSideIndex() { return sideIndex; }

    /** Returns token that created the entry.
     * @return Token that created the entry.
     */
    public Operation getType() { return type; }

    /** Returns detached diagnostic text.
     * @return Detached diagnostic text.
     */
    public String getValue() { return value; }

    /** Returns reference-style dictionary coordinates and diagnostic text.
     * @return A developer-readable description of this segment
     */
    @Override public String toString() {
        if (length == 0) return "'" + value + "' ";
        return "Codeword:" + codeword + " " + value + " Position:" + position + " Length: " + length
                + (downIndex == 0 ? "" : " DownIndex: " + downIndex)
                + (sideIndex == 0 ? "" : " SideIndex: " + sideIndex);
    }
}
