# ADR 0001: WebSocket for live console communication

Status: Accepted by the user on 2026-09-16. Not implemented or runtime-verified.

## Context

The Windows-local Java application needs incremental stdout/stderr, prompts without a newline, and interactive standard input for supported Scanner calls. Diagram playback starts after execution terminates. The confirmed frontend uses TypeScript, React, Vite, TanStack Query, Axios, and TanStack Router; the backend uses Java 21 and Spring Boot.

## Decision

Use the browser's native WebSocket API and Spring WebSocket handlers for run-scoped console output, input, EOF, and waiting/lifecycle notifications. Use HTTP through Axios and TanStack Query for run creation, cancellation, authoritative status retrieval, and completed results. Socket notifications may trigger cache reconciliation; the backend remains authoritative.

Keep the connection manager separate from editor components, playback state, and renderers. Browser communication terminates at the trusted backend, which mediates worker stdin/stdout. This decision does not give submitted Java network access or select worker trace transport.

Use direct WebSocket messages for this scope. A broker, STOMP, and SockJS are not needed for the approved console interaction. Message schemas and concrete endpoints remain future contract work.

## Reasons and Alternatives

A bidirectional connection fits the console's ongoing input/output exchange and is supported by both selected platforms. Server-sent events with separate HTTP input requests are viable, but would split console communication across two mechanisms. Polling is not selected for live console delivery; status retrieval remains an HTTP responsibility.

## Consequences and Required Follow-up

- Specify run/source binding, message versions, sequencing, input acknowledgement, and duplicate/retry handling. Accepted input is not proof of Java consumption.
- Define connection establishment before output can be lost, or bounded catch-up behavior. Define reconnect, disconnect, retention, EOF, and late-message policies before implementation.
- Bound message sizes, output queues, transcript memory, and processing rates. Browser WebSocket does not provide automatic receive backpressure. Define overflow outcomes without silently presenting incomplete output as complete.
- Serialize concurrent server sends and enforce send/buffer limits. Preserve documented stream ordering without inventing stdout/stderr ordering guarantees.
- Validate connection origin and run association. Connection state does not override trusted run status or permit input to another run.
- Verify prompts, input, cancellation while waiting, disconnection, excess output, and cleanup in the runner/frontend prototype. No such checks have run yet.

## References

Reviewed on 2026-09-16:

- [Spring WebSocket API](https://docs.spring.io/spring-framework/reference/web/websocket/server.html)
- [MDN WebSocket](https://developer.mozilla.org/en-US/docs/Web/API/WebSocket)
- [MDN EventSource](https://developer.mozilla.org/en-US/docs/Web/API/EventSource)
