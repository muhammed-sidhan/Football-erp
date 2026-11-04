package com.football.web;

import com.football.config.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;

/**
 * Small, server-rendered application controller. Keeping the workflows in one
 * servlet makes this college project easy to follow while still enforcing all
 * permissions on the server.
 */
@WebServlet({"/public/*", "/admin/*", "/coadmin/*", "/register-team"})
public class PortalServlet extends HttpServlet {
    private static final String ADMIN = "ADMIN";
    private static final String CO_ADMIN = "CO_ADMIN";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String path = path(request);
        try (Connection connection = DBConnection.getConnection()) {
            if (path.equals("/register-team")) {
                renderTeamRegistration(request, response, connection, null);
            } else if (path.startsWith("/public/")) {
                publicPage(request, response, connection, path.substring("/public/".length()));
            } else if (path.startsWith("/admin/")) {
                if (!requireRole(request, response, ADMIN)) return;
                adminPage(request, response, connection, path.substring("/admin/".length()), null);
            } else if (path.startsWith("/coadmin/")) {
                if (!requireRole(request, response, CO_ADMIN)) return;
                coAdminPage(request, response, connection, path.substring("/coadmin/".length()), null);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (SQLException exception) {
            getServletContext().log("Database operation failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The database is unavailable. Check MySQL and try again.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        String path = path(request);
        try (Connection connection = DBConnection.getConnection()) {
            if (path.equals("/register-team")) {
                registerTeam(request, response, connection);
            } else if (path.startsWith("/admin/")) {
                if (!requireRole(request, response, ADMIN)) return;
                adminPost(request, response, connection, path.substring("/admin/".length()));
            } else if (path.startsWith("/coadmin/")) {
                if (!requireRole(request, response, CO_ADMIN)) return;
                coAdminPost(request, response, connection, path.substring("/coadmin/".length()));
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (IllegalArgumentException exception) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, exception.getMessage());
        } catch (SQLException exception) {
            getServletContext().log("Database operation failed", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
                    "The database operation could not be completed.");
        }
    }

    private String path(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        String pathInfo = request.getPathInfo();
        return "/register-team".equals(servletPath) ? servletPath
                : (pathInfo == null ? servletPath : servletPath + pathInfo);
    }

    private boolean requireRole(HttpServletRequest request, HttpServletResponse response, String role)
            throws IOException {
        HttpSession session = request.getSession(false);
        if (session == null || !role.equals(session.getAttribute("role"))) {
            response.sendRedirect(request.getContextPath() + "/login");
            return false;
        }
        return true;
    }

    private void publicPage(HttpServletRequest request, HttpServletResponse response,
                            Connection connection, String page) throws IOException, SQLException {
        String title = switch (page) {
            case "tournaments" -> "Tournaments";
            case "teams" -> "Teams";
            case "matches" -> "Matches";
            case "live" -> "Live Matches";
            case "results" -> "Results";
            case "standings" -> "Points Tables";
            default -> null;
        };
        if (title == null) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        begin(response, title, "public");
        if (page.equals("tournaments")) {
            heading(response.getWriter(), "Tournaments", "Browse all published tournament information.");
            table(response.getWriter(), connection,
                    "SELECT tournament_id,tournament_name,description,start_date,end_date,location,status " +
                            "FROM tournaments ORDER BY start_date",
                    new String[]{"ID", "Tournament", "Description", "Start", "End", "Location", "Status"},
                    new String[]{"tournament_id", "tournament_name", "description", "start_date", "end_date", "location", "status"});
        } else if (page.equals("teams")) {
            heading(response.getWriter(), "Approved Teams", "Only approved teams are visible to public viewers.");
            table(response.getWriter(), connection,
                    "SELECT team_id,team_name,coach_name,description FROM teams WHERE registration_status='APPROVED' ORDER BY team_name",
                    new String[]{"ID", "Team", "Coach", "About"},
                    new String[]{"team_id", "team_name", "coach_name", "description"});
        } else if (page.equals("standings")) {
            heading(response.getWriter(), "Points Tables", "Each tournament has its own table. Completed matches only: win 3, draw 1.");
            renderStandings(response.getWriter(), connection);
        } else {
            String filter = page.equals("live") ? " WHERE m.status='LIVE'" :
                    page.equals("results") ? " WHERE m.status='COMPLETED'" : "";
            heading(response.getWriter(), title, "Match schedules and published results.");
            table(response.getWriter(), connection,
                    "SELECT m.match_id,t.tournament_name,ht.team_name home_team,at.team_name away_team," +
                            "m.match_date,m.match_time,m.venue,m.home_score,m.away_score,m.status " +
                            "FROM matches m JOIN tournaments t ON t.tournament_id=m.tournament_id " +
                            "JOIN teams ht ON ht.team_id=m.home_team_id AND ht.registration_status='APPROVED' " +
                            "JOIN teams at ON at.team_id=m.away_team_id AND at.registration_status='APPROVED'" +
                            filter + " ORDER BY m.match_date,m.match_time",
                    new String[]{"ID", "Tournament", "Home", "Away", "Date", "Time", "Venue", "Score", "Status"},
                    new String[]{"match_id", "tournament_name", "home_team", "away_team", "match_date", "match_time", "venue", "score", "status"});
        }
        end(response.getWriter());
    }

    private void adminPage(HttpServletRequest request, HttpServletResponse response,
                           Connection connection, String page, String message) throws IOException, SQLException {
        begin(response, "Admin - " + page, "admin");
        PrintWriter out = response.getWriter();
        if (page.equals("dashboard")) {
            heading(out, "Admin Dashboard", "Manage the tournament from one place.");
            out.println("<div class='cards'>");
            metric(out, "Tournaments", scalar(connection, "SELECT COUNT(*) FROM tournaments"));
            metric(out, "Teams", scalar(connection, "SELECT COUNT(*) FROM teams"));
            metric(out, "Players", scalar(connection, "SELECT COUNT(*) FROM players"));
            metric(out, "Matches", scalar(connection, "SELECT COUNT(*) FROM matches"));
            metric(out, "Upcoming", scalar(connection, "SELECT COUNT(*) FROM matches WHERE status='UPCOMING'"));
            metric(out, "Live", scalar(connection, "SELECT COUNT(*) FROM matches WHERE status='LIVE'"));
            out.println("</div>");
        } else if (page.equals("tournaments")) {
            heading(out, "Tournament Management", "Create, update and remove tournaments.");
            tournamentForm(out, null);
            table(out, connection, "SELECT tournament_id,tournament_name,description,start_date,end_date,location,status FROM tournaments ORDER BY start_date",
                    new String[]{"ID", "Name", "Description", "Start", "End", "Location", "Status"},
                    new String[]{"tournament_id", "tournament_name", "description", "start_date", "end_date", "location", "status"});
            out.println("<form method='post'><input name='tournamentId' type='number' required placeholder='Tournament ID'><button name='action' value='delete'>Delete tournament</button></form>");
        } else if (page.equals("teams")) {
            heading(out, "Team Approvals and Assignment", "Approve teams and optionally assign a Co-Admin.");
            table(out, connection, "SELECT team_id,team_name,coach_name,registration_status FROM teams ORDER BY team_name",
                    new String[]{"ID", "Team", "Coach", "Status"}, new String[]{"team_id", "team_name", "coach_name", "registration_status"});
            out.println("<h2>Team action</h2><form method='post'><input name='teamId' type='number' required placeholder='Team ID'>"
                    + "<select name='status'><option>APPROVED</option><option>REJECTED</option><option>PENDING</option></select>"
                    + "<input name='username' placeholder='Co-Admin username (optional)'><button name='action' value='team'>Save team</button></form>");
        } else if (page.equals("players")) {
            heading(out, "Player Approvals", "Approve or reject registered players.");
            table(out, connection, "SELECT p.player_id,p.player_name,t.team_name,p.age,p.jersey_number,p.position,p.registration_status FROM players p JOIN teams t ON t.team_id=p.team_id ORDER BY t.team_name,p.player_name",
                    new String[]{"ID", "Player", "Team", "Age", "Jersey", "Position", "Status"},
                    new String[]{"player_id", "player_name", "team_name", "age", "jersey_number", "position", "registration_status"});
            out.println("<h2>Player action</h2><form method='post'><input name='playerId' type='number' required placeholder='Player ID'>"
                    + "<select name='status'><option>APPROVED</option><option>REJECTED</option><option>PENDING</option></select>"
                    + "<button name='action' value='player'>Save player</button></form>");
        } else if (page.equals("matches")) {
            heading(out, "Match Management", "Create fixtures and publish scores.");
            matchForm(out, connection);
            table(out, connection, "SELECT m.match_id,t.tournament_name,ht.team_name home_team,at.team_name away_team,m.match_date,m.match_time,m.venue,m.home_score,m.away_score,m.status FROM matches m JOIN tournaments t ON t.tournament_id=m.tournament_id JOIN teams ht ON ht.team_id=m.home_team_id JOIN teams at ON at.team_id=m.away_team_id ORDER BY m.match_date,m.match_time",
                    new String[]{"ID", "Tournament", "Home", "Away", "Date", "Time", "Venue", "Score", "Status"},
                    new String[]{"match_id", "tournament_name", "home_team", "away_team", "match_date", "match_time", "venue", "score", "status"});
        } else if (page.equals("standings")) {
            heading(out, "Points Tables", "Completed matches only. Win 3, draw 1, loss 0.");
            renderStandings(out, connection);
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        end(out);
    }

    private void coAdminPage(HttpServletRequest request, HttpServletResponse response,
                             Connection connection, String page, String message) throws IOException, SQLException {
        int teamId = sessionTeamId(request);
        if (teamId <= 0) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "No team is assigned to this Co-Admin.");
            return;
        }
        begin(response, "Co-Admin - " + page, "coadmin");
        PrintWriter out = response.getWriter();
        if (page.equals("dashboard") || page.equals("team")) {
            heading(out, "My Team", "You can only manage the team assigned to your account.");
            table(out, connection, "SELECT team_id,team_name,coach_name,description,registration_status FROM teams WHERE team_id=" + teamId,
                    new String[]{"ID", "Team", "Coach", "Description", "Status"}, new String[]{"team_id", "team_name", "coach_name", "description", "registration_status"});
            out.println("<h2>Update team</h2><form method='post'><input name='teamName' required placeholder='Team name'><input name='coachName' placeholder='Coach name'><textarea name='description' placeholder='Description'></textarea><button name='action' value='team'>Save team</button></form>");
        } else if (page.equals("players")) {
            heading(out, "My Players", "New players remain pending until an Admin approves them.");
            playerForm(out);
            table(out, connection, "SELECT player_id,player_name,age,jersey_number,position,registration_status FROM players WHERE team_id=" + teamId + " ORDER BY player_name",
                    new String[]{"ID", "Player", "Age", "Jersey", "Position", "Status"}, new String[]{"player_id", "player_name", "age", "jersey_number", "position", "registration_status"});
        } else if (page.equals("matches")) {
            heading(out, "My Matches", "Upcoming and completed matches involving your team.");
            table(out, connection, "SELECT m.match_id,t.tournament_name,ht.team_name home_team,at.team_name away_team,m.match_date,m.match_time,m.venue,m.home_score,m.away_score,m.status FROM matches m JOIN tournaments t ON t.tournament_id=m.tournament_id JOIN teams ht ON ht.team_id=m.home_team_id JOIN teams at ON at.team_id=m.away_team_id WHERE m.home_team_id=" + teamId + " OR m.away_team_id=" + teamId + " ORDER BY m.match_date,m.match_time",
                    new String[]{"ID", "Tournament", "Home", "Away", "Date", "Time", "Venue", "Score", "Status"}, new String[]{"match_id", "tournament_name", "home_team", "away_team", "match_date", "match_time", "venue", "score", "status"});
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        end(out);
    }

    private void adminPost(HttpServletRequest request, HttpServletResponse response,
                           Connection connection, String page) throws IOException, SQLException {
        String action = value(request, "action");
        if (page.equals("tournaments")) {
            validateDates(request.getParameter("startDate"), request.getParameter("endDate"));
            if (action.equals("delete")) {
                execute(connection, "DELETE FROM tournaments WHERE tournament_id=?", intValue(request, "tournamentId"));
            } else {
                String sql = request.getParameter("tournamentId") == null || request.getParameter("tournamentId").isBlank()
                        ? "INSERT INTO tournaments(tournament_name,description,start_date,end_date,location,status) VALUES(?,?,?,?,?,?)"
                        : "UPDATE tournaments SET tournament_name=?,description=?,start_date=?,end_date=?,location=?,status=? WHERE tournament_id=?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, required(request, "name"));
                    statement.setString(2, request.getParameter("description"));
                    statement.setDate(3, Date.valueOf(required(request, "startDate")));
                    statement.setDate(4, Date.valueOf(required(request, "endDate")));
                    statement.setString(5, required(request, "location"));
                    statement.setString(6, required(request, "status"));
                    if (sql.contains("WHERE")) statement.setInt(7, intValue(request, "tournamentId"));
                    statement.executeUpdate();
                }
            }
            redirect(response, request, "/admin/tournaments");
        } else if (page.equals("teams")) {
            int teamId = intValue(request, "teamId");
            execute(connection, "UPDATE teams SET registration_status=? WHERE team_id=?", value(request, "status"), teamId);
            String username = request.getParameter("username");
            if (username != null && !username.isBlank()) {
                execute(connection, "UPDATE users SET team_id=? WHERE username=? AND role='CO_ADMIN'", teamId, username.trim());
            }
            redirect(response, request, "/admin/teams");
        } else if (page.equals("players")) {
            execute(connection, "UPDATE players SET registration_status=? WHERE player_id=?", value(request, "status"), intValue(request, "playerId"));
            redirect(response, request, "/admin/players");
        } else if (page.equals("matches")) {
            saveMatch(request, connection);
            redirect(response, request, "/admin/matches");
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void coAdminPost(HttpServletRequest request, HttpServletResponse response,
                             Connection connection, String page) throws IOException, SQLException {
        int teamId = sessionTeamId(request);
        if (page.equals("team")) {
            execute(connection, "UPDATE teams SET team_name=?,coach_name=?,description=? WHERE team_id=?",
                    required(request, "teamName"), request.getParameter("coachName"), request.getParameter("description"), teamId);
            redirect(response, request, "/coadmin/team");
        } else if (page.equals("players")) {
            String id = request.getParameter("playerId");
            if (id == null || id.isBlank()) {
                try (PreparedStatement statement = connection.prepareStatement("INSERT INTO players(team_id,player_name,age,jersey_number,position,registration_status) VALUES(?,?,?,?,?,'PENDING')")) {
                    statement.setInt(1, teamId);
                    statement.setString(2, required(request, "playerName"));
                    statement.setInt(3, intValue(request, "age"));
                    statement.setInt(4, intValue(request, "jerseyNumber"));
                    statement.setString(5, required(request, "position"));
                    statement.executeUpdate();
                }
            } else {
                execute(connection, "UPDATE players SET player_name=?,age=?,jersey_number=?,position=? WHERE player_id=? AND team_id=?",
                        required(request, "playerName"), intValue(request, "age"), intValue(request, "jerseyNumber"), required(request, "position"), Integer.parseInt(id), teamId);
            }
            redirect(response, request, "/coadmin/players");
        } else {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private void registerTeam(HttpServletRequest request, HttpServletResponse response, Connection connection) throws IOException, SQLException {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO teams(tournament_id,team_name,coach_name,description,registration_status) VALUES(?,?,?,?, 'PENDING')")) {
            statement.setInt(1, intValue(request, "tournamentId"));
            statement.setString(2, required(request, "teamName"));
            statement.setString(3, request.getParameter("coachName"));
            statement.setString(4, request.getParameter("description"));
            statement.executeUpdate();
        }
        redirect(response, request, "/");
    }

    private void saveMatch(HttpServletRequest request, Connection connection) throws SQLException {
        int tournamentId = intValue(request, "tournamentId");
        int home = intValue(request, "homeTeamId");
        int away = intValue(request, "awayTeamId");
        if (home == away) throw new IllegalArgumentException("A team cannot play against itself.");
        if (scalar(connection, "SELECT COUNT(*) FROM teams WHERE team_id IN (" + home + "," + away + ") AND tournament_id=" + tournamentId) != 2) {
            throw new IllegalArgumentException("Both teams must belong to the selected tournament.");
        }
        String id = request.getParameter("matchId");
        String sql = id == null || id.isBlank()
                ? "INSERT INTO matches(tournament_id,home_team_id,away_team_id,match_date,match_time,venue,home_score,away_score,status) VALUES(?,?,?,?,?,?,?,?,?)"
                : "UPDATE matches SET tournament_id=?,home_team_id=?,away_team_id=?,match_date=?,match_time=?,venue=?,home_score=?,away_score=?,status=? WHERE match_id=?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, tournamentId); statement.setInt(2, home); statement.setInt(3, away);
            statement.setDate(4, Date.valueOf(required(request, "matchDate")));
            statement.setTime(5, Time.valueOf(required(request, "matchTime") + ":00"));
            statement.setString(6, required(request, "venue"));
            statement.setInt(7, intValue(request, "homeScore")); statement.setInt(8, intValue(request, "awayScore"));
            statement.setString(9, required(request, "matchStatus"));
            if (id != null && !id.isBlank()) statement.setInt(10, Integer.parseInt(id));
            statement.executeUpdate();
        }
    }

    private int sessionTeamId(HttpServletRequest request) {
        Object value = request.getSession(false).getAttribute("teamId");
        return value instanceof Number number ? number.intValue() : 0;
    }

    private void renderTeamRegistration(HttpServletRequest request, HttpServletResponse response, Connection connection, String ignored) throws IOException, SQLException {
        begin(response, "Register a Team", "public");
        PrintWriter out = response.getWriter();
        heading(out, "Register a Team", "Submit a team for Admin approval.");
        out.println("<form method='post'><select name='tournamentId' required><option value=''>Choose tournament</option>");
        try (PreparedStatement statement = connection.prepareStatement("SELECT tournament_id,tournament_name FROM tournaments WHERE status<>'COMPLETED' ORDER BY start_date"); ResultSet result = statement.executeQuery()) {
            while (result.next()) out.println("<option value='" + result.getInt(1) + "'>" + Html.escape(result.getString(2)) + "</option>");
        }
        out.println("</select><input name='teamName' required placeholder='Team name'><input name='coachName' placeholder='Coach name'><textarea name='description' placeholder='Team description'></textarea><button>Submit registration</button></form>");
        end(out);
    }

    private void tournamentForm(PrintWriter out, Object ignored) {
        out.println("<form method='post'><input name='tournamentId' type='number' placeholder='ID to edit (blank creates)'><input name='name' required placeholder='Tournament name'><input name='startDate' type='date' required><input name='endDate' type='date' required><input name='location' required placeholder='Location'><select name='status'><option>UPCOMING</option><option>ONGOING</option><option>COMPLETED</option></select><textarea name='description' placeholder='Description'></textarea><button>Save tournament</button></form>");
    }

    private void playerForm(PrintWriter out) {
        out.println("<form method='post'><input name='playerName' required placeholder='Player name'><input name='age' type='number' min='5' max='70' required placeholder='Age'><input name='jerseyNumber' type='number' min='0' max='99' required placeholder='Jersey number'><select name='position'><option>Goalkeeper</option><option>Defender</option><option>Midfielder</option><option>Forward</option></select><button>Register player</button></form>");
    }

    private void matchForm(PrintWriter out, Connection connection) throws SQLException {
        out.println("<form method='post'><input name='matchId' type='number' placeholder='ID to edit (blank creates)'><select name='tournamentId' required><option value=''>Tournament</option>");
        options(out, connection, "SELECT tournament_id,tournament_name FROM tournaments ORDER BY start_date");
        out.println("</select><select name='homeTeamId' required><option value=''>Home team</option>"); options(out, connection, "SELECT team_id,team_name FROM teams WHERE registration_status='APPROVED' ORDER BY team_name");
        out.println("</select><select name='awayTeamId' required><option value=''>Away team</option>"); options(out, connection, "SELECT team_id,team_name FROM teams WHERE registration_status='APPROVED' ORDER BY team_name");
        out.println("</select><input name='matchDate' type='date' required><input name='matchTime' type='time' required><input name='venue' required placeholder='Venue'><input name='homeScore' type='number' min='0' value='0'><input name='awayScore' type='number' min='0' value='0'><select name='matchStatus'><option>UPCOMING</option><option>LIVE</option><option>COMPLETED</option></select><button>Save match</button></form>");
    }

    private void options(PrintWriter out, Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) out.println("<option value='" + result.getInt(1) + "'>" + Html.escape(result.getString(2)) + "</option>");
        }
    }

    private void renderStandings(PrintWriter out, Connection connection) throws SQLException {
        String tournamentsSql = "SELECT tournament_id,tournament_name,status FROM tournaments ORDER BY start_date,tournament_id";
        String standingsSql = "SELECT t.team_name, COUNT(m.match_id) played, "
                + "COALESCE(SUM(CASE WHEN (m.home_team_id=t.team_id AND m.home_score>m.away_score) "
                + "OR (m.away_team_id=t.team_id AND m.away_score>m.home_score) THEN 1 ELSE 0 END),0) won, "
                + "COALESCE(SUM(CASE WHEN m.home_score=m.away_score THEN 1 ELSE 0 END),0) drawn, "
                + "COALESCE(SUM(CASE WHEN (m.home_team_id=t.team_id AND m.home_score<m.away_score) "
                + "OR (m.away_team_id=t.team_id AND m.away_score<m.home_score) THEN 1 ELSE 0 END),0) lost, "
                + "COALESCE(SUM(CASE WHEN m.home_team_id=t.team_id THEN m.home_score ELSE m.away_score END),0) gf, "
                + "COALESCE(SUM(CASE WHEN m.home_team_id=t.team_id THEN m.away_score ELSE m.home_score END),0) ga "
                + "FROM teams t LEFT JOIN matches m ON m.tournament_id=? AND m.status='COMPLETED' "
                + "AND (m.home_team_id=t.team_id OR m.away_team_id=t.team_id) "
                + "WHERE t.tournament_id=? AND t.registration_status='APPROVED' "
                + "GROUP BY t.team_id,t.team_name ORDER BY (won*3+drawn) DESC,(gf-ga) DESC,gf DESC,t.team_name";

        try (PreparedStatement tournamentStatement = connection.prepareStatement(tournamentsSql);
             ResultSet tournaments = tournamentStatement.executeQuery()) {
            while (tournaments.next()) {
                int tournamentId = tournaments.getInt("tournament_id");
                out.println("<h2>" + Html.escape(tournaments.getString("tournament_name")) + " <span class='muted'>(" + Html.escape(tournaments.getString("status")) + ")</span></h2>");
                out.println("<table><tr><th>Team</th><th>P</th><th>W</th><th>D</th><th>L</th><th>GF</th><th>GA</th><th>GD</th><th>Points</th></tr>");
                try (PreparedStatement statement = connection.prepareStatement(standingsSql)) {
                    statement.setInt(1, tournamentId);
                    statement.setInt(2, tournamentId);
                    try (ResultSet result = statement.executeQuery()) {
                        while (result.next()) {
                            int won = result.getInt("won"), drawn = result.getInt("drawn"), gf = result.getInt("gf"), ga = result.getInt("ga");
                            out.println("<tr><td>" + Html.escape(result.getString("team_name")) + "</td><td>" + result.getInt("played") + "</td><td>" + won + "</td><td>" + drawn + "</td><td>" + result.getInt("lost") + "</td><td>" + gf + "</td><td>" + ga + "</td><td>" + (gf - ga) + "</td><td>" + (won * 3 + drawn) + "</td></tr>");
                        }
                    }
                }
                out.println("</table>");
            }
        }
    }

    private void table(PrintWriter out, Connection connection, String sql, String[] headers, String[] columns) throws SQLException {
        out.println("<table><tr>"); for (String header : headers) out.println("<th>" + header + "</th>"); out.println("</tr>");
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                out.println("<tr>");
                for (String column : columns) {
                    String value = column.equals("score") ? result.getInt("home_score") + " - " + result.getInt("away_score") : result.getString(column);
                    out.println("<td>" + Html.escape(value) + "</td>");
                }
                out.println("</tr>");
            }
        }
        out.println("</table>");
    }

    private int scalar(Connection connection, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql); ResultSet result = statement.executeQuery()) { result.next(); return result.getInt(1); }
    }

    private void execute(Connection connection, String sql, Object... values) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) { for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]); statement.executeUpdate(); }
    }

    private String required(HttpServletRequest request, String name) { String value = request.getParameter(name); if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); return value.trim(); }
    private String value(HttpServletRequest request, String name) { return required(request, name); }
    private int intValue(HttpServletRequest request, String name) { try { return Integer.parseInt(required(request, name)); } catch (NumberFormatException exception) { throw new IllegalArgumentException(name + " must be a number"); } }
    private void validateDates(String start, String end) { Date s = Date.valueOf(requiredParam(start, "startDate")), e = Date.valueOf(requiredParam(end, "endDate")); if (e.before(s)) throw new IllegalArgumentException("End date must not be before start date"); }
    private String requiredParam(String value, String name) { if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required"); return value; }
    private void redirect(HttpServletResponse response, HttpServletRequest request, String target) throws IOException { response.sendRedirect(request.getContextPath() + target); }

    private void begin(HttpServletResponse response, String title, String area) throws IOException {
        response.setContentType("text/html;charset=UTF-8"); PrintWriter out = response.getWriter();
        String context = getServletContext().getContextPath();
        String links = "<a href='" + context + "/public/tournaments'>Tournaments</a> <a href='" + context + "/public/teams'>Teams</a> <a href='" + context + "/public/matches'>Matches</a> <a href='" + context + "/public/standings'>Standings</a>";
        if (area.equals("admin")) {
            links = "<a href='" + context + "/admin/dashboard'>Dashboard</a> <a href='" + context + "/admin/tournaments'>Tournaments</a> <a href='" + context + "/admin/teams'>Teams</a> <a href='" + context + "/admin/players'>Players</a> <a href='" + context + "/admin/matches'>Matches</a> <a href='" + context + "/admin/standings'>Standings</a>";
        } else if (area.equals("coadmin")) {
            links = "<a href='" + context + "/coadmin/dashboard'>Dashboard</a> <a href='" + context + "/coadmin/team'>My Team</a> <a href='" + context + "/coadmin/players'>Players</a> <a href='" + context + "/coadmin/matches'>Matches</a>";
        }
        links += " <a href='" + context + "/logout'>Logout</a>";
        out.println("<!doctype html><html lang='en'><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width,initial-scale=1'><title>" + Html.escape(title) + "</title><link rel='stylesheet' href='../css/style.css'></head><body><nav><a href='" + context + "/'>Football Tournament</a><span>" + links + "</span></nav><main>");
    }
    private void heading(PrintWriter out, String title, String subtitle) { out.println("<h1>" + Html.escape(title) + "</h1><p class='muted'>" + Html.escape(subtitle) + "</p>"); }
    private void metric(PrintWriter out, String label, int value) { out.println("<div class='card'><strong>" + label + "</strong><b>" + value + "</b></div>"); }
    private void end(PrintWriter out) { out.println("</main><footer>Football Tournament Management System</footer></body></html>"); }
}
