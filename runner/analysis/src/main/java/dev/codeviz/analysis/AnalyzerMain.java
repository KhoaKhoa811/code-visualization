package dev.codeviz.analysis;

import java.nio.charset.StandardCharsets;

/** Bounded human-readable report, not a trace or API wire format. */
public final class AnalyzerMain {
    public static final int MAX_REPORT_BYTES = 65_536;
    public static byte[] report(ArrayAnalyzer.Result result) {
        StringBuilder text = new StringBuilder("analysis=array-probe-v1\n");
        text.append("sourceId=").append(result.source() == null ? "unavailable" : result.source().id()).append('\n');
        text.append("completeness=").append(result.completeness()).append('\n');
        text.append("eligible=").append(result.eligible()).append('\n');
        text.append("entry=").append(result.entry()).append('\n');
        for (var scope : result.scopes()) text.append("scope=").append(scope).append('\n');
        for (var binding : result.bindings()) text.append("binding=").append(binding).append('\n');
        for (var access : result.accesses()) text.append("access=").append(access).append('\n');
        text.append("sites=").append(result.sites()).append('\n');
        for (var diagnostic : result.diagnostics()) text.append("diagnostic=").append(diagnostic).append('\n');
        byte[] report = text.toString().getBytes(StandardCharsets.UTF_8);
        if (report.length > MAX_REPORT_BYTES) throw new IllegalStateException("REPORT_LIMIT");
        return report;
    }
    public static void main(String[] args) throws Exception {
        if (Runtime.version().feature() != 21) throw new IllegalStateException("Java 21 required");
        byte[] source = System.in.readNBytes(SourceSnapshot.MAX_BYTES + 1);
        ArrayAnalyzer.Result result = new ArrayAnalyzer().analyze(source);
        try { System.out.write(report(result)); }
        catch (IllegalStateException e) {
            System.out.print("analysis=array-probe-v1\neligible=false\ndiagnostic=LIMIT:REPORT_LIMIT\n");
            System.exit(2);
        }
        if (!result.eligible()) System.exit(1);
    }
}
