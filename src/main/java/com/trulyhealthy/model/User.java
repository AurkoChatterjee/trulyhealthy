package com.trulyhealthy.model;

public class User {
    public int id;
    public String username;
    public String passwordHash; // never serialized out
    public String role; // PATIENT, DOCTOR, ADMIN, RESEARCH
    public String fullName;
    public String email;
    public boolean active = true;

    public User() {}

    public User(int id, String username, String role, String fullName, String email, boolean active) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.fullName = fullName;
        this.email = email;
        this.active = active;
    }

    /** Safe view for JSON responses — omits the password hash. */
    public static class PublicView {
        public int id;
        public String username;
        public String role;
        public String fullName;
        public String email;
        public boolean active;

        public PublicView(User u) {
            this.id = u.id;
            this.username = u.username;
            this.role = u.role;
            this.fullName = u.fullName;
            this.email = u.email;
            this.active = u.active;
        }
    }
}
