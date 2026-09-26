const user = Auth.requireRole(["PATIENT"]);
let rescheduleTargetId = null;

if (user) {
    renderTopbar(user, [{ href: "about.html", label: "About" }]);
    loadAppointments();
    loadRecords();
    loadProfile();
    loadDoctors();
}

async function loadAppointments() {
    const rows = await apiFetch("/appointments");
    const tbody = document.querySelector("#appt-table tbody");
    tbody.innerHTML = "";
    if (!rows.length) {
        tbody.appendChild(el("tr", {}, el("td", { colspan: "5", class: "empty-state", text: "No appointments yet — book one above." })));
        return;
    }
    rows.forEach(a => {
        const canAct = ["PENDING", "CONFIRMED", "RESCHEDULED"].includes(a.status);
        const actions = el("td", {}, canAct ? [
            el("button", { class: "btn btn-ghost btn-sm", onclick: () => openRescheduleModal(a.id) }, "Reschedule"),
            " ",
            el("button", { class: "btn btn-danger btn-sm", onclick: () => cancelAppt(a.id) }, "Cancel")
        ] : []);
        tbody.appendChild(el("tr", {}, [
            el("td", { text: "Dr. " + a.doctorName }),
            el("td", { text: fmtDateTime(a.appointmentTime) }),
            el("td", { text: a.reason || "—" }),
            el("td", {}, el("span", { class: badgeClass(a.status), text: a.status })),
            actions
        ]));
    });
}

async function loadRecords() {
    const me = Auth.getUser();
    const rows = await apiFetch(`/patients/${me.id}/records`);
    const tbody = document.querySelector("#records-table tbody");
    tbody.innerHTML = "";
    if (!rows.length) {
        tbody.appendChild(el("tr", {}, el("td", { colspan: "5", class: "empty-state", text: "No visits recorded yet." })));
        return;
    }
    rows.forEach(r => {
        tbody.appendChild(el("tr", {}, [
            el("td", { text: r.visitDate }),
            el("td", { text: "Dr. " + r.doctorName }),
            el("td", { text: r.diagnosis || "—" }),
            el("td", { text: r.treatment || "—" }),
            el("td", { text: r.prescription || "—" })
        ]));
    });
}

async function loadProfile() {
    const me = Auth.getUser();
    const p = await apiFetch(`/patients/${me.id}`);
    const form = document.getElementById("profile-form");
    form.dob.value = p.dob || "";
    form.gender.value = p.gender || "";
    form.bloodGroup.value = p.bloodGroup || "";
    form.contact.value = p.contact || "";
    form.address.value = p.address || "";
}

async function loadDoctors() {
    const doctors = await apiFetch("/doctors");
    const select = document.getElementById("doctor-select");
    select.innerHTML = "";
    doctors.forEach(d => {
        select.appendChild(el("option", { value: d.userId, text: `Dr. ${d.fullName} — ${d.specialization || "General"}` }));
    });
}

document.getElementById("profile-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const me = Auth.getUser();
    const payload = Object.fromEntries(new FormData(e.target).entries());
    try {
        await apiFetch(`/patients/${me.id}`, { method: "PUT", body: JSON.stringify(payload) });
        toast("Profile saved");
    } catch (err) { toast(err.message, "error"); }
});

document.getElementById("book-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(e.target).entries());
    try {
        await apiFetch("/appointments", { method: "POST", body: JSON.stringify(payload) });
        toast("Appointment requested");
        closeBookModal();
        loadAppointments();
    } catch (err) { toast(err.message, "error"); }
});

document.getElementById("reschedule-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(e.target).entries());
    try {
        await apiFetch(`/appointments/${rescheduleTargetId}/reschedule`, { method: "PUT", body: JSON.stringify(payload) });
        toast("Appointment rescheduled");
        closeRescheduleModal();
        loadAppointments();
    } catch (err) { toast(err.message, "error"); }
});

async function cancelAppt(id) {
    if (!confirm("Cancel this appointment?")) return;
    try {
        await apiFetch(`/appointments/${id}/cancel`, { method: "PUT" });
        toast("Appointment cancelled");
        loadAppointments();
    } catch (err) { toast(err.message, "error"); }
}

function openBookModal() { document.getElementById("book-modal").style.display = "flex"; }
function closeBookModal() { document.getElementById("book-modal").style.display = "none"; document.getElementById("book-form").reset(); }
function openRescheduleModal(id) { rescheduleTargetId = id; document.getElementById("reschedule-modal").style.display = "flex"; }
function closeRescheduleModal() { document.getElementById("reschedule-modal").style.display = "none"; document.getElementById("reschedule-form").reset(); }
