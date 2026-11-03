package com.football.web;

/** Small HTML helpers used by the servlet views. */
public final class Html {
    private Html() {
    }

    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
