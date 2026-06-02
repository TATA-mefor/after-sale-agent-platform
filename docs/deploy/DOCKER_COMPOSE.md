## Docker Compose Local Development

V3.2 adds an optional Docker Compose path for local app + MySQL startup. This is a local development setup only. It is
not a production deployment model and it does not change the default in-memory test path.
The first run may need to build the local app image and pull base/MySQL images, so startup can be affected by the local
Docker cache and network access.

Start app + MySQL:

```bash
docker compose up --build
```

The compose file starts:

- `mysql` on host port `3306`
- `app` on host port `8080`

The app service runs with:

```text
SPRING_PROFILES_ACTIVE=mysql
AFTERSALE_MYSQL_URL=jdbc:mysql://mysql:3306/after_sale_agent?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true
AFTERSALE_MYSQL_USERNAME=aftersale
AFTERSALE_MYSQL_PASSWORD=aftersale
```

These are local placeholder credentials. Override them from your shell or an uncommitted local `.env` file when needed.
Do not commit real passwords, API keys, tokens, or production configuration.

MySQL initialization uses the V3.1 scripts:

```text
src/main/resources/schema-mysql.sql
src/main/resources/data-mysql.sql
```

Check the running app:

```bash
curl http://localhost:8080/api/health
curl http://localhost:8080/actuator/health
```

Stop containers:

```bash
docker compose down
```

Stop containers and remove the local MySQL volume:

```bash
docker compose down -v
```

Default validation still does not require Docker:

```bash
mvn test
```

