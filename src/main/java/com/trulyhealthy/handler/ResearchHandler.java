package com.trulyhealthy.handler;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.trulyhealthy.dao.ResearchDao;
import com.trulyhealthy.model.User;

import java.io.IOException;
import java.util.Optional;

/**
 * GET /api/research/stats -> anonymized, aggregate-only statistics.
 * Available to RESEARCH and ADMIN roles. Never exposes patient names or free text.
 */
public class ResearchHandler extends BaseHandler implements HttpHandler {

    private final ResearchDao researchDao = new ResearchDao();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        if (handlePreflight(ex)) return;
        try {
            Optional<User> caller = requireRole(ex, "RESEARCH", "ADMIN");
            if (caller.isEmpty()) return;
            sendJson(ex, 200, researchDao.summary());
        } catch (Exception e) {
            sendError(ex, 500, "Server error: " + e.getMessage());
        }
    }
}
