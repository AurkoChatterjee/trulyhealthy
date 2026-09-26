-- TrulyHealthy EHR — schema + demo seed data
-- Run with: mysql -u root -p < schema.sql

DROP DATABASE IF EXISTS trulyhealthy;
CREATE DATABASE trulyhealthy CHARACTER SET utf8mb4;
USE trulyhealthy;

-- ── Users & roles ────────────────────────────────────────────────
CREATE TABLE users (
    id            INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(150) NOT NULL,      -- format: salt$sha256hex
    role          ENUM('PATIENT','DOCTOR','ADMIN','RESEARCH') NOT NULL,
    full_name     VARCHAR(120) NOT NULL,
    email         VARCHAR(120),
    active        BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE patients (
    user_id     INT PRIMARY KEY,
    dob         DATE,
    gender      VARCHAR(15),
    blood_group VARCHAR(5),
    contact     VARCHAR(20),
    address     VARCHAR(255),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE doctors (
    user_id        INT PRIMARY KEY,
    specialization VARCHAR(100),
    license_no     VARCHAR(50),
    contact        VARCHAR(20),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Research / Government officials — read-only, anonymized access
CREATE TABLE officials (
    user_id      INT PRIMARY KEY,
    organization VARCHAR(150),
    designation  VARCHAR(100),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ── Clinical data ────────────────────────────────────────────────
CREATE TABLE appointments (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    patient_id        INT NOT NULL,
    doctor_id         INT NOT NULL,
    appointment_time  DATETIME NOT NULL,
    status            ENUM('PENDING','CONFIRMED','CANCELLED','COMPLETED','RESCHEDULED') DEFAULT 'PENDING',
    reason            VARCHAR(255),
    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(user_id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id)  REFERENCES doctors(user_id)  ON DELETE CASCADE
);

CREATE TABLE medical_records (
    id           INT AUTO_INCREMENT PRIMARY KEY,
    patient_id   INT NOT NULL,
    doctor_id    INT NOT NULL,
    visit_date   DATE NOT NULL,
    diagnosis    VARCHAR(255),
    treatment    VARCHAR(255),
    prescription VARCHAR(255),
    notes        TEXT,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (patient_id) REFERENCES patients(user_id) ON DELETE CASCADE,
    FOREIGN KEY (doctor_id)  REFERENCES doctors(user_id)  ON DELETE CASCADE
);

-- ── Sessions (server-side token store) ──────────────────────────
CREATE TABLE sessions (
    token      VARCHAR(64) PRIMARY KEY,
    user_id    INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- ── Demo seed data ───────────────────────────────────────────────
-- Passwords below are pre-hashed with the app's salt$sha256(salt+password) scheme.
-- admin/Admin@123, doctor1/Doctor@123, doctor2/Doctor@123,
-- patient1/Patient@123, patient2/Patient@123, research1/Research@123

INSERT INTO users (username, password_hash, role, full_name, email) VALUES
('admin',     'a1b2c3d4e5f6a7b8$ee00cff77166897aebea9a20a3ed8af17bdb372dfb60f98588e87ffbe21c1819', 'ADMIN',    'System Administrator', 'admin@trulyhealthy.local'),
('doctor1',   'b2c3d4e5f6a7b8c9$b85e776d5e268c84ab5aba1070add2fa52394c9fba65f7fd33d4b81df53fdd2d', 'DOCTOR',   'Dr. Priya Sharma',      'priya.sharma@trulyhealthy.local'),
('doctor2',   'c3d4e5f6a7b8c9d0$afe9a7137f41ea76e521a958e4c2c416f68e8bfe4f53dc3d9e535af3d6815a64', 'DOCTOR',   'Dr. Arjun Mehta',       'arjun.mehta@trulyhealthy.local'),
('patient1',  'd4e5f6a7b8c9d0e1$e9c8fec755c1475edda8120417d6a36efad087931064020be662a6282e3bfb20', 'PATIENT',  'Rahul Verma',           'rahul.verma@example.com'),
('patient2',  'e5f6a7b8c9d0e1f2$609ba35e04bc8dea399fb26e80015eb952bbbbc32e2bdf09c1869ce3af685022', 'PATIENT',  'Sneha Iyer',            'sneha.iyer@example.com'),
('research1', 'f6a7b8c9d0e1f2a3$69ab86965847efac2bc4510d2f88b07d7ad42c2cfbeaebfcc8b3e417ec4ec615', 'RESEARCH', 'Dr. Kavita Rao',        'kavita.rao@moh.gov.in');

INSERT INTO doctors (user_id, specialization, license_no, contact)
SELECT id, 'Cardiology', 'MCI-88213', '+91-9876500001' FROM users WHERE username = 'doctor1';
INSERT INTO doctors (user_id, specialization, license_no, contact)
SELECT id, 'General Medicine', 'MCI-77104', '+91-9876500002' FROM users WHERE username = 'doctor2';

INSERT INTO patients (user_id, dob, gender, blood_group, contact, address)
SELECT id, '1998-03-14', 'Male', 'O+', '+91-9812345001', 'Indore, MP' FROM users WHERE username = 'patient1';
INSERT INTO patients (user_id, dob, gender, blood_group, contact, address)
SELECT id, '2001-07-22', 'Female', 'B+', '+91-9812345002', 'Bhopal, MP' FROM users WHERE username = 'patient2';

INSERT INTO officials (user_id, organization, designation)
SELECT id, 'Ministry of Health & Family Welfare', 'Research Officer' FROM users WHERE username = 'research1';

INSERT INTO appointments (patient_id, doctor_id, appointment_time, status, reason)
SELECT p.user_id, d.user_id, '2026-10-02 10:30:00', 'CONFIRMED', 'Routine cardiac checkup'
FROM patients p, doctors d
WHERE p.user_id = (SELECT id FROM users WHERE username='patient1')
  AND d.user_id = (SELECT id FROM users WHERE username='doctor1');

INSERT INTO medical_records (patient_id, doctor_id, visit_date, diagnosis, treatment, prescription, notes)
SELECT p.user_id, d.user_id, '2026-06-15', 'Mild hypertension', 'Lifestyle changes + monitoring', 'Amlodipine 5mg OD', 'BP 142/90 at visit. Advised low-sodium diet and follow-up in 3 months.'
FROM patients p, doctors d
WHERE p.user_id = (SELECT id FROM users WHERE username='patient1')
  AND d.user_id = (SELECT id FROM users WHERE username='doctor1');
