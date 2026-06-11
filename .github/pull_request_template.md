## Summary

<!-- What does this PR do? Link to TASK / SPEC. -->

- TASK: `docs/tasks/TASK-xxx.md`
- SPEC: `docs/specs/SPEC-xxx.md`

## Type of Change

- [ ] Feature
- [ ] Bug fix
- [ ] Refactor
- [ ] Docs / workflow
- [ ] Dependency update

## Checklist

### Workflow

- [ ] PRD / REQ / SPEC / PLAN / TASK exist (or N/A for docs-only)
- [ ] Context Pack respected — only allowed files modified
- [ ] Single TASK per PR (unless parallel worktree approved)

### Quality

- [ ] Tests run (`docs/harness/TEST_COMMANDS.md`)
- [ ] Architecture drift check passed
- [ ] No secrets committed
- [ ] New dependencies reviewed (`docs/security/`)

### Delivery

- [ ] Evidence document created or updated
- [ ] Acceptance checklist completed
- [ ] Changelog updated
- [ ] Project memory updated

## Test Plan

<!-- Commands run and results. -->

```bash
cd backend && mvn test
cd frontend && pnpm build
```

## Screenshots

<!-- If UI changes. -->

## Notes

<!-- Known limitations, follow-up tasks. -->
