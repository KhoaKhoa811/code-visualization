# ADR 0003: Confirm prototype process outcomes through Docker events

Status: Implemented within the user-approved error-classification fix, 2026-09-21. This is a controlled-fixture prototype decision, not a production transport commitment.

## Problem

A nonzero Docker CLI exit can describe either a Java process failure or a Docker/launch failure. Mapping every nonzero result to a compilation/runtime error incorrectly blames submitted code. Reserving numerical exit codes is insufficient because Java programs can return those same codes. Console strings and container-writable status files cannot establish trusted outcomes.

## Decision

Keep the existing Docker CLI execution and output collection. Before classifying a completed compiler/program command, query bounded Docker management-channel events for the exact container ID. Require one matching `exec_create` command record and one `exec_die` record with its exec ID. Require the engine exit code and CLI exit code to agree. Missing, invalid, ambiguous, or conflicting evidence produces an infrastructure error and an unconfirmed (null) process exit code. Existing committed cancellation, timeout, and output-limit causes remain authoritative.

The event query ends at a fixed timestamp, is bounded to three seconds within the existing stage/overall deadline, and retains at most 64 KiB of management output. Program stdout/stderr remain outside the evidence channel. Source is never rerun to recover missing evidence. Cleanup remains independently reported.

This avoids adding a Docker Engine API client or an in-container status writer for the small prototype. The Docker management endpoint is trusted; submitted code has no access to it.

## Limits and verification

Docker retains only the last 256 events. Busy-engine eviction, unavailable management transport, clock differences, or incompatible event shapes can make a completed program's outcome unconfirmable. Report that uncertainty rather than infer a source failure. Reevaluate direct Engine exec creation/start/inspection when implementing production orchestration. Event history is not durable ownership storage or restart recovery.

Seventeen simulated lifecycle cases and seven real controlled-container cases passed. Simulations cover missing/conflicting evidence and Docker failures; actual engine disconnection was not tested. Admission, orphan recovery, and other isolation gates remain unfinished.

References: [Docker events documentation](https://docs.docker.com/reference/cli/docker/system/events/) describes management events, bounded history, filtering and timestamps. [Moby exec implementation](https://github.com/moby/moby/blob/master/daemon/exec.go) distinguishes exec creation/start from process completion. The runtime suite validates the event shape used by the installed engine; upstream source alone is not compatibility proof.
