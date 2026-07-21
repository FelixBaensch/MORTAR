# Contributing to MORTAR

Thank you for your interest in contributing to MORTAR! This document provides guidelines and instructions to help you get started.

## Table of Contents
* [Code of Conduct](#code-of-conduct)
* [How to Contribute](#how-to-contribute)
  * [Reporting Bugs](#reporting-bugs)
  * [Suggesting Enhancements](#suggesting-enhancements)
  * [Pull Requests](#pull-requests)
* [Development Setup](#development-setup)
* [Branching Strategy](#branching-strategy)
* [Coding Guidelines](#coding-guidelines)
* [Commit Messages](#commit-messages)
* [Testing](#testing)
* [Contact](#contact)

---

## Code of Conduct

<!-- TODO: Needed? -->
<!-- TODO: Describe expected behavior and community standards, or link to a CODE_OF_CONDUCT.md file. -->
<!-- Example: "This project follows the [Contributor Covenant](https://www.contributor-covenant.org/) Code of Conduct." -->

---

## How to Contribute

### Reporting Bugs

If you find a bug, please open a [GitHub issue](https://github.com/FelixBaensch/MORTAR/issues) and include:

- A clear and descriptive title
- Steps to reproduce the problem
- Expected and actual behavior
- Your operating system, Java version, and MORTAR version
- Any relevant screenshots or log output

### Suggesting Enhancements

Feature requests and enhancement suggestions are welcome. Please open a [GitHub issue](https://github.com/FelixBaensch/MORTAR/issues) or start a [GitHub Discussion](https://github.com/FelixBaensch/MORTAR/discussions) and describe:

- The motivation and use case for the enhancement
- A detailed description of the proposed behavior
- Any alternatives you have considered

### Pull Requests

1. Fork the repository and create your branch from `production` (see [Branching Strategy](#branching-strategy)).
2. Implement your changes following the [Coding Guidelines](#coding-guidelines).
3. Add or update tests where applicable (see [Testing](#testing)).
4. Ensure your changes build and all tests pass.
5. Open a pull request targeting the `production` branch with a clear description of your changes.

---

## Development Setup

MORTAR is a Gradle/Java/JavaFX project. To set up a local development environment:

1. **Prerequisites:**
   - Java Development Kit (JDK) version 21.0.1 or higher (e.g., [Adoptium OpenJDK](https://adoptium.net))
   - Gradle version 8.14.3 ([Gradle Build Tool](https://gradle.org))
   - An IDE with Gradle support (e.g., IntelliJ IDEA)

2. **Clone the repository:**
   ```shell
   git clone https://github.com/FelixBaensch/MORTAR.git
   ```

3. **Open the project** in your IDE as a Gradle project and run the `build.gradle.kts` build file. Gradle will resolve all dependencies automatically.

4. **Set your project JDK** to version 21.0.1 or higher in your IDE settings.

<!-- TODO: Add any additional setup steps specific to your development workflow (e.g., environment variables, IDE plugins, code style settings). -->
<!-- TODO: Add note about spotless and SonarLint plugin -->
---

## Branching Strategy

| Branch | Purpose |
|--------|---------|
| `main` | Stable releases only — **do not target this branch with pull requests** |
| `production` | Active development branch — base your work here |

- Always branch off `production` for new features or bug fixes.
- When your work is complete, open a pull request back to `production`.
- Use descriptive branch names, e.g., `feature/add-new-fragmentation-method` or `fix/issue-123-null-pointer`.

---

## Coding Guidelines

<!-- TODO: Fill in the specific coding conventions used in this project. Examples below: -->

- Follow the existing code style and formatting already present in the source files.
- Use meaningful, descriptive names for variables, methods, and classes.
- Keep methods short and focused on a single responsibility.
- Document public APIs with Javadoc comments.
- <!-- TODO: Add any project-specific style rules (e.g., indentation size, naming conventions, specific patterns to follow or avoid). -->
- <!-- TODO: Reference or link to any code style configuration files (e.g., `.editorconfig`, IDE style XML). -->

---

## Commit Messages

<!-- TODO: Describe the preferred commit message format. Example below: -->

Please write clear and concise commit messages. A suggested format:

```
<type>: <short summary>

<optional body explaining the motivation and details>
```

**Types:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

**Examples:**
- `feat: add SMARTS-based fragmentation algorithm`
- `fix: resolve null pointer in molecule import`
- `docs: update installation instructions`

<!-- TODO: Adjust the commit message format to match your team's conventions. -->

---

## Testing

<!-- TODO: Describe how tests are structured, how to run them, and what is expected from contributors. Example below: -->

- Unit tests are written using [JUnit 5](https://junit.org/junit5/) and located in `src/test/`.
- Run all tests with:
  ```shell
  ./gradlew test
  ```
- New features and bug fixes should be accompanied by appropriate test coverage.
- <!-- TODO: Add any additional testing requirements or instructions (e.g., integration tests, test data location). -->

---

## Contact

If you have questions about contributing, you can reach the maintainers via:

- [GitHub Issues](https://github.com/FelixBaensch/MORTAR/issues)
- [GitHub Discussions](https://github.com/FelixBaensch/MORTAR/discussions)
- [Email](mailto:jonas.schaub@uni-jena.de)
