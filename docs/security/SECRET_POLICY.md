# Secret Policy

## Never Commit

- API keys (OpenAI, Anthropic, etc.)
- Database passwords
- JWT signing secrets
- OAuth client secrets
- Private keys / certificates
- User PII or credentials

## Storage

- Development: `.env` files (gitignored)
- Production: environment variables or secret manager
- Never in `docs/memory/`, code comments, or test fixtures

## Detection

Before every commit, verify:

- No `.env` files staged
- No hardcoded keys in source
- No secrets in memory documents

## If Secret Is Committed

1. Rotate the secret immediately.
2. Remove from git history if possible.
3. Document incident in `docs/decisions/` ADR.
4. Update `.gitignore` if needed.

## AI Tool Access

- AI tools must not read `.env` files unless explicitly requested by user.
- Never paste secrets into prompts or memory files.
