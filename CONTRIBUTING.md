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

We want this project to be a welcoming and productive space for everyone. By participating, you are expected to:

- **Be respectful** — Treat all contributors and users with courtesy, regardless of experience level, background, or viewpoint.
- **Be honest** — Provide accurate information, acknowledge mistakes openly, and give constructive feedback in good faith.
- **Be collaborative** — Work together to resolve disagreements, assume good intent, and focus on what is best for the project.
- **Be inclusive** — Welcome newcomers, use inclusive language, and avoid dismissive or exclusionary behavior.

Unacceptable behavior — such as harassment, personal attacks, or deliberate misinformation — will not be tolerated.

---

## How to Contribute

### Reporting Bugs

If you find a bug, please open a [GitHub issue](https://github.com/FelixBaensch/MORTAR/issues) and include:

- A clear and descriptive title
- Steps to reproduce the problem
- Expected and actual behavior
- Your operating system and MORTAR version
- The MORTAR log file (within the application, there is a "Log file" button in the "About" view that opens the log file directory)
- Any relevant screenshots
<!-- To do: adjust this according to issue templates that will be implemented in the future -->

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

MORTAR is a Java+Gradle project. To set up a local development environment:

1. **Prerequisites:**
   - **Java Development Kit (JDK)**: MORTAR is currently built with Java version 21 as the set compiler source syntax level. 
     Higher versions of Java may work for building and running the project, but are not guaranteed to be compatible. The most 
     important factors for compatibility are the employed Gradle version (currently 8.14.3) and Kotlin-DSL Gradle plugin version 
     (currently 5.2.0, running Kotlin version 2.0.21 internally). Check the 
     [Gradle compatibility matrix](https://docs.gradle.org/current/userguide/compatibility.html) and the 
     [Kotlin release history](https://kotlinlang.org/docs/releases.html?#release-history) for details on which Java 
     versions are supported by which Gradle and Kotlin version.
     Based on this, we currently recommend using JDK versions 21 to 22 for development. Make sure you have one of 
     them installed, e.g. sourced from [Adoptium OpenJDK / Temurin](https://adoptium.net). It needs to be accessible on 
     your `PATH` or via the `$JAVA_HOME` environment variable. Otherwise, you can try passing the path to the specific 
     JDK installation to Gradle via the argument `-Dorg.gradle.java.home=/JDK_PATH` during the build (see step 3).
   
   - **Git**: Version control system to clone the repository and manage branches. Optionally, you can use a Git GUI client 
     (e.g., SourceTree) for easier management.
   
   - Optional: An Integrated Development Environment (IDE) with Gradle support (e.g., IntelliJ IDEA; also provides a Git GUI client).

2. **Clone the repository and switch to the `production` branch:**
   - a) If you just want to explore or build the project locally, run the following commands in your terminal:
     ```shell
      git clone https://github.com/FelixBaensch/MORTAR.git
      cd MORTAR
      git checkout production
     ```
     (Alternatively, do these steps within your Git GUI client)

   - b) If you want to contribute, fork the repository on GitHub first, then clone your fork and switch to the 
     `production` branch there:
     ```shell
      git clone https://github.com/YOUR_USERNAME/MORTAR.git
      cd MORTAR
      git checkout production
     ```
     (Alternatively, do these steps within your Git GUI client)

3. **Build the project**: a Gradle wrapper (`./gradlew`) of version 8.14.3 is included in this repository and should be 
   used to build the project. 
   Run the following command in your terminal:
   ```shell
    ./gradlew build
   ```
   (`build` optionally preceded by `clean`)
   
   Alternatively, to immediately build and run MORTAR from the command line, you can use:
   ```shell
    ./gradlew run
   ```
   (`run` optionally preceded by `clean`)
   
   To make the wrapper executable on Linux or macOS, you might first have to run:
    ```shell
     chmod +x gradlew
    ```
   Alternatively, open the project in your IDE as a Gradle project and run the `build.gradle.kts` build file. It might 
   be necessary to set your project JDK to version 21. or higher (see above) in your IDE settings.
   
   Gradle will resolve all dependencies automatically.
   
   Now, you are good to go, happy coding!

### Note on creating Distributions
   
   The Gradle distribution commands (e.g. `installDist` etc.) are configured to include 
   a specific Java Runtime Environment (JRE) which is supposed to be used in generated execution scripts.
   For this to work, you need to create an "AdoptOpenJDK\jdk-21.0.1_12_jre\" folder in the repository root directory and 
   put the JRE with the specified version into it (i.e. sub-folders of "AdoptOpenJDK\jdk-21.0.1_12_jre\" need to be 
   "bin", "conf", "legal", "lib", etc.).
   If you are only going to use our custom deployment Gradle tasks (e.g. `localWinDeploy` etc.), you do not need to download the JRE manually, the tasks
   will take care of it automatically.

---

## Branching Strategy

| Branch       | Purpose                                                                 |
|--------------|-------------------------------------------------------------------------|
| `main`       | Stable releases only — **do not target this branch with pull requests** |
| `production` | Active development branch — base your work here                         |

- Always branch off `production` for new features or bug fixes.
- When your work is complete, open a pull request back to `production`.
- Use descriptive branch names, e.g., `feature/add-new-fragmentation-method` or `fix/issue-123-null-pointer`.

---

## Coding Guidelines

- Follow the existing code style and formatting already present in the source files.
  - This includes, i.a., a consistent use of the ``this`` statement for class variables and methods.
  - Static variables and methods should be indicated by explicitly giving the class name in front of them.
  - Use explicit imports, no wildcard imports.
- Use meaningful, descriptive names for variables, methods, and classes.
  - Method parameter names should be prefixed with "a"-/"an"-.
  - Temporary method variable names should be prefixed with "tmp"-.
  - Apart from that, follow the general Java naming conventions (e.g., public static constants should have all-caps names).
- Keep methods short and focused on a single responsibility.
- Document public APIs with descriptive and informative Javadoc comments.
- All new class files should have the same license header (see [License-header/License-header.txt](License-header/License-header.txt)).
- When building, the [spotless plugin](https://github.com/diffplug/spotless) checks some code style aspects (see [build.gradle.kts](build.gradle.kts) for its configuration).
- We recommend using the [SonarQube for IDE plugin](https://www.sonarsource.com/products/sonarqube/ide/) to catch code issues before committing (since SonarQube will also check the code when you create a pull request).

---

## Commit Messages

Please write clear and concise commit messages. A suggested format based on [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>: <short summary>

<optional body explaining the motivation and details>
```

**Types:** `feat`, `fix`, `docs`, `refactor`, `test`, `chore`

**Examples:**
- `feat: add SMARTS-based fragmentation algorithm`
- `fix: resolve null pointer in molecule import`
- `docs: update installation instructions`

---

## Testing

- Unit tests are written using [JUnit 5](https://junit.org/junit5/) and located in `src/test/`.
- If you need to add test resources, add them to the respective folder within the same package path as the class that you are using them in.
- Run all tests with:
  ```shell
  ./gradlew test
  ```
  (`test` optionally preceded by `clean`)
- New features and bug fixes should be accompanied by appropriate test coverage.

---

## Contact

If you have questions about contributing, you can reach the maintainers via:

- [GitHub Issues](https://github.com/FelixBaensch/MORTAR/issues)
- [GitHub Discussions](https://github.com/FelixBaensch/MORTAR/discussions)
- [Email](mailto:jonas.schaub@uni-jena.de)
