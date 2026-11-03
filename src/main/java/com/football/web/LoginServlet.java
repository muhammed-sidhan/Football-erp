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
        renderLogin(response, null);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String username = request.getParameter("username");
        String password = request.getParameter("password");

        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            renderLogin(response, "Enter both username and password.");
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
            renderLogin(response, "Invalid username or password.");
        } catch (Exception exception) {
            getServletContext().log("Unable to authenticate user", exception);
            renderLogin(response, "The database is unavailable. Check MySQL and try again.");
        }
    }

    private void renderLogin(HttpServletResponse response, String error) throws IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            out.println("<!doctype html><html lang='en'><head><meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                    + "<title>Login - Football Tournament</title>"
                    + "<style>body{font-family:Arial;margin:3rem auto;max-width:420px;padding:0 1rem}"
                    + ".card{border:1px solid #d1d5db;border-radius:10px;padding:2rem;box-shadow:0 2px 8px #0001}"
                    + "label{display:block;margin-top:1rem;font-weight:bold}input{box-sizing:border-box;width:100%;padding:.7rem;margin-top:.35rem}"
                    + "button{margin-top:1.4rem;width:100%;padding:.7rem;background:#166534;color:white;border:0;border-radius:5px}"
                    + ".error{color:#b91c1c;margin-top:1rem}</style></head><body><main class='card'>"
                    + "<h1>Football Tournament</h1><h2>Sign in</h2>");
            if (error != null) {
                out.println("<p class='error'>" + Html.escape(error) + "</p>");
            }
            out.println("<form method='post' action='login'>"
                    + "<label for='username'>Username</label><input id='username' name='username' required autofocus>"
                    + "<label for='password'>Password</label><input id='password' name='password' type='password' required>"
                    + "<button type='submit'>Sign in</button></form></main></body></html>");
        }
    }
}
