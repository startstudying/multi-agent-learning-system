# Quality Gates

## Gate 1: Spec-First

- [ ] PRD exists
- [ ] REQ exists
- [ ] SPEC exists
- [ ] PLAN exists
- [ ] TASK exists
- [ ] Context Pack exists

## Gate 2: Skill Selection

- [ ] Task type identified
- [ ] Relevant skills selected
- [ ] Missing skills addressed (GitHub reference or new skill)

## Gate 3: Subagent (if complex)

- [ ] Subagent decision documented
- [ ] Subagent report created (if used)
- [ ] Integration review completed (if used)

## Gate 4: Implementation

- [ ] Only Context Pack files modified
- [ ] Single TASK implemented
- [ ] No new dependencies without review
- [ ] Architecture drift check passed

## Gate 5: Testing

- [ ] Tests run per TEST_COMMANDS.md
- [ ] Failures documented with explanation
- [ ] Manual verification if automated tests unavailable

## Gate 6: Delivery

- [ ] Evidence document created
- [ ] Acceptance report created
- [ ] Changelog updated
- [ ] Project memory updated
- [ ] Retrospective completed (for features)
- [ ] New skills extracted (if applicable)

## Gate Failure Policy

If any gate fails:

1. Do not mark task as done.
2. Document failure in Evidence.
3. Create follow-up TASK to resolve.
4. Do not proceed to next TASK until resolved.
