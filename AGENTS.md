# AGENTS.md

## Dev environment tips
- Run `mvn clean install -DskipTests` to install dependencies and compile without running tests.
- Use `mvn exec:exec@ktlint-format` to auto-format Kotlin sources.
- Keep Vert.x version aligned with the Control Plane; check the `pom.xml` parent section.
- Run the Agent locally with `mvn -DskipTests vertx:run` or by executing the built fat jar.
- Check the `README.md` in the Agent module for module-specific configuration examples.

## Testing instructions
- Find the CI plan in the `.github/workflows` folder.
- Run `mvn clean test` to execute all unit and integration tests.
- Use `mvn test -Dtest=<TestClassName>` to run a specific test.
- Add or update tests for the code you change, even if nobody asked.
- Fix any failing tests or ktlint issues before merging.
- Ensure `mvn exec:exec@ktlint-format` passes locally before committing.

## PR instructions
- Title format: [agent] <Title>
- Always run `mvn clean test` and `mvn exec:exec@ktlint-format` before committing.
- Verify your code builds on JDK 25 and matches the current Maven version used in CI.
