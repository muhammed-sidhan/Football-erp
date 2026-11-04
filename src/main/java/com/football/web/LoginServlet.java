package com.football.web;

import com.football.config.DBConnection;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {
    private static final String FIND_USER = "SELECT user_id, username, password, role, team_id FROM users WHERE username = ?";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        renderLogin(request, response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            renderLogin(request, response, "Enter both username and password.");
            return;
        }

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(FIND_USER)) {
            statement.setString(1, username.trim());
            try (ResultSet result = statement.executeQuery()) {
                if (result.next() && BCrypt.checkpw(password, result.getString("password"))) {
                    var session = request.getSession(true);
                    session.setAttribute("userId", result.getInt("user_id"));
                    session.setAttribute("username", result.getString("username"));
                    session.setAttribute("role", result.getString("role"));
                    int teamId = result.getInt("team_id");
                    session.setAttribute("teamId", result.wasNull() ? null : teamId);
                    String dashboard = "ADMIN".equals(result.getString("role")) ? "/admin/dashboard" : "/coadmin/dashboard";
                    response.sendRedirect(request.getContextPath() + dashboard);
                    return;
                }
            }
            renderLogin(request, response, "Invalid username or password.");
        } catch (Exception exception) {
            getServletContext().log("Unable to authenticate user", exception);
            renderLogin(request, response, "The database is unavailable. Check MySQL and try again.");
        }
    }

    private void renderLogin(HttpServletRequest request, HttpServletResponse response, String error) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        String contextPath = request.getContextPath();
        try (PrintWriter out = response.getWriter()) {
            out.println("<!doctype html><html lang='en'><head><meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                    + "<title>Sign in - Football Tournament</title>"
                    + "<link rel='stylesheet' href='" + Html.escape(contextPath) + "/css/style.css'>"
                    + "</head><body><main class='auth-shell'><section class='auth-copy'>"
                    + "<div class='eyebrow'>Welcome back</div><h1>Ready for match day?</h1>"
                    + "<p>Sign in to manage your team, players, fixtures and tournament results.</p>"
                    + "<a class='button secondary' href='" + Html.escape(contextPath) + "/'>Back to public site</a>"
                    + "</section><section class='auth-card'><h2>Sign in</h2>"
                    + "<p class='muted'>Use your tournament account to continue.</p>");
            if (error != null) {
                out.println("<p class='error'>" + Html.escape(error) + "</p>");
            }
            out.println("<form method='post' action='" + Html.escape(contextPath) + "/login'>"
                    + "<label for='username'>Username</label><input id='username' name='username' required autofocus>"
                    + "<label for='password'>Password</label><input id='password' name='password' type='password' required>"
                    + "<button type='submit'>Sign in</button></form></section></main>"
                    + "<footer>Football Tournament Management System</footer></body></html>");
        }
    }
}
