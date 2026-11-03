# Football Tournament Management System

Stage 1 of a simple Java Servlet/JDBC college project.

## Technology

- Java 25 (latest LTS)
- Maven
- Jakarta Servlet API 6.0 (Tomcat 10.1)
- MySQL 8+
- JDBC and BCrypt

## Stage 1 setup

1. Create the database and sample data by running `database.sql` in MySQL Workbench or the MySQL client.
2. Update the database credentials in environment variables, or in `DBConnection.java` defaults for local development:
   - `FOOTBALL_DB_URL`
   - `FOOTBALL_DB_USERNAME`
   - `FOOTBALL_DB_PASSWORD`
3. Build the WAR file with `mvn clean package`.
4. Deploy `target/football-tournament-system.war` to Tomcat 10.1.

Sample credentials after running `database.sql`:

- Admin: `admin` / `password`
- Co-Admin: `coadmin` / `password`

The sample accounts are for local demonstration only. Change them before using the application outside a classroom environment.

## Available pages

Public pages are available without login:

- `/public/tournaments`, `/public/teams`, `/public/matches`
- `/public/live`, `/public/results`, `/public/standings`
- `/register-team` for submitting a team registration

After login, users are redirected by role:

- Admin: `/admin/dashboard`, `/admin/tournaments`, `/admin/teams`, `/admin/players`, `/admin/matches`, `/admin/standings`
- Co-Admin: `/coadmin/dashboard`, `/coadmin/team`, `/coadmin/players`, `/coadmin/matches`

All protected writes check the authenticated role on the server. Co-Admin requests use the `teamId` stored in the login session, so a browser cannot switch the operation to another team.

The application is intentionally server-rendered with simple Servlets and JDBC. `PortalServlet` contains the public, Admin and Co-Admin workflows so the code remains easy to demonstrate in a college project.
