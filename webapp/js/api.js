// TrulyHealthy — shared API helper.
// Same origin as the backend (the Java server serves this file too), so
// relative URLs are enough — no CORS juggling needed at runtime.
const API_BASE = "/api";

const Auth = {
    getToken: () => localStorage.getItem("th_token"),
    getUser: () => JSON.parse(localStorage.getItem("th_user") || "null"),
    save(token, user) {
        localStorage.setItem("th_token", token);
        localStorage.setItem("th_user", JSON.stringify(user));
    },
    clear() {
        localStorage.removeItem("th_token");
        localStorage.removeItem("th_user");
    },
    requireRole(roles) {
        const user = Auth.getUser();
        if (!user || !Auth.getToken()) {
            window.location.href = "index.html";
            return null;
        }
        if (roles && !roles.includes(user.role)) {
            window.location.href = dashboardFor(user.role);
            return null;
        }
        return user;
    }
};

function dashboardFor(role) {
    switch (role) {
        case "PATIENT": return "patient-dashboard.html";
        case "DOCTOR": return "doctor-dashboard.html";
        case "ADMIN": return "admin-dashboard.html";
        case "RESEARCH": return "research-dashboard.html";
        default: return "index.html";
    }
}

async function apiFetch(path, options = {}) {
    const headers = Object.assign({ "Content-Type": "application/json" }, options.headers || {});
    const token = Auth.getToken();
    if (token) headers["Authorization"] = "Bearer " + token;

    const res = await fetch(API_BASE + path, Object.assign({}, options, { headers }));
    if (res.status === 204) return null;

    let data = null;
    try { data = await res.json(); } catch (e) { /* empty body */ }

    if (!res.ok) {
        if (res.status === 401) {
            Auth.clear();
            window.location.href = "index.html";
        }
        throw new Error((data && data.error) || `Request failed (${res.status})`);
    }
    return data;
}

function toast(message, type = "success") {
    const el = document.createElement("div");
    el.className = `toast ${type}`;
    el.textContent = message;
    document.body.appendChild(el);
    setTimeout(() => el.remove(), 3500);
}

function fmtDateTime(iso) {
    if (!iso) return "—";
    const d = new Date(iso);
    return d.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
}

function badgeClass(status) {
    return "badge badge-" + String(status || "").toLowerCase();
}

function el(tag, attrs = {}, children = []) {
    const node = document.createElement(tag);
    for (const [k, v] of Object.entries(attrs)) {
        if (k === "text") node.textContent = v;
        else if (k.startsWith("on")) node.addEventListener(k.substring(2), v);
        else node.setAttribute(k, v);
    }
    (Array.isArray(children) ? children : [children]).forEach(c => {
        if (c) node.appendChild(typeof c === "string" ? document.createTextNode(c) : c);
    });
    return node;
}

function renderTopbar(activeUser, links) {
    const nav = document.getElementById("topbar-nav");
    if (!nav) return;
    nav.innerHTML = "";
    nav.appendChild(el("span", { class: "role-badge", text: activeUser.role }));
    links.forEach(l => nav.appendChild(el("a", { href: l.href, text: l.label })));
    nav.appendChild(el("button", { onclick: async () => {
        try { await apiFetch("/auth/logout", { method: "POST" }); } catch (e) {}
        Auth.clear();
        window.location.href = "index.html";
    }}, "Log out"));
    document.getElementById("welcome-name").textContent = activeUser.fullName;
}
