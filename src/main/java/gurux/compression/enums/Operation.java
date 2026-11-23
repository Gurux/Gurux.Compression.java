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

/** Data tokens and diagnostic events, with the reference numeric identifiers. */
public enum Operation {
    /** No previous token. */
    NONE(-3),
    /** An operation starts. */
    START(-2),
    /** An operation ends. */
    END(-1),
    /** A literal byte. */
    ORDINAL(0),
    /** A dictionary string. */
    CODEWORD(1),
    /** A continuation after a dictionary string. */
    STRING_EXTENSION(2),
    /** Ordinal width increases. */
    STEP_UP_ORDINAL(3),
    /** Codeword width increases. */
    STEP_UP_CODEWORD(4),
    /** Byte-aligned FLUSH control. */
    FLUSH(5),
    /** History and dictionary initialization. */
    REINIT(6),
    /** Invalid compressed data. */
    ERROR(7),
    /** A dictionary codeword is being processed. */
    PROCESSING(8);

    private final int value;
    Operation(int value) { this.value = value; }
    /** Returns numeric identifier used by the reference diagnostics.
     * @return Numeric identifier used by the reference diagnostics.
     */
    public int getValue() { return value; }
}
