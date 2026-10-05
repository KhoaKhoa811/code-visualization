# ADR 0011: Build tracing by Java construct, not algorithm template

Status: proposed, 2026-10-04. The user requested differently written code be handled generally and approved a design review after PR #10 merged. Detailed implementation remains subject to confirmation.

Current probes validate exact statement sequences. They established execution, capture and replay correctness but would require accumulating recognizers for equivalent programs. The intended product needs reusable Java statement/expression support within explicit tracing coverage, independent of algorithm names.

Propose a recursive, typed analysis handoff and composed statement/expression lowering. Use structured source plans for validation; Java executes, and replay consumes recorded facts. Reuse source mapping, isolation, recorder transport and existing operation semantics. Keep legacy paths and tests while introducing the new capability path. Do not replace the parser or introduce a Java interpreter without evidence requiring it.

Supersedes ADR 0010's immediate bubble-sort-specific draft-5 implementation sequence, dedicated sorting facts/plan direction and sorting-only budget selection. Retain the bubble-sort source/checkpoints as proposed acceptance material and the need for fresh runtime identities. Draft-5, general scope-exit/grouping rules and larger budgets must be reconsidered against composable programs before implementation. No schema or limit changes result from this decision.

The [review and proposed increments](../composable-java-tracing.md) recommend first implementing analysis for variable/array declarations, assignments and a bounded int-expression family in arbitrary supported sequences. Then extend production/contracts, blocks/conditions, loops and method/frame/reference support with separate verification. Algorithms are acceptance cases; quicksort is not promised until its required capabilities exist.

Tradeoff: this requires replacing some prototype-specific recognizers and more demanding semantic composition tests. It avoids algorithm-by-algorithm dispatch and preserves useful boundaries rather than promising zero refactoring or arbitrary Java. An immediate full-Java rewrite and continued algorithm-template accumulation are both rejected as implementation directions.
