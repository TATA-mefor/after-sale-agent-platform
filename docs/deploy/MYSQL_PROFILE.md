## MySQL Profile

V3.1 adds an explicit `mysql` profile for local persistence. The default profile remains in-memory, and default
`mvn test` does not connect to MySQL.

The MySQL profile persists:

- Ticket records
- AgentRun records
- ToolCallTrace records
- ApprovalRequest records
- Demo order data
- After-sale policy data

Schema and seed initialization are loaded from:

```text
src/main/resources/schema-mysql.sql
src/main/resources/data-mysql.sql
```

Configure MySQL with local environment variables. Do not commit real passwords.

PowerShell example:

```powershell
$env:SPRING_PROFILES_ACTIVE = "mysql"
$env:AFTERSALE_MYSQL_URL = "jdbc:mysql://localhost:3306/after_sale_agent?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true"
$env:AFTERSALE_MYSQL_USERNAME = "aftersale"
$env:AFTERSALE_MYSQL_PASSWORD = "<local-password>"
mvn spring-boot:run
```

Bash example:

```bash
SPRING_PROFILES_ACTIVE=mysql \
AFTERSALE_MYSQL_URL='jdbc:mysql://localhost:3306/after_sale_agent?useUnicode=true&characterEncoding=utf8&connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true' \
AFTERSALE_MYSQL_USERNAME=aftersale \
AFTERSALE_MYSQL_PASSWORD='<local-password>' \
mvn spring-boot:run
```

The application only creates a JDBC `DataSource` when `SPRING_PROFILES_ACTIVE=mysql` is set. Without that profile, the
in-memory repositories are active and no database connection is configured.

Manual local verification completed on 2026-05-18:

- Local MySQL version: 8.0.44.
- `schema-mysql.sql` imported successfully.
- `data-mysql.sql` imported successfully.
- `orders` seed count: 6.
- `aftersale_policies` seed count: 6.
- Application startup with the explicit `mysql` profile succeeded.
- Creating a Ticket, triggering an AgentRun, and querying the Execution Tree passed through local HTTP API verification.

This verification used local environment variables only. Do not commit real database passwords, local absolute paths,
API keys, tokens, or production configuration.

