const user = Auth.requireRole(["DOCTOR"]);
let activePatientId = null;
let knownPatients = new Map();

if (user) {
    renderTopbar(user, [{ href: "about.html", label: "About" }]);
    loadAppointments();
    loadProfile();
}

async function loadAppointments() {
    const rows = await apiFetch("/appointments");
    const tbody = document.querySelector("#appt-table tbody");
    tbody.innerHTML = "";
    knownPatients.clear();

    if (!rows.length) {
        tbody.appendChild(el("tr", {}, el("td", { colspan: "5", class: "empty-state", text: "No appointments yet." })));
    }
    rows.forEach(a => {
        knownPatients.set(a.patientId, a.patientName);
        const actions = [];
        if (a.status === "PENDING") {
            actions.push(el("button", { class: "btn btn-ghost btn-sm", onclick: () => setStatus(a.id, "confirm") }, "Confirm"));
        }
        if (["PENDING", "CONFIRMED", "RESCHEDULED"].includes(a.status)) {
            actions.push(document.createTextNode(" "));
            actions.push(el("button", { class: "btn btn-ghost btn-sm", onclick: () => setStatus(a.id, "complete") }, "Mark complete"));
            actions.push(document.createTextNode(" "));
            actions.push(el("button", { class: "btn btn-danger btn-sm", onclick: () => setStatus(a.id, "cancel") }, "Cancel"));
        }
        tbody.appendChild(el("tr", {}, [
            el("td", { text: a.patientName }),
            el("td", { text: fmtDateTime(a.appointmentTime) }),
            el("td", { text: a.reason || "—" }),
            el("td", {}, el("span", { class: badgeClass(a.status), text: a.status })),
            el("td", {}, actions)
        ]));
    });

    renderPatientsTable();
}

async function renderPatientsTable() {
    const tbody = document.querySelector("#patients-table tbody");
    tbody.innerHTML = "";
    if (knownPatients.size === 0) {
        tbody.appendChild(el("tr", {}, el("td", { colspan: "4", class: "empty-state", text: "Patients appear here once they book with you." })));
        return;
    }
    for (const [id, name] of knownPatients) {
        let profile = {};
        try { profile = await apiFetch(`/patients/${id}`); } catch (e) { profile = {}; }
        tbody.appendChild(el("tr", {}, [
            el("td", { text: name }),
            el("td", { text: profile.bloodGroup || "—" }),
            el("td", { text: profile.contact || "—" }),
            el("td", {}, el("button", { class: "btn btn-ghost btn-sm", onclick: () => openRecordsModal(id, name) }, "View / add records"))
        ]));
    }
}

async function setStatus(apptId, action) {
    if (action === "cancel" && !confirm("Cancel this appointment?")) return;
    try {
        await apiFetch(`/appointments/${apptId}/${action}`, { method: "PUT" });
        toast("Updated");
        loadAppointments();
    } catch (err) { toast(err.message, "error"); }
}

async function loadProfile() {
    try {
        const d = await apiFetch(`/doctors/${user.id}`);
        const form = document.getElementById("profile-form");
        form.specialization.value = d.specialization || "";
        form.licenseNo.value = d.licenseNo || "";
        form.contact.value = d.contact || "";
    } catch (e) { /* first login, no profile row yet */ }
}

document.getElementById("profile-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(e.target).entries());
    try {
        await apiFetch(`/doctors/${user.id}`, { method: "PUT", body: JSON.stringify(payload) });
        toast("Profile saved");
    } catch (err) { toast(err.message, "error"); }
});

async function openRecordsModal(patientId, patientName) {
    activePatientId = patientId;
    document.getElementById("records-modal-title").textContent = `${patientName} — medical records`;
    document.getElementById("records-modal").style.display = "flex";
    const rows = await apiFetch(`/patients/${patientId}/records`);
    const tbody = document.querySelector("#patient-records-table tbody");
    tbody.innerHTML = "";
    if (!rows.length) {
        tbody.appendChild(el("tr", {}, el("td", { colspan: "4", class: "empty-state", text: "No records yet." })));
    }
    rows.forEach(r => {
        tbody.appendChild(el("tr", {}, [
            el("td", { text: r.visitDate }),
            el("td", { text: r.diagnosis || "—" }),
            el("td", { text: r.treatment || "—" }),
            el("td", { text: r.prescription || "—" })
        ]));
    });
}

function closeRecordsModal() {
    document.getElementById("records-modal").style.display = "none";
    document.getElementById("add-record-form").reset();
}

document.getElementById("add-record-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(e.target).entries());
    try {
        await apiFetch(`/patients/${activePatientId}/records`, { method: "POST", body: JSON.stringify(payload) });
        toast("Record added");
        openRecordsModal(activePatientId, document.getElementById("records-modal-title").textContent.split(" — ")[0]);
        e.target.reset();
    } catch (err) { toast(err.message, "error"); }
});
