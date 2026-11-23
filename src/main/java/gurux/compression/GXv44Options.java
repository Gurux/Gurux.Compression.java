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

import gurux.compression.enums.MaximumStringLength;

/** Mutable V.44 configuration. Codecs validate and copy it at construction. */
public final class GXv44Options {
    private int defaultCodewordSize = 6;
    private int defaultOrdinalSize = 7;
    private int maxCodewords = 1024;
    private MaximumStringLength maximumStringLength = MaximumStringLength.VALUE_255;
    private int historyLimit = 3072;
    private boolean hex = false;

    /** Creates the reference default configuration. */
    public GXv44Options() { }

    /** Returns initial codeword width in bits.
     * @return Initial codeword width in bits.
     */
    public int getDefaultCodewordSize() { return defaultCodewordSize; }

    /** Sets the configuration value; call validate to check all bounds.
     * @param value Initial codeword width in bits.
     */
    public void setDefaultCodewordSize(int value) { if (value < 6 || value > 16) throw new IllegalArgumentException(Messages.get("codewordSize")); this.defaultCodewordSize = value; }

    /** Returns initial ordinal width in bits.
     * @return Initial ordinal width in bits.
     */
    public int getDefaultOrdinalSize() { return defaultOrdinalSize; }

    /** Sets the configuration value; call validate to check all bounds.
     * @param value Initial ordinal width in bits.
     */
    public void setDefaultOrdinalSize(int value) { if (value < 7 || value > 8) throw new IllegalArgumentException(Messages.get("ordinalSize")); this.defaultOrdinalSize = value; }

    /** Returns maximum dictionary codewords including four reserved controls.
     * @return Maximum dictionary codewords including four reserved controls.
     */
    public int getMaxCodewords() { return maxCodewords; }

    /** Sets the configuration value; call validate to check all bounds.
     * @param value Maximum dictionary codewords including four reserved controls.
     */
    public void setMaxCodewords(int value) { this.maxCodewords = value; }

    /** Returns negotiated maximum dictionary string length.
     * @return Negotiated maximum dictionary string length.
     */
    public MaximumStringLength getMaximumStringLength() { return maximumStringLength; }

    /** Sets the configuration value; call validate to check all bounds.
     * @param value Negotiated maximum dictionary string length.
     */
    public void setMaximumStringLength(MaximumStringLength value) { this.maximumStringLength = value; }

    /** Returns maximum history bytes between REINIT controls.
     * @return Maximum history bytes between REINIT controls.
     */
    public int getHistoryLimit() { return historyLimit; }

    /** Sets the configuration value; call validate to check all bounds.
     * @param value Maximum history bytes between REINIT controls.
     */
    public void setHistoryLimit(int value) { this.historyLimit = value; }

    /** Indicates whether diagnostic values use hexadecimal formatting.
     * @return Whether diagnostic values use hexadecimal formatting.
     */
    public boolean isHex() { return hex; }

    /** Sets the configuration value; call validate to check all bounds.
     * @param value Whether diagnostic values use hexadecimal formatting.
     */
    public void setHex(boolean value) { this.hex = value; }

    /** Validates dictionary capacity, history bounds and string length.
     * @throws IllegalArgumentException if a setting is unsupported
     */
    public void validate() {
        if (maxCodewords < 256 || maxCodewords > 65535) throw new IllegalArgumentException(Messages.get("maxCodewords"));
        if (historyLimit < 512) throw new IllegalArgumentException(Messages.get("historyLimit"));
        if (maximumStringLength == null) throw new IllegalArgumentException(Messages.get("maximumLength"));
    }

    /** Creates a validated independent configuration snapshot. */
    GXv44Options snapshot() {
        validate();
        GXv44Options copy = new GXv44Options();
        copy.defaultCodewordSize = defaultCodewordSize;
        copy.defaultOrdinalSize = defaultOrdinalSize;
        copy.maxCodewords = maxCodewords;
        copy.maximumStringLength = maximumStringLength;
        copy.historyLimit = historyLimit;
        copy.hex = hex;
        return copy;
    }
}
