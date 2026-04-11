---
name: deep-plan
description: >
  Multi-perspective planning for complex code changes.
  Trigger when user asks for a detailed plan, migration
  strategy, or architecture review.
---


## Deep Plan — Multi-Agent Analysis Skill

### Phase 1: Architecture Analysis
Analyze the requested change from a structural perspective.
Identify affected modules, data flows, and integration points.
Map dependencies between components.

### Phase 2: File Discovery
Locate ALL files that need modification — not just the obvious
targets. Include test files, type definitions, configuration
files, and downstream consumers.

### Phase 3: Risk Assessment
For each proposed change, identify:
- Edge cases that could cause runtime failures
- Breaking changes for existing consumers
- Race conditions or state management issues
- Security implications
- Rollback complexity

### Phase 4: Plan Synthesis & Review
Combine findings from all phases into a unified implementation
plan. Order steps by dependency chain. Flag any phase where
findings conflict. Include rollback procedures for each step.

### Output Format
Use numbered steps with sub-items. Include file paths.
Generate a Mermaid dependency diagram if more than 5 files
are affected. End with a risk summary table.
