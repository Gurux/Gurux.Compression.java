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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import gurux.compression.enums.Operation;

/**
 * V.44 encoder compatible with Gurux.Compression.Net. Instances are reusable but
 * not thread safe or reentrant; callbacks must not call the same codec again.
 * Both peers must use matching options. Streams remain open on success or failure.
 */
public final class GXV44Encoder {
    private final GXv44Options options;
    private final int[] roots = new int[256];
    private final List<EncoderEntry> nodes = new ArrayList<>();
    private int ordinalSize = 7;
    private int codewordSize = 6;
    private boolean hex;
    private Consumer<GXV44TraceArgs> onTrace;

    /** Creates an encoder with reference default options. */
    public GXV44Encoder() { this(new GXv44Options()); }

    /** Creates an encoder using a validated copy of the supplied settings.
     * @param options non-null negotiated settings
     * @throws IllegalArgumentException if settings are unsupported
     */
    public GXV44Encoder(GXv44Options options) {
        this.options = Objects.requireNonNull(options, "options").snapshot();
        hex = options.isHex();
    }

    /** Returns current ordinal width, restored on REINIT.
     * @return Current ordinal width, restored on REINIT.
     */
    public int getOrdinalSize() { return ordinalSize; }
    /** Returns current codeword width, restored on REINIT.
     * @return Current codeword width, restored on REINIT.
     */
    public int getCodewordSize() { return codewordSize; }
    /** Indicates whether new diagnostics use hexadecimal text.
     * @return Whether new diagnostics use hexadecimal text.
     */
    public boolean isHex() { return hex; }
    /** Controls formatting of subsequently captured diagnostic values.
     * @param value true to use hexadecimal text
     */
    public void setHex(boolean value) { hex = value; }
    /** Returns current trace listener, or null.
     * @return Current trace listener, or null.
     */
    public Consumer<GXV44TraceArgs> getOnTrace() { return onTrace; }
    /** Registers an optional listener; no trace bit strings are made when absent.
     * @param value callback invoked synchronously, or null
     */
    public void setOnTrace(Consumer<GXV44TraceArgs> value) { onTrace = value; }

    /** Returns an immutable detached snapshot of encoder dictionary segments.
     * @return An immutable detached snapshot of encoder dictionary segments.
     */
    public List<GXv44Node> getNodeTree() {
        List<GXv44Node> result = new ArrayList<>(nodes.size());
        for (EncoderEntry node : nodes) result.add(node.snapshot());
        return Collections.unmodifiableList(result);
    }

    /** Returns immutable root-byte to first-codeword pairs; bytes are unsigned ints.
     * @return Immutable root-byte to first-codeword pairs; bytes are unsigned ints.
     */
    public List<Map.Entry<Integer, Integer>> getRootArray() {
        List<Map.Entry<Integer, Integer>> result = new ArrayList<>();
        for (int i = 0; i < roots.length; ++i) {
            if (roots[i] != 0) result.add(new AbstractMap.SimpleImmutableEntry<>(i, roots[i]));
        }
        return Collections.unmodifiableList(result);
    }

    /** Releases diagnostic values while retaining dictionary coordinates and links. */
    public void clearDiagnostics() {
        for (EncoderEntry node : nodes) node.value = "";
    }

    /** Clears dictionary links and restores negotiated widths. */
    public void reset() {
        Arrays.fill(roots, 0);
        nodes.clear();
        ordinalSize = options.getDefaultOrdinalSize();
        codewordSize = options.getDefaultCodewordSize();
    }

    /** Compresses ASCII text; encode other character sets explicitly to bytes.
     * @param input non-null text; non-ASCII characters are replaced with '?'
     * @return Complete zero-padded V.44 stream, including FLUSH for empty input
     * @throws CancellationException if the listener requests a stop
     */
    public byte[] compress(String input) {
        return compress(Objects.requireNonNull(input, "input").getBytes(StandardCharsets.US_ASCII));
    }

    /** Compresses one independent byte array without modifying it.
     * @param input non-null source bytes
     * @return Complete V.44 stream
     * @throws CancellationException if the listener requests a stop
     */
    public byte[] compress(byte[] input) {
        Objects.requireNonNull(input, "input");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitWriter writer = new BitWriter(output, onTrace != null);
        try {
            encodeBlock(input, input.length, true, writer);
            writer.align();
        } catch (IOException impossible) {
            throw new AssertionError(impossible);
        }
        return output.toByteArray();
    }

    /** Compresses using at most min(historyLimit, 65536) buffered input bytes.
     * Neither stream is closed or flushed. Already written bytes remain on failure.
     * @param input readable source
     * @param output writable destination
     * @throws IOException if input or output fails
     * @throws CancellationException if the listener requests a stop
     */
    public void compress(InputStream input, OutputStream output) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        if ((Object)input == output) throw new IllegalArgumentException(Messages.get("streams"));
        byte[] block = new byte[Math.min(options.getHistoryLimit(), 65536)];
        BitWriter writer = new BitWriter(output, onTrace != null);
        int pending = -1;
        do {
            int count = 0;
            if (pending >= 0) block[count++] = (byte)pending;
            while (count < block.length) {
                int read = input.read(block, count, block.length - count);
                if (read < 0) break;
                if (read == 0) {
                    int next = input.read();
                    if (next < 0) break;
                    block[count++] = (byte)next;
                } else count += read;
            }
            pending = input.read();
            encodeBlock(block, count, pending < 0, writer);
            if (pending >= 0) {
                writer.write(1, 1);
                writer.write(3, codewordSize);
            }
        } while (pending >= 0);
        writer.align();
    }

    private void trace(Operation operation, Object value, int index, byte[] input,
            int count, BitWriter writer) {
        Consumer<GXV44TraceArgs> listener = onTrace;
        if (listener == null) return;
        GXV44TraceArgs args = new GXV44TraceArgs(true, operation, value, index, writer.bits(),
                Arrays.copyOf(input, count), getNodeTree(), null);
        listener.accept(args);
        if (args.isStop()) throw new CancellationException(Messages.get("cancelled"));
    }

    private void ordinal(int ch, Operation previous, BitWriter writer) throws IOException {
        if (ch > 127 && ordinalSize == 7) {
            writer.write(1, 1);
            writer.write(2, codewordSize);
            ++ordinalSize;
        } else if (previous == Operation.CODEWORD) writer.write(0, 1);
        writer.write(0, 1);
        writer.write(ch, ordinalSize);
    }

    private void string(int code, int extension, BitWriter writer) throws IOException {
        writer.write(1, 1);
        while (code >= (1 << codewordSize)) {
            writer.write(2, codewordSize++);
            writer.write(1, 1);
        }
        writer.write(code, codewordSize);
        if (extension != 0) {
            writer.write(2, 2);
            writer.extension(extension, options.getMaximumStringLength().getExtensionBits());
        }
    }

    private void sibling(int head, EncoderEntry entry) {
        EncoderEntry current = nodes.get(head - 4);
        while (current.sideIndex != 0) current = nodes.get(current.sideIndex - 4);
        current.sideIndex = entry.codeword;
    }

    private void child(EncoderEntry parent, EncoderEntry entry) {
        if (parent.downIndex == 0) parent.downIndex = entry.codeword;
        else sibling(parent.downIndex, entry);
    }

    private boolean matches(EncoderEntry node, byte[] input, int cursor, int end, int history) {
        if (node.length > end - cursor || node.position + node.length > history) return false;
        for (int i = 0; i < node.length; ++i) {
            if (input[cursor + i] != input[node.position + i]) return false;
        }
        return true;
    }

    private void encodeBlock(byte[] input, int count, boolean flush, BitWriter writer) throws IOException {
        reset();
        int end = Math.min(count, options.getHistoryLimit());
        int history = 0;
        int maximum = options.getMaximumStringLength().getValue();
        Operation previous = Operation.ORDINAL;
        trace(Operation.START, null, 0, input, count, writer);
        int i = 0;
        while (i < count) {
            if (i == end || 4 + nodes.size() >= options.getMaxCodewords()) {
                writer.write(1, 1);
                writer.write(3, codewordSize);
                reset();
                history = i;
                end = i + Math.min(options.getHistoryLimit(), count - i);
                previous = Operation.NONE;
                trace(Operation.REINIT, null, i, input, count, writer);
            }
            int ch = input[i] & 255;
            ++history;
            int head = roots[ch];
            int best = -1;
            int total = 1;
            int level = head == 0 ? -1 : head - 4;
            int cursor = i + 1;
            while (level >= 0) {
                EncoderEntry node = nodes.get(level);
                if (total + node.length <= maximum && matches(node, input, cursor, end, history)) {
                    best = level;
                    total += node.length;
                    cursor += node.length;
                    level = node.downIndex == 0 ? -1 : node.downIndex - 4;
                } else level = node.sideIndex == 0 ? -1 : node.sideIndex - 4;
            }
            if (best < 0) {
                ordinal(ch, previous, writer);
                previous = Operation.ORDINAL;
                EncoderEntry entry = new EncoderEntry(4 + nodes.size(), i + 1, 1,
                        DiagnosticValue.format(input, i, 1, hex));
                nodes.add(entry);
                if (i + 1 < count) {
                    if (head == 0) roots[ch] = entry.codeword;
                    else sibling(head, entry);
                }
                ++i;
                trace(Operation.ORDINAL, entry.snapshot(), i, input, count, writer);
                continue;
            }
            EncoderEntry matched = nodes.get(best);
            history += total - 1;
            int extension = 0;
            int source = matched.position + matched.length;
            while (extension < maximum - total && i + total + extension < end
                    && source + extension < history
                    && input[i + total + extension] == input[source + extension]) {
                ++history;
                ++extension;
            }
            EncoderEntry entry = new EncoderEntry(4 + nodes.size(), i + total,
                    extension == 0 ? 1 : extension,
                    DiagnosticValue.format(input, i, total + extension, hex));
            entry.type = extension == 0 ? Operation.CODEWORD : Operation.STRING_EXTENSION;
            if (extension != 0 || total < maximum) {
                child(matched, entry);
                nodes.add(entry);
            }
            string(matched.codeword, extension, writer);
            previous = entry.type;
            trace(entry.type, entry.snapshot(), i, input, count, writer);
            trace(Operation.PROCESSING, matched.codeword, i, input, count, writer);
            i += total + extension;
        }
        if (flush) {
            writer.write(1, 1);
            writer.write(1, codewordSize);
        }
        trace(Operation.END, null, 0, input, count, writer);
    }
}
