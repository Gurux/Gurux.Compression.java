# Gurux.Compression.java

[![Maven Central](https://img.shields.io/maven-central/v/org.gurux/gurux.compression?strategy=releaseProperty)](https://central.sonatype.com/artifact/org.gurux/gurux.compression)

Java implementation of the V.44 encoder and decoder. The library targets Java 8,
uses standard Java streams and has no runtime dependencies.

## Installation

The badge above shows the latest release available from Maven Central and links
to the artifact page. It updates automatically after releases are indexed.
It becomes available after the first successful Central publication.

Add this dependency to your Maven project's `pom.xml`, replacing `VERSION` with
the release shown on [Maven Central](https://central.sonatype.com/artifact/org.gurux/gurux.compression):

```xml
<dependency>
  <groupId>org.gurux</groupId>
  <artifactId>gurux.compression</artifactId>
  <version>VERSION</version>
</dependency>
```

Use a specific published version in your application. The badge tracks Central;
the example's `VERSION` is a placeholder, not an automatic Maven version selector.
Maven Central is available by default, so no additional repository is required.

Requires Java 8 or later to use the library.

## Compress and decompress

```java
import gurux.compression.GXV44Decoder;
import gurux.compression.GXV44Encoder;
import java.nio.charset.StandardCharsets;

byte[] original = "Hello, hello, hello!".getBytes(StandardCharsets.UTF_8);
byte[] compressed = new GXV44Encoder().compress(original);
byte[] restored = new GXV44Decoder().decompress(compressed);
String text = new String(restored, StandardCharsets.UTF_8);
```

The decoder methods throw `IOException` for malformed data. The string overload
of `compress` uses ASCII replacement, matching .NET; use an explicit charset
and the byte-array overload for Unicode text. The string overload of
`decompress` accepts bits in transmission order, ignoring spaces, CR, LF,
dashes and braces.

Both peers must use matching settings. The compressed stream does not carry
negotiation metadata.

## Compression options

```java
import gurux.compression.GXv44Options;
import gurux.compression.enums.MaximumStringLength;

GXv44Options options = new GXv44Options();
options.setHistoryLimit(3072);
options.setMaxCodewords(1024);
options.setMaximumStringLength(MaximumStringLength.VALUE_255);
GXV44Encoder encoder = new GXV44Encoder(options);
GXV44Decoder decoder = new GXV44Decoder(options);
```

Each codec validates and copies its options at construction, so subsequent
changes to `options` do not change that codec.

| Setting | Default | Accepted values |
| --- | --- | --- |
| `defaultCodewordSize` | 6 | 6–16 bits |
| `defaultOrdinalSize` | 7 | 7 or 8 bits |
| `maxCodewords` | 1024 | 256–65535, including reserved controls |
| `maximumStringLength` | `VALUE_255` | `VALUE_46`, `VALUE_78`, `VALUE_142`, `VALUE_255` |
| `historyLimit` | 3072 | At least 512 bytes |
| `hex` | false | Diagnostic formatting only |

## Files and streams

```java
import gurux.compression.GXV44Encoder;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

try (InputStream source = Files.newInputStream(Paths.get("input.bin"));
     OutputStream destination = Files.newOutputStream(Paths.get("input.v44"))) {
    new GXV44Encoder().compress(source, destination);
}
```

Use `decoder.decompress(source, destination)` to restore data. The codecs
leave streams open and do not call their `flush` methods. Compression buffers
at most `min(historyLimit, 65536)` source bytes; decoding retains at most the
negotiated history plus bounded dictionary entries. Use distinct underlying
source and destination files. On errors, the destination can contain partial
output. Every new operation resets codec state, including after failure.

Instances support sequential reuse; they are not thread safe or reentrant.
Callbacks must not recursively invoke the same instance. Transparent mode is
unsupported. Complete inputs must end in FLUSH with zero padding; empty input
compresses to the single byte `03`. There is no checksum or automatic detection
of settings mismatches in the V.44 payload.

## Diagnostics

```java
encoder.setOnTrace(event -> {
    System.out.println(event.getOperation());
    // event.setStop(true); // Cancels with CancellationException.
});
```

Trace callbacks execute synchronously. No trace bit strings are constructed
when tracing is disabled. Tracing allocates snapshots and can be expensive on
large streams. Encoder trace bits are in transmission order; decoder stream
events do not retain the entire compressed input and return null for those bits.

`getNodeTree()`, `getRootArray()` and `getStringCollection()` return immutable,
detached snapshots. Trace byte arrays are defensively copied. `clearDiagnostics()`
releases formatted values while retaining dictionary metadata; previously
returned snapshots remain valid. `encoder.reset()` clears its dictionary.

## .NET API mapping

| .NET | Java |
| --- | --- |
| `Gurux.Compression` | `gurux.compression` |
| `Compress` / `Decompress` | `compress` / `decompress` |
| Option properties | `get...()` / `set...()`; `isHex()` |
| `OrdinalSize` / `CodewordSize` | `getOrdinalSize()` / `getCodewordSize()` |
| `OnTrace` delegate | `setOnTrace(Consumer<GXV44TraceArgs>)` |
| `Stop` | `setStop(boolean)` |
| `NodeTree` / `StringCollection` | `getNodeTree()` / `getStringCollection()` |
| `GetRootArray()` | `getRootArray()`, unsigned byte keys represented as `Integer` |
| `ClearDiagnostics()` / `Reset()` | `clearDiagnostics()` / `reset()` |
| `MaximumStringLength.Value255` | `MaximumStringLength.VALUE_255` |
| `Operation.StepUpCodeword` | `Operation.STEP_UP_CODEWORD` |
| `InvalidDataException` | `IOException` |
| `OperationCanceledException` | `CancellationException` |

Java diagnostic entries are immutable instead of exposing mutable .NET DTOs.
Unsigned .NET numeric values are represented by Java `int`. The obsolete .NET
`MaxDictionarySize` and `EmitReinitControl` options are omitted: they have no
effect in the reference implementation. Dictionary capacity is configured with
`setMaxCodewords`; necessary REINIT controls are always emitted.

## License

GPL-2.0-only or a separate Gurux commercial license. See [LICENSE](LICENSE) and
[the commercial licensing notice](LICENSES/LicenseRef-Gurux-Commercial).
Both notices are included in the library JAR. Commercial terms are obtained
directly from [Gurux Ltd](https://www.gurux.fi/).
