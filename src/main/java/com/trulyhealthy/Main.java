package com.trulyhealthy;

import com.sun.net.httpserver.HttpServer;
import com.trulyhealthy.config.DBConfig;
import com.trulyhealthy.handler.*;

import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.util.concurrent.Executors;

/**
 * TrulyHealthy — Electronic Health Record system.
 * Run from the project root so the relative "webapp" folder resolves:
 *   java -jar target/trulyhealthy.jar
 */
public class Main {

    public static void main(String[] args) throws Exception {
        int port = DBConfig.SERVER_PORT;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.setExecutor(Executors.newFixedThreadPool(16));

        // ── API routes ──────────────────────────────────────────
        server.createContext("/api/auth", new AuthHandler());
        server.createContext("/api/appointments", new AppointmentHandler());
        server.createContext("/api/patients", new PatientHandler());
        server.createContext("/api/doctors", new DoctorHandler());
        server.createContext("/api/admin/users", new AdminHandler());
        server.createContext("/api/research", new ResearchHandler());

        // ── Static frontend ─────────────────────────────────────
        Path webRoot = Path.of("webapp").toAbsolutePath().normalize();
        server.createContext("/", new StaticFileHandler(webRoot));

        server.start();
        System.out.println("=================================================");
        System.out.println(" TrulyHealthy EHR server running");
        System.out.println(" Web UI:  http://localhost:" + port);
        System.out.println(" API:     http://localhost:" + port + "/api/...");
        System.out.println(" Serving frontend from: " + webRoot);
        System.out.println("=================================================");
    }
}
