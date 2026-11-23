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

import java.util.Collections;
import java.util.List;
import gurux.compression.enums.Operation;

/** Detached trace event. Set stop to cancel processing with CancellationException. */
public final class GXV44TraceArgs {
    private final boolean compress;
    private final Operation operation;
    private final Object value;
    private final int index;
    private final String compressed;
    private final byte[] uncompressed;
    private final List<GXv44Node> nodeTree;
    private final List<GXv44DecoderEntry> stringCollection;
    private boolean stop;

    GXV44TraceArgs(boolean compress, Operation operation, Object value, int index,
            String compressed, byte[] uncompressed, List<GXv44Node> nodeTree,
            List<GXv44DecoderEntry> stringCollection) {
        this.compress = compress;
        this.operation = operation;
        this.value = value;
        this.index = index;
        this.compressed = compressed;
        this.uncompressed = uncompressed == null ? null : uncompressed.clone();
        this.nodeTree = nodeTree == null ? Collections.emptyList() : nodeTree;
        this.stringCollection = stringCollection == null ? Collections.emptyList() : stringCollection;
    }

    /** Returns true for compression, false for decompression.
     * @return True for compression, false for decompression.
     */
    public boolean isCompress() { return compress; }
    /** Returns the operation reported by this event.
     * @return The operation reported by this event.
     */
    public Operation getOperation() { return operation; }
    /** Returns associated immutable node, numeric codeword, error text, or null.
     * @return Associated immutable node, numeric codeword, error text, or null.
     */
    public Object getValue() { return value; }
    /** Returns associated input or compressed bit position.
     * @return Associated input or compressed bit position.
     */
    public int getIndex() { return index; }
    /** Returns transmission-order bits, or null when decoding a stream.
     * @return Transmission-order bits, or null when decoding a stream.
     */
    public String getCompressed() { return compressed; }
    /** Returns a copy of the captured bytes, or null.
     * @return A copy of the captured bytes, or null.
     */
    public byte[] getUncompressed() { return uncompressed == null ? null : uncompressed.clone(); }
    /** Returns immutable encoder dictionary snapshot.
     * @return Immutable encoder dictionary snapshot.
     */
    public List<GXv44Node> getNodeTree() { return nodeTree; }
    /** Returns immutable decoder dictionary snapshot.
     * @return Immutable decoder dictionary snapshot.
     */
    public List<GXv44DecoderEntry> getStringCollection() { return stringCollection; }
    /** Indicates whether the callback has requested cancellation.
     * @return Whether the callback has requested cancellation.
     */
    public boolean isStop() { return stop; }
    /** Requests cancellation after this callback returns.
     * @param value true to stop processing
     */
    public void setStop(boolean value) { stop = value; }

    /** Formats an event using the reference diagnostic operation names.
     * @return A developer-readable trace description
     */
    @Override public String toString() {
        switch (operation) {
        case START: return (compress ? "Compress " : "Decompress ") + "Start";
        case END: return (compress ? "Compress " : "Decompress ") + "End";
        case ORDINAL:
        case CODEWORD:
        case STRING_EXTENSION: return value == null ? "" : value.toString();
        case STEP_UP_ORDINAL: return "StepUp Ordinal";
        case STEP_UP_CODEWORD: return "StepUp Codeword";
        case FLUSH: return "Flush";
        case REINIT: return "ReInit";
        case ERROR: return "Error";
        case PROCESSING: return "CW(" + value + ")";
        default: return "";
        }
    }
}
