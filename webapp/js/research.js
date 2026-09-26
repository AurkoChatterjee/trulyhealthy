const user = Auth.requireRole(["RESEARCH"]);

if (user) {
    renderTopbar(user, [{ href: "about.html", label: "About" }]);
    loadStats();
}

async function loadStats() {
    const s = await apiFetch("/research/stats");

    const wrap = document.getElementById("stat-cards");
    wrap.innerHTML = "";
    [
        ["Total patients", s.totalPatients],
        ["Total doctors", s.totalDoctors],
        ["Total appointments", s.totalAppointments],
        ["Completed visits", s.completedAppointments],
        ["Cancelled appointments", s.cancelledAppointments],
    ].forEach(([label, value]) => {
        wrap.appendChild(el("div", { class: "card stat-card" }, [
            el("div", { class: "stat-value", text: String(value) }),
            el("div", { class: "stat-label", text: label })
        ]));
    });

    renderBreakdown("gender-breakdown", s.genderBreakdown);
    renderBreakdown("blood-breakdown", s.bloodGroupBreakdown);
    renderBreakdown("spec-breakdown", s.specializationLoad);
    renderBreakdown("diagnosis-breakdown", s.topDiagnoses);
}

function renderBreakdown(containerId, rows) {
    const container = document.getElementById(containerId);
    container.innerHTML = "";
    if (!rows || !rows.length) {
        container.appendChild(el("div", { class: "empty-state", text: "No data yet." }));
        return;
    }
    const max = Math.max(...rows.map(r => r.count));
    rows.forEach(r => {
        const pct = max ? Math.round((r.count / max) * 100) : 0;
        container.appendChild(el("div", { style: "margin-bottom:12px;" }, [
            el("div", { class: "flex-between", style: "margin-bottom:4px;" }, [
                el("span", { text: r.label || "Unspecified" }),
                el("span", { class: "mono", text: String(r.count) })
            ]),
            el("div", { style: "background:var(--border); border-radius:999px; height:6px; overflow:hidden;" },
                el("div", { style: `background:var(--royal-blue); width:${pct}%; height:100%;` }))
        ]));
    });
}
