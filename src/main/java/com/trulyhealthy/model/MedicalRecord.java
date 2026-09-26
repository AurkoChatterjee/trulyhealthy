package com.trulyhealthy.model;

public class MedicalRecord {
    public int id;
    public int patientId;
    public String patientName;
    public int doctorId;
    public String doctorName;
    public String visitDate;   // ISO yyyy-MM-dd
    public String diagnosis;
    public String treatment;
    public String prescription;
    public String notes;
}
