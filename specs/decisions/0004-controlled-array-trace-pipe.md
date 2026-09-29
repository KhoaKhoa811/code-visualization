# ADR 0004: Separate pipe for the controlled array trace

Status: implemented and tested within the approved array-recording experiment, 2026-09-21.

## Decision and reason

The manual recorder writes canonical draft-1 event JSON lines to a private mode-0600 named pipe in container scratch. A separate Docker exec process reads it into a bounded trusted collector. This keeps trace data separate from console output without host mounts, external network, or changing the Docker execution-evidence channel. Incremental writes allow a valid prefix to survive cancellation or exceptions; normal-exit-only file collection would not establish that behavior.

The collector validates the experiment's precise wire encoding, original-source associations and logical prefix before accepting each event. General JSON/schema validation remains in the existing contract checker after result assembly. The collector is intentionally not a general JSON parser or arbitrary-program analyzer. Transport completion/limit markers are not user-visible events. Completion also requires successful runner and reader outcomes. Reader/writer blocking and cleanup use the existing run deadlines.

Retain at most 64 KiB, 32 events and 16 elements per array, with a two-event test override. Invalid/over-limit data triggers termination; never skip a bad record and resume. Preserve only the accepted prefix, with unavailable coverage when empty. Original and instrumented source hashes remain distinct.

## Boundaries and evidence

The recorder and pipe are accessible to the same container identity; this does not make them tamper-proof. The application still does not accept arbitrary submissions. Manual pairs deliberately defer automatic AST transformation so the experiment proves recording/transport/reconstruction independently. Complete coverage refers only to the three-operation region, not full method/frame lifetime.

Seven recording cases and four original/instrumented comparisons passed, with existing runner regressions and generated contract/reconstruction checks. The no-writer case proved bounded waiting for a pipe reader; it does not prove every engine-disconnection or process-crash path. Full admission, durable recovery, broader tracing and UI remain separately approved work.
