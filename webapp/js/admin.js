const user = Auth.requireRole(["ADMIN"]);

if (user) {
    renderTopbar(user, [{ href: "about.html", label: "About" }]);
    loadUsers();
    loadAppointments();
}

document.getElementById("new-role").addEventListener("change", (e) => {
    document.getElementById("doctor-extra-fields").style.display = e.target.value === "DOCTOR" ? "block" : "none";
});

async function loadUsers() {
    const rows = await apiFetch("/admin/users");
    renderStats(rows);
    const tbody = document.querySelector("#users-table tbody");
    tbody.innerHTML = "";
    rows.forEach(u => {
        const toggleLabel = u.active ? "Deactivate" : "Activate";
        tbody.appendChild(el("tr", {}, [
            el("td", { text: u.fullName }),
            el("td", { class: "mono", text: u.username }),
            el("td", {}, el("span", { class: "badge", style: "background:var(--royal-blue-light);color:var(--royal-blue-dark);", text: u.role })),
            el("td", {}, el("span", { class: "badge " + (u.active ? "badge-active" : "badge-inactive"), text: u.active ? "Active" : "Inactive" })),
            el("td", {}, [
                el("button", { class: "btn btn-ghost btn-sm", onclick: () => toggleStatus(u.id, !u.active) }, toggleLabel),
                document.createTextNode(" "),
                el("button", { class: "btn btn-danger btn-sm", onclick: () => removeUser(u.id) }, "Delete")
            ])
        ]));
    });
}

function renderStats(users) {
    const counts = { PATIENT: 0, DOCTOR: 0, ADMIN: 0, RESEARCH: 0 };
    users.forEach(u => counts[u.role] = (counts[u.role] || 0) + 1);
    const wrap = document.getElementById("stat-cards");
    wrap.innerHTML = "";
    [["Patients", counts.PATIENT], ["Doctors", counts.DOCTOR], ["Total accounts", users.length]].forEach(([label, value]) => {
        wrap.appendChild(el("div", { class: "card stat-card" }, [
            el("div", { class: "stat-value", text: String(value) }),
            el("div", { class: "stat-label", text: label })
        ]));
    });
}

async function loadAppointments() {
    const rows = await apiFetch("/appointments");
    const tbody = document.querySelector("#appt-table tbody");
    tbody.innerHTML = "";
    if (!rows.length) {
        tbody.appendChild(el("tr", {}, el("td", { colspan: "4", class: "empty-state", text: "No appointments in the system yet." })));
        return;
    }
    rows.forEach(a => {
        tbody.appendChild(el("tr", {}, [
            el("td", { text: a.patientName }),
            el("td", { text: "Dr. " + a.doctorName }),
            el("td", { text: fmtDateTime(a.appointmentTime) }),
            el("td", {}, el("span", { class: badgeClass(a.status), text: a.status }))
        ]));
    });
}

document.getElementById("create-user-form").addEventListener("submit", async (e) => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(e.target).entries());
    try {
        await apiFetch("/admin/users", { method: "POST", body: JSON.stringify(payload) });
        toast("Account created");
        e.target.reset();
        loadUsers();
    } catch (err) { toast(err.message, "error"); }
});

async function toggleStatus(id, active) {
    try {
        await apiFetch(`/admin/users/${id}/status`, { method: "PUT", body: JSON.stringify({ active }) });
        loadUsers();
    } catch (err) { toast(err.message, "error"); }
}

async function removeUser(id) {
    if (!confirm("Permanently delete this account? This also removes their appointments and records.")) return;
    try {
        await apiFetch(`/admin/users/${id}`, { method: "DELETE" });
        toast("Account deleted");
        loadUsers();
        loadAppointments();
    } catch (err) { toast(err.message, "error"); }
}
