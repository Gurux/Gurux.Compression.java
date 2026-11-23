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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.function.Consumer;
import gurux.compression.enums.Operation;

/**
 * Strict V.44 decoder compatible with Gurux.Compression.Net. Instances are
 * reusable but not thread safe or reentrant. Streams remain open on failure.
 * Malformed or incomplete compressed data is reported as IOException.
 */
public final class GXV44Decoder {
    private final GXv44Options options;
    private final List<GXv44DecoderEntry> strings = new ArrayList<>();
    private byte[] history;
    private int size;
    private int lastValuePosition;
    private Operation previous = Operation.NONE;
    private int previousCodeword;
    private int previousOrdinal;
    private int ordinalSize = 7;
    private int codewordSize = 6;
    private Consumer<GXV44TraceArgs> onTrace;

    /** Creates a decoder with reference default settings. */
    public GXV44Decoder() { this(new GXv44Options()); }

    /** Copies and validates the negotiated settings.
     * @param options non-null settings matching the encoder
     * @throws IllegalArgumentException if settings are unsupported
     */
    public GXV44Decoder(GXv44Options options) {
        this.options = Objects.requireNonNull(options, "options").snapshot();
        history = new byte[Math.min(4096, options.getHistoryLimit())];
    }

    /** Returns current ordinal width in bits.
     * @return Current ordinal width in bits.
     */
    public int getOrdinalSize() { return ordinalSize; }
    /** Returns current dictionary codeword width in bits.
     * @return Current dictionary codeword width in bits.
     */
    public int getCodewordSize() { return codewordSize; }
    /** Returns optional synchronous trace callback.
     * @return Optional synchronous trace callback.
     */
    public Consumer<GXV44TraceArgs> getOnTrace() { return onTrace; }
    /** Registers a callback that may cancel by setting the event stop flag.
     * @param value callback, or null to disable tracing
     */
    public void setOnTrace(Consumer<GXV44TraceArgs> value) { onTrace = value; }

    /** Returns immutable detached dictionary snapshot.
     * @return Immutable detached dictionary snapshot.
     */
    public List<GXv44DecoderEntry> getStringCollection() {
        return Collections.unmodifiableList(new ArrayList<>(strings));
    }

    /** Releases diagnostic text, preserving dictionary coordinates and history. */
    public void clearDiagnostics() {
        for (int i = 0; i < strings.size(); ++i) {
            GXv44DecoderEntry entry = strings.get(i);
            strings.set(i, new GXv44DecoderEntry(entry.getCodeword(), entry.getPosition(), entry.getLength(), ""));
        }
    }

    /** Decodes a complete byte array, requiring terminal FLUSH and valid padding.
     * @param input non-null compressed bytes
     * @return Restored bytes
     * @throws IOException for malformed or incomplete compressed input
     * @throws CancellationException if tracing requests cancellation
     */
    public byte[] decompress(byte[] input) throws IOException {
        Objects.requireNonNull(input, "input");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        String bits = null;
        if (onTrace != null) {
            StringBuilder builder = new StringBuilder();
            for (byte b : input) for (int bit = 0; bit < 8; ++bit) builder.append((b >>> bit) & 1);
            bits = builder.toString();
        }
        decode(new ByteArrayInputStream(input), output, bits);
        return output.toByteArray();
    }

    /** Decodes transmission-order bits; spaces, CR, LF, dashes and braces are ignored.
     * The final partial byte is zero padded, as in the reference implementation.
     * @param bitstring non-null binary representation
     * @return Restored bytes
     * @throws IllegalArgumentException for invalid characters
     * @throws IOException for malformed or incomplete compressed input
     */
    public byte[] decompress(String bitstring) throws IOException {
        Objects.requireNonNull(bitstring, "bitstring");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BitWriter writer = new BitWriter(output, false);
        for (int i = 0; i < bitstring.length(); ++i) {
            char ch = bitstring.charAt(i);
            if (ch == '0' || ch == '1') writer.write(ch - '0', 1);
            else if (ch != ' ' && ch != '\r' && ch != '\n' && ch != '-' && ch != '{' && ch != '}') {
                throw new IllegalArgumentException(Messages.get("binary"));
            }
        }
        writer.align();
        return decompress(output.toByteArray());
    }

    /** Decodes with bounded history storage, leaving both streams open and unflushed.
     * Completed history blocks can be written before a later error is detected.
     * @param input readable compressed stream
     * @param output writable destination
     * @throws IOException for malformed data or a stream I/O failure
     * @throws CancellationException if tracing requests cancellation
     */
    public void decompress(InputStream input, OutputStream output) throws IOException {
        decode(input, output, null);
    }

    private void reset() {
        strings.clear();
        size = 0;
        lastValuePosition = 0;
        previous = Operation.NONE;
        previousCodeword = 0;
        previousOrdinal = 0;
        ordinalSize = options.getDefaultOrdinalSize();
        codewordSize = options.getDefaultCodewordSize();
    }

    private void append(int value) throws IOException {
        if (size >= options.getHistoryLimit()) throw new IOException(Messages.get("history"));
        if (size == history.length) {
            int capacity = (int)Math.min(options.getHistoryLimit(), Math.max((long)size + 1, (long)size * 2));
            history = Arrays.copyOf(history, capacity);
        }
        history[size++] = (byte)value;
    }

    private int at(int index) throws IOException {
        if (index < 0 || index >= size) throw new IOException(Messages.get("extension"));
        return history[index] & 255;
    }

    private void addString(int length, int lastPosition) {
        if (length > options.getMaximumStringLength().getValue() || strings.size() + 4 >= options.getMaxCodewords()) return;
        String value = DiagnosticValue.format(history, lastValuePosition, size - lastValuePosition, options.isHex());
        strings.add(new GXv44DecoderEntry(4 + strings.size(), lastPosition, length, value));
        lastValuePosition = size;
    }

    private int length(int codeword) { return strings.get(codeword - 4).getLength(); }

    private void ordinal(int value) throws IOException {
        append(value);
        if (previous == Operation.ORDINAL) addString(2, size - 1);
        else if (previous == Operation.CODEWORD) addString(length(previousCodeword) + 1, size - 1);
        previous = Operation.ORDINAL;
        previousOrdinal = value;
    }

    private void copy(GXv44DecoderEntry entry) throws IOException {
        for (int i = 0; i < entry.getLength(); ++i) append(at(entry.getStartPos() + i));
    }

    private void codeword(int code) throws IOException {
        int next = 4 + strings.size();
        if (code < 4 || code >= options.getMaxCodewords() || code > next
                || (code == next && previous != Operation.ORDINAL && previous != Operation.CODEWORD)
                || (code == next && previous == Operation.CODEWORD
                    && length(previousCodeword) >= options.getMaximumStringLength().getValue())) {
            throw new IOException(Messages.get("codeword", code));
        }
        int start = size;
        if (code < next) copy(strings.get(code - 4));
        else if (previous == Operation.ORDINAL) {
            append(previousOrdinal);
            append(previousOrdinal);
        } else {
            GXv44DecoderEntry entry = strings.get(previousCodeword - 4);
            copy(entry);
            append(at(entry.getStartPos()));
        }
        if (previous == Operation.ORDINAL) addString(2, start);
        else if (previous == Operation.CODEWORD) addString(length(previousCodeword) + 1, start);
        previous = Operation.CODEWORD;
        previousCodeword = code;
    }

    private void extension(int count) throws IOException {
        if (previous != Operation.CODEWORD || previousCodeword - 4 >= strings.size()
                || length(previousCodeword) + count > options.getMaximumStringLength().getValue()) {
            throw new IOException(Messages.get("extension"));
        }
        GXv44DecoderEntry entry = strings.get(previousCodeword - 4);
        int source = entry.getPosition() + 1;
        for (int i = 0; i < count; ++i) append(at(source + i));
        addString(length(previousCodeword) + count, size - 1);
        previous = Operation.STRING_EXTENSION;
    }

    private void trace(Operation operation, Object value, BitReader reader, String bits) {
        Consumer<GXV44TraceArgs> listener = onTrace;
        if (listener == null) return;
        GXV44TraceArgs args = new GXV44TraceArgs(false, operation, value, reader.getPosition(), bits,
                Arrays.copyOf(history, size), null, getStringCollection());
        listener.accept(args);
        if (args.isStop()) throw new CancellationException(Messages.get("cancelled"));
    }

    private void decode(InputStream input, OutputStream output, String bits) throws IOException {
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(output, "output");
        if ((Object)input == output) throw new IllegalArgumentException(Messages.get("streams"));
        reset();
        BitReader reader = new BitReader(input);
        trace(Operation.START, null, reader, bits);
        boolean afterCodeword = false;
        boolean endedWithFlush = false;
        int written = 0;
        try {
            while (reader.peek() >= 0) {
                endedWithFlush = false;
                int prefix = reader.read(1);
                if (afterCodeword && prefix == 0 && reader.read(1) == 1) {
                    extension(reader.extension(options.getMaximumStringLength()));
                    afterCodeword = false;
                    continue;
                }
                afterCodeword = false;
                if (prefix == 0) {
                    ordinal(reader.read(ordinalSize));
                    continue;
                }
                int code = reader.read(codewordSize);
                switch (code) {
                case 0:
                    throw new IOException(Messages.get("transparent"));
                case 1:
                    reader.align();
                    output.write(history, written, size - written);
                    written = size;
                    endedWithFlush = true;
                    trace(Operation.FLUSH, null, reader, bits);
                    break;
                case 2:
                    int next = reader.peek();
                    if (next < 0) throw new IOException(Messages.get("stepup"));
                    if (next == 0) {
                        if (ordinalSize == 8) throw new IOException(Messages.get("stepup"));
                        ++ordinalSize;
                        trace(Operation.STEP_UP_ORDINAL, null, reader, bits);
                    } else {
                        int maxBits = 32 - Integer.numberOfLeadingZeros(options.getMaxCodewords() - 1);
                        if (codewordSize >= Math.max(options.getDefaultCodewordSize(), maxBits)) {
                            throw new IOException(Messages.get("stepup"));
                        }
                        ++codewordSize;
                        trace(Operation.STEP_UP_CODEWORD, null, reader, bits);
                    }
                    break;
                case 3:
                    output.write(history, written, size - written);
                    written = 0;
                    reset();
                    trace(Operation.REINIT, null, reader, bits);
                    break;
                default:
                    codeword(code);
                    afterCodeword = true;
                    break;
                }
            }
            if (!endedWithFlush) throw new IOException(Messages.get("flush"));
        } catch (IOException error) {
            trace(Operation.ERROR, error.getMessage(), reader, bits);
            throw error;
        }
        trace(Operation.END, null, reader, bits);
    }
}
