# Temple Digital Services Platform

# Cursor Module Implementation Prompt

This file defines the standard implementation workflow for every project module.

Detailed learning notes, tutorials, interview preparation, scenario-based questions,

command explanations, API explanations, hands-on learning records, and troubleshooting

learning material are maintained separately in the `temple-project-learning` repository.

Cursor should focus its Agent usage on implementation, testing, debugging, and verification.

---

# REQUIRED CONTEXT

Before starting a module, read:

- /docs/AI_[CONTEXT.md](http://CONTEXT.md)
- /docs/MODULE_[STATUS.md](http://STATUS.md)

Use `MODULE_STATUS.md` as the source of truth for:

- completed modules
- current module
- current module status
- project progress

Use `AI_CONTEXT.md` for:

- architecture
- project conventions
- security requirements
- technical decisions
- reliability requirements
- important implementation context

Inspect only repository files relevant to the approved module.

Do not scan the entire repository unless necessary.

---

# BEFORE IMPLEMENTATION

Before changing files:

1. Confirm the approved module.
2. Read `AI_CONTEXT.md`.
3. Read `MODULE_STATUS.md`.
4. Inspect only relevant existing implementation files.
5. Identify dependencies on completed modules.
6. Provide a SHORT implementation plan.
7. List expected files to create or modify.

Do NOT provide a long conceptual explanation.

Do NOT generate:

- tutorials
- detailed learning notes
- interview questions
- scenario-based learning questions
- large command explanations
- detailed API learning explanations
- textbook-style explanations

These are maintained separately in `temple-project-learning`.

---

# DURING IMPLEMENTATION

Follow these rules:

- Implement only the approved module.
- Do not implement future modules.
- Preserve completed module functionality.
- Follow existing architecture and conventions.
- Reuse existing code where appropriate.
- Avoid unnecessary refactoring.
- Avoid unnecessary dependencies.
- Avoid unnecessary repository scanning.
- Do not regenerate unchanged files.
- Keep implementation production-oriented but appropriately scoped.
- Optimize Cursor Agent/token usage.

Never hard-code:

- passwords
- database credentials
- API keys
- AWS credentials
- tokens
- private keys
- certificates
- production secrets

Use environment variables or appropriate secret-management patterns.

---

# TESTING

After implementation:

1. Build or compile where applicable.
2. Run relevant automated tests.
3. Test important module behavior.
4. Report the actual result.
5. Fix implementation-related failures where appropriate.
6. Re-run tests after fixes.
7. Perform concise manual verification where required.

Never:

- hide test failures
- disable important tests just to obtain a passing build
- suppress errors without understanding them
- mark a module complete when important validation is still failing

---

# TROUBLESHOOTING

When an implementation or test fails, follow:

ERROR

-> IDENTIFY FAILING LAYER

-> COLLECT EVIDENCE

-> FIND ROOT CAUSE

-> APPLY SMALLEST APPROPRIATE FIX

-> RETEST

-> VERIFY

Do not randomly modify configuration.

Do not reinstall tools unless evidence shows the installation is missing or broken.

Do not perform destructive actions without developer approval.

Troubleshooting remains an implementation responsibility in this repository.

However, detailed troubleshooting learning notes and scenario explanations belong in

the separate `temple-project-learning` repository.

---

# DOCUMENTATION

After completing the module, update:

- /docs/MODULE_[STATUS.md](http://STATUS.md)

Update:

- /docs/AI_[CONTEXT.md](http://CONTEXT.md)

only when the module changes:

- architecture
- important conventions
- major implementation decisions
- important operational context
- security requirements
- reliability requirements
- project-wide technical context

Documentation in this repository should remain concise and implementation-specific.

Do NOT duplicate detailed educational material.

Detailed learning notes, interview preparation, commands, API explanations,

hands-on walkthroughs, and troubleshooting learning material are maintained in

the separate `temple-project-learning` repository.

---

# REQUIRED COMPLETION RESPONSE

After implementing and validating the approved module, return only the following sections.

## 1. Files Created/Modified

List the files changed.

## 2. Implementation Summary

Briefly describe what was implemented.

## 3. Commands Executed

List important commands actually executed.

Examples may include:

- build commands
- test commands
- run commands
- database validation commands
- API validation commands

Do not provide long command tutorials.

## 4. Test Results

Clearly report:

- build result
- tests passed
- tests failed
- tests skipped where relevant

## 5. Problems Encountered

Briefly describe actual implementation problems and how they were resolved.

If none occurred, state:

`None.`

Do not convert this section into detailed learning material.

## 6. Manual Verification

Provide the minimum commands or steps required for developer verification.

Examples may include:

- application startup command
- API endpoint checks
- health checks
- database connectivity checks

## 7. Module Status

Use one of:

- COMPLETE
- TESTING
- BLOCKED

A module should be marked `COMPLETE` only when required implementation and validation succeed.

## 8. Suggested Git Commit Message

Provide one concise commit message.

---

# GIT SAFETY

Do not automatically:

- commit
- push
- merge
- force-push
- delete branches
- rewrite history

Wait for developer approval.

---

# INFRASTRUCTURE AND CLOUD SAFETY

Never automatically run destructive or cost-impacting operations such as:

- terraform apply
- terraform destroy
- destructive AWS commands
- destructive Kubernetes operations
- destructive database operations
- production deployment commands

unless explicitly approved by the developer.

Prefer local and low-cost resources until a module specifically requires cloud infrastructure.

---

# MODULE WORKFLOW

DESIGN

-> IMPLEMENT

-> TEST

-> TROUBLESHOOT

-> FIX

-> VERIFY

-> UPDATE PROJECT CONTEXT

-> COMMIT

Detailed learning and interview preparation are handled separately in

`temple-project-learning`.

---

# STOP RULE

After completing the approved module:

STOP.

Do not start the next module.

Wait for developer approval.