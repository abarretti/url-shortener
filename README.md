# DB Setup
## Testcontainers JDBC
Use the following Testcontainers config for:

✔ integration tests

✔ interview demos

✔ not managing a persistent DB manually
```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:16:///urlshortener_db
    driver-class-name: org.testcontainers.jdbc.ContainerDatabaseDriver
 ```
What happens when your app starts:
1. Spring Boot asks for a DB connection
2. Testcontainers:
- Spins up a random Docker container
- On a random host port (NOT 5432)
- With random credentials
3. Injects those into the app in memory only
This DB exists only for your running JVM, not as a normal Postgres instance.
So IntelliJ has no idea it exists.

🧠 This DB is:
- Ephemeral
- Auto-created
- Auto-destroyed
- Internal to the test/app runtime

## Postgres container w/ IntelliJ DB Access
Uses a real DB Docker container. Use for:

✔ You can browse tables and run queries

✔ See Flyway migrations

✔ Perfect for local dev

Create a `docker-compose.yml` file with the DB container config as follows:
```yaml
version: "3.9"

services:
  postgres:
    image: postgres:16
    container_name: urlshortener-postgres
    restart: unless-stopped
    environment:
      POSTGRES_DB: urlshortener_db
      POSTGRES_USER: urlshortener
      POSTGRES_PASSWORD: urlshortener
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data

volumes:
  postgres_data:

```

Update `application-local.yml` as follows:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/urlshortener_db
    username: urlshortener
    password: urlshortener
    driver-class-name: org.postgresql.Driver
```
Start up DB container by either executing `docker compose up -d` from the terminal or hitting the play button in `docker-compose.yml`

# Testing
## Integration Tests
Use [WebTestClient](https://stackoverflow.com/questions/61318756/testresttemplate-vs-webtestclient-vs-restassured-what-is-the-best-approach-for) instead of TestRestTemplate. 

✅ Suggested explanation

"I chose WebTestClient bound to the real HTTP server because I wanted the integration tests to exercise the full stack: the actual servlet container, the controller layer, JSON serialization, filters, and database interactions with Testcontainers.

Using MockMvc would have tested only the controller layer in a simulated servlet environment — it’s fast and great for slice tests, but it doesn’t capture issues that can occur in a real HTTP request.

TestRestTemplate is fine, but WebTestClient provides a more fluent API for assertions, works well with modern Spring Boot versions, and can handle redirects and validation more easily. Even though the app is MVC and not reactive, WebTestClient.bindToServer() works perfectly for full-stack integration testing."

Key points to hit:
1. Full-stack realism — tests the app as it runs in production.

2. Fluent assertions — JSON paths, status codes, headers.

3. Modern Boot 4 practice — TestRestTemplate is legacy; MockMvc is for slices.

4. Works with Testcontainers + Flyway — ensures database migrations and isolation are tested.

💡 Tip: You can also mention that you understand the trade-offs:
- MockMvc: faster, but less realistic.
- TestRestTemplate: simpler, but less fluent API.
- WebTestClient: full realism, modern API, slightly slower but worth it for integration tests.

## Repository Tests
✅ Bottom line:
- Not bad practice to skip repository tests if integration tests cover everything.
- Standalone repository tests are now mostly useful for fast feedback loops or complex queries, not for simple save/find operations.

If your integration tests already cover the repository layer end-to-end, it’s perfectly reasonable to omit standalone repository tests.
Here’s the rationale:

1️⃣ What a repository test gives you
A typical @DataJpaTest for a repository checks things like:
- The mapping between the entity and table is correct.
- CRUD operations work (save, find, delete).
- Query methods (derived queries, custom @Query) behave as expected.

2️⃣ When it’s redundant
If your integration tests:
- Start a real Postgres container.
- Run Flyway migrations.
- Call your service/controller methods that internally call the repository.
- Assert that the expected data is stored/retrieved correctly.
- 
…then the repository layer is already being exercised with real DB operations. Standalone repository tests mostly duplicate this coverage.

3️⃣ When to still consider repo/unit tests
- You have complex derived queries or custom JPQL/native queries.
- You want fast, isolated tests for quick feedback without spinning up a container.
- You want early failure if entity mapping changes, without running full service/integration tests.

If none of these apply, you’re fine skipping them.

4️⃣ Best practice approach
Many teams now follow this pattern:

Test type	DB	Purpose
Repository/unit tests (@DataJpaTest)	Optional, lightweight H2 or TestContainers	Only for complex queries or fast feedback. Not always necessary.
Integration tests (@SpringBootTest)	Postgres + Flyway	Full coverage including repository, service, controller, and DB interactions.
