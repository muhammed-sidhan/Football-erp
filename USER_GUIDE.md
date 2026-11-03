# Football Tournament Management System – User Guide

## Application URL

When Tomcat is running, open:

```text
http://localhost:8080/football-tournament-system/
```

## Public viewer pages

Public viewers do not need an account or login.

| Page | URL |
|---|---|
| Home | `http://localhost:8080/football-tournament-system/` |
| Tournaments | `/public/tournaments` |
| Approved teams | `/public/teams` |
| All matches | `/public/matches` |
| Live matches | `/public/live` |
| Completed results | `/public/results` |
| Points table | `/public/standings` |
| Register a team | `/register-team` |

Append each path to the application URL. For example:

```text
http://localhost:8080/football-tournament-system/public/standings
```

Only approved teams and approved players are shown publicly. Only completed matches are included in the points table.

## Login page

```text
http://localhost:8080/football-tournament-system/login
```

## Admin login

Use the sample account created by [database.sql](database.sql):

```text
Username: admin
Password: password
Role: ADMIN
```

After login, the Admin dashboard is available at:

```text
http://localhost:8080/football-tournament-system/admin/dashboard
```

Admin pages:

| Feature | URL |
|---|---|
| Dashboard | `/admin/dashboard` |
| Tournament management | `/admin/tournaments` |
| Team approvals and Co-Admin assignment | `/admin/teams` |
| Player approvals | `/admin/players` |
| Match management and scores | `/admin/matches` |
| Points tables | `/admin/standings` |

Admin can create and edit tournaments, approve teams and players, assign Co-Admins, create matches, update scores, and publish results.

## Co-Admin login

Use the sample account created by [database.sql](database.sql):

```text
Username: coadmin
Password: password
Role: CO_ADMIN
Assigned team: Campus United
```

After login, the Co-Admin dashboard is available at:

```text
http://localhost:8080/football-tournament-system/coadmin/dashboard
```

Co-Admin pages:

| Feature | URL |
|---|---|
| Dashboard | `/coadmin/dashboard` |
| My team | `/coadmin/team` |
| My players | `/coadmin/players` |
| My matches | `/coadmin/matches` |

A Co-Admin can update only the assigned team and can register or edit only players belonging to that team. The server verifies the assigned `teamId` from the login session; changing form values in the browser cannot grant access to another team.

## Database setup

1. Start MySQL 8 or newer.
2. Run [database.sql](database.sql) in MySQL Workbench or the MySQL client.
3. The default connection is:

```text
Host: localhost
Port: 3306
Database: football_tournament_db
Username: root
Password: empty by default
```

For a different MySQL configuration, set these environment variables before starting Tomcat:

```text
FOOTBALL_DB_URL=jdbc:mysql://localhost:3306/football_tournament_db?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
FOOTBALL_DB_USERNAME=root
FOOTBALL_DB_PASSWORD=your-password
```

## Build and deploy

From the project directory:

```bash
mvn clean package
```

Deploy this file to Tomcat 10.1:

```text
target/football-tournament-system.war
```

For the bundled Tomcat installation in this project, the deployment location is:

```text
tools/apache-tomcat-10.1.61/webapps/football-tournament-system.war
```

## Restart the bundled Tomcat server on Windows

Run in PowerShell:

```powershell
$tomcat = "C:\Users\DELL\Desktop\java-project\tools\apache-tomcat-10.1.61"
$env:CATALINA_HOME = $tomcat
$env:CATALINA_BASE = $tomcat

& "$tomcat\bin\shutdown.bat"
Start-Sleep -Seconds 5
& "$tomcat\bin\startup.bat"
```

## Logout

```text
http://localhost:8080/football-tournament-system/logout
```

The sample passwords are for classroom demonstration only. Change them before using the application in a real environment.
