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
package gurux.compression.enums;

/** Negotiated V.44 maximum string lengths (N7T). */
public enum MaximumStringLength {
    /** Strings of at most 46 bytes. */
    VALUE_46(46, 5),
    /** Strings of at most 78 bytes. */
    VALUE_78(78, 6),
    /** Strings of at most 142 bytes. */
    VALUE_142(142, 7),
    /** Strings of at most 255 bytes. */
    VALUE_255(255, 8);

    private final int value;
    private final int extensionBits;

    MaximumStringLength(int value, int extensionBits) {
        this.value = value;
        this.extensionBits = extensionBits;
    }

    /** Returns maximum number of bytes in a dictionary string.
     * @return Maximum number of bytes in a dictionary string.
     */
    public int getValue() { return value; }

    /** Returns width of the long extension-length field.
     * @return Width of the long extension-length field.
     */
    public int getExtensionBits() { return extensionBits; }
}
