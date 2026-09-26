package com.trulyhealthy.model;

public class Appointment {
    public int id;
    public int patientId;
    public String patientName;
    public int doctorId;
    public String doctorName;
    public String appointmentTime; // ISO 8601 "yyyy-MM-dd'T'HH:mm:ss"
    public String status;          // PENDING, CONFIRMED, CANCELLED, COMPLETED, RESCHEDULED
    public String reason;
}
