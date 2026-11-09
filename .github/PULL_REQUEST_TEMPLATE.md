## Description

<!-- What does this PR change, and why? -->

## Related Issues

<!-- e.g. Closes #123 -->

## Checklist

- [ ] I have read [CONTRIBUTING.md](../CONTRIBUTING.md)
- [ ] This pull request targets the `production` branch
- [ ] Commit messages follow [Conventional Commits](https://www.conventionalcommits.org/) (`feat:`, `fix:`, `test:`, ...)
- [ ] `./gradlew spotlessApply` was run; new `.java` files carry the MIT license header
- [ ] `./gradlew build` passes locally (this includes `spotlessCheck` and the tests)
- [ ] New/changed user-facing text is resolved via `Message.get(key)` and added to the message bundle
- [ ] New or updated tests cover the change
- [ ] New fragmentation algorithms implement `IMoleculeFragmenter` and are registered in `FragmentationService`
