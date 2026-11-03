package com.football.web;

import com.football.config.DBConnection;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/dashboard")
public class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        var session = request.getSession(false);
        if (session == null || session.getAttribute("username") == null) {
            response.sendRedirect(request.getContextPath() + "/login");
            return;
        }

        response.setContentType("text/html;charset=UTF-8");
        String context = request.getContextPath();
        String username = String.valueOf(session.getAttribute("username"));
        String role = String.valueOf(session.getAttribute("role"));
        try (Connection connection = DBConnection.getConnection(); PrintWriter out = response.getWriter()) {
            out.println("<!doctype html><html lang='en'><head><meta charset='UTF-8'>"
                    + "<meta name='viewport' content='width=device-width,initial-scale=1'>"
                    + "<title>Dashboard - Football Tournament</title>"
                    + "<style>body{font-family:Arial;margin:2rem auto;max-width:1000px;padding:0 1rem;color:#1f2937}"
                    + "header{display:flex;justify-content:space-between;align-items:center;gap:1rem;flex-wrap:wrap}"
                    + "a{color:#166534}.cards{display:flex;gap:1rem;flex-wrap:wrap;margin:1.5rem 0}.card{border:1px solid #d1d5db;border-radius:8px;padding:1rem;min-width:130px}"
                    + ".number{font-size:2rem;font-weight:bold;color:#166534}table{border-collapse:collapse;width:100%}th,td{border-bottom:1px solid #e5e7eb;padding:.7rem;text-align:left}"
                    + "</style></head><body><header><div><h1>Dashboard</h1><p>Welcome, "
                    + Html.escape(username) + " (" + Html.escape(role) + ")</p></div><a href='"
                    + context + "/logout'>Sign out</a></header>");

            out.println("<section class='cards'>");
            metric(out, "Tournaments", count(connection, "tournaments"));
            metric(out, "Teams", count(connection, "teams"));
            metric(out, "Players", count(connection, "players"));
            metric(out, "Matches", count(connection, "matches"));
            out.println("</section><h2>Upcoming tournaments</h2><table><tr><th>Name</th><th>Dates</th><th>Location</th><th>Status</th></tr>");
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT tournament_name, start_date, end_date, location, status FROM tournaments ORDER BY start_date")) {
                try (ResultSet result = statement.executeQuery()) {
                    while (result.next()) {
                        out.println("<tr><td>" + Html.escape(result.getString("tournament_name")) + "</td><td>"
                                + result.getDate("start_date") + " to " + result.getDate("end_date") + "</td><td>"
                                + Html.escape(result.getString("location")) + "</td><td>"
                                + Html.escape(result.getString("status")) + "</td></tr>");
                    }
                }
            }
            out.println("</table></body></html>");
        } catch (SQLException exception) {
            getServletContext().log("Unable to load dashboard", exception);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Unable to load dashboard data");
        }
    }

    private void metric(PrintWriter out, String label, int value) {
        out.println("<div class='card'><div>" + label + "</div><div class='number'>" + value + "</div></div>");
    }

    private int count(Connection connection, String table) throws SQLException {
        if (!table.matches("[a-z]+")) {
            throw new IllegalArgumentException("Invalid table name");
        }
        try (PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
             ResultSet result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }
}
