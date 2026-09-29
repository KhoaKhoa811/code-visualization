package dev.codeviz.analysis;

import com.github.javaparser.Position;
import com.github.javaparser.Range;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

/** Original bytes and UTF-16 offsets. No formatting or newline normalization. */
public final class SourceSnapshot {
    public static final int MAX_BYTES = 65_536;
    public record Location(int line, int column) {}
    public record Span(int startOffset, int endOffset, Location start, Location end) {}
    private final String text;
    private final String id;
    private final int[] lines;

    public SourceSnapshot(byte[] bytes) throws CharacterCodingException {
        if (bytes.length > MAX_BYTES) throw new IllegalArgumentException("SOURCE_LIMIT");
        text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString();
        id = hash(bytes);
        List<Integer> starts = new ArrayList<>();
        starts.add(0);
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '\r') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '\n') i++;
                starts.add(i + 1);
            } else if (text.charAt(i) == '\n') starts.add(i + 1);
        }
        lines = starts.stream().mapToInt(Integer::intValue).toArray();
    }
    public String text() { return text; }
    public String id() { return id; }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException e) { throw new AssertionError(e); }
    }
    public void requireSame(byte[] current) {
        if (!id.equals(hash(current))) throw new IllegalArgumentException("STALE_SOURCE");
    }
    private int offset(Position p) {
        if (p.line < 1 || p.line > lines.length || p.column < 1) throw new IllegalArgumentException("Invalid parser position");
        int offset = lines[p.line - 1] + p.column - 1;
        int end = p.line < lines.length ? lines[p.line] : text.length();
        if (offset >= end) throw new IllegalArgumentException("Parser position outside source line");
        return offset;
    }
    private Location location(int offset) {
        int index = Arrays.binarySearch(lines, offset);
        if (index < 0) index = -index - 2;
        return new Location(index + 1, offset - lines[index] + 1);
    }
    public Span span(Range range) {
        int begin = offset(range.begin), end = offset(range.end) + 1;
        if (begin >= end) throw new IllegalArgumentException("Invalid parser range");
        return new Span(begin, end, location(begin), location(end));
    }
    public String slice(Span span) { return text.substring(span.startOffset(), span.endOffset()); }
}
