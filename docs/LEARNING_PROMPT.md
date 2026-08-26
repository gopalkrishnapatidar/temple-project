# Temple Digital Services Platform

# Reusable Learning Review Prompt

This prompt applies to every project module.

Read first:

- /docs/AI_[CONTEXT.md](http://CONTEXT.md)

- /docs/MODULE_[STATUS.md](http://STATUS.md)

Use the current module recorded in MODULE_[STATUS.md](http://STATUS.md).

My objective is to LEARN and gain hands-on experience, not simply generate code.

---

# BEFORE IMPLEMENTATION

Before changing anything:

1. Explain what problem this module solves.

2. Explain why this module is required in our Temple Digital Services Platform.

3. Explain the architecture involved.

4. Explain how the components communicate.

5. Explain important internal concepts.

6. Explain how this technology is normally used in production.

7. Explain security implications.

8. Explain scalability implications.

9. Explain high-availability implications where applicable.

10. Explain cost implications where applicable.

11. Explain common failure scenarios.

12. Explain how those failures would be troubleshot.

13. Give important interview concepts/questions for this module.

14. Inspect only repository files relevant to the current module.

15. Show a concise implementation plan.

16. List the files expected to be created or modified.

For significant changes, wait for my approval before implementation.

---

# DURING IMPLEMENTATION

Follow these rules:

- Implement only the current module.

- Do not implement future modules.

- Follow /docs/AI_[CONTEXT.md](http://CONTEXT.md).

- Reuse existing code where appropriate.

- Do not unnecessarily refactor working code.

- Do not scan unrelated repository files.

- Optimize Cursor token usage.

- Do not add unnecessary dependencies.

- Never hard-code credentials.

- Never expose secrets.

- Prefer production-oriented solutions.

- Keep the implementation understandable.

- Explain important decisions.

If an error occurs:

Do NOT randomly change things.

Follow:

ERROR

→ INVESTIGATE

→ ROOT CAUSE

→ FIX

→ RETEST

---

# AFTER IMPLEMENTATION

After implementation:

1. Explain what was implemented.

2. Explain every important file created or modified.

3. Explain important commands used.

4. Explain how I can manually verify the implementation.

5. Explain how to run the tests.

6. Give me 3-5 SAFE failure scenarios where applicable.

7. Explain how to reproduce those failures.

8. Explain how to troubleshoot them.

9. Explain expected root causes.

10. Explain security considerations.

11. Explain scalability and availability considerations.

12. Explain cost implications.

13. Give interview questions based on the ACTUAL implementation.

14. Give scenario-based interview questions.

15. Tell me what I should be able to explain without AI before considering

the module complete.

16. Update /docs/MODULE_[STATUS.md](http://STATUS.md).

17. Suggest a Git commit message.

Then STOP.

Do NOT automatically start the next module.

---

# LEARNING STANDARD

For every important technology I should eventually be able to answer:

WHAT is it?

WHY are we using it?

HOW does it work?

HOW did we implement it?

HOW do we test it?

HOW can it fail?

HOW do we troubleshoot it?

HOW do we secure it?

HOW do we scale it?

HOW do we make it highly available?

HOW do we monitor it?

HOW much can it cost?

WHAT alternatives exist?

WHAT trade-offs did we make?

HOW would I explain our implementation in a DevOps/SRE interview?