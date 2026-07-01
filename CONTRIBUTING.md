# Contributing to the Loan Origination System

Thank you for your interest in contributing! Even though this originated as a solo project, maintaining strict engineering disciplines ensures the codebase remains clean, readable, and scalable.

## 🌿 1. Branch Naming Convention

Always create a new branch for your work. Do not commit directly to `main` or `develop`. Use the following prefixes to categorize your branch:

*   `feature/` - For new features (e.g., `feature/kyc-upload-validation`)
*   `bugfix/` - For resolving issues (e.g., `bugfix/dti-ratio-calculation`)
*   `refactor/` - For code improvements without behavior changes (e.g., `refactor/extract-rule-engine-interface`)
*   `docs/` - For documentation updates (e.g., `docs/api-swagger-setup`)

**Example:** `git checkout -b feature/jwt-auth-implementation`

## 💬 2. Commit Message Convention

This project strictly follows the [Conventional Commits](https://www.conventionalcommits.org/) specification. This helps auto-generate changelogs and makes history easy to read.

**Format:**
```
<type>(<optional scope>): <description>

[optional body]
```

**Types:**
*   `feat:` A new feature
*   `fix:` A bug fix
*   `docs:` Documentation only changes
*   `style:` Formatting, missing semi-colons, etc.
*   `refactor:` A code change that neither fixes a bug nor adds a feature
*   `test:` Adding missing tests or correcting existing tests
*   `chore:` Updates to build tasks, package manager configs, etc.

**Examples:**
*   `feat(bre): add DebtToIncomeRule implementation`
*   `fix(auth): resolve HttpOnly cookie secure flag issue`
*   `test(api): add mockito unit tests for ApplicationService`

## ✅ 3. Pull Request (PR) Checklist

Before submitting a Pull Request, ensure you have completed the following:

- [ ] **Self-Review:** I have reviewed my own code.
- [ ] **Tests Passing:** I have run `mvn test` and `npm run test` locally, and all tests pass.
- [ ] **No Console Logs:** I have removed debugging `console.log` and `System.out.println` statements.
- [ ] **Commenting Rule:** I have commented *every non-trivial line* of code (especially business logic, DI, and DB queries) as if explaining it to a beginner, per project guidelines.
- [ ] **Documentation:** If this introduces a new API endpoint or configuration, I have updated `README.md` or Swagger annotations accordingly.

### PR Description Template
When opening a PR, please use this format in the description box:

```markdown
### What does this PR do?
(Briefly explain the feature or fix)

### Why is this necessary?
(Explain the architectural or business reasoning)

### How was it tested?
- [ ] Unit tested via JUnit / Jasmine
- [ ] Manual end-to-end test via UI
- [ ] Load tested via k6
```