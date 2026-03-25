
// Cambia esta URL por el dominio/IP de tu EC2 Spring cuando despliegues en AWS
const API_BASE = "https://TU_DOMINIO_SPRING:8443";

const regEmail      = document.getElementById("reg-email");
const regPassword   = document.getElementById("reg-password");
const regBtn        = document.getElementById("reg-btn");
const regOutput     = document.getElementById("reg-output");

const loginEmail    = document.getElementById("login-email");
const loginPassword = document.getElementById("login-password");
const loginBtn      = document.getElementById("login-btn");
const loginOutput   = document.getElementById("login-output");

const helloBtn      = document.getElementById("hello-btn");
const helloOutput   = document.getElementById("hello-output");

const statusDot     = document.getElementById("status-dot");
const statusLabel   = document.getElementById("status-label");

let basicAuthHeader = "";

function setOutput(el, message, type = "") {
  el.textContent = typeof message === "string"
    ? message
    : JSON.stringify(message, null, 2);
  el.className = "output" + (type ? ` ${type}` : "");
}

function setStatus(connected) {
  statusDot.className   = "status-dot " + (connected ? "connected" : "error");
  statusLabel.textContent = connected ? "conectado" : "error de conexión";
}

async function register() {
  setOutput(regOutput, "Registrando...");
  const email    = regEmail.value.trim();
  const password = regPassword.value;

  if (!email || !password) {
    setOutput(regOutput, "Completa email y contraseña.", "error");
    return;
  }

  try {
    const res = await fetch(`${API_BASE}/api/auth/register`, {
      method:  "POST",
      headers: { "Content-Type": "application/json" },
      body:    JSON.stringify({ email, password }),
    });

    const data = await res.json();

    if (res.ok) {
      setOutput(regOutput, data, "success");
      setStatus(true);
    } else {
      setOutput(regOutput, data, "error");
      setStatus(false);
    }
  } catch (err) {
    setOutput(regOutput, `Error de red: ${err.message}`, "error");
    setStatus(false);
  }
}

async function login() {
  setOutput(loginOutput, "Autenticando...");
  const email    = loginEmail.value.trim();
  const password = loginPassword.value;

  if (!email || !password) {
    setOutput(loginOutput, "Completa email y contraseña.", "error");
    return;
  }


  basicAuthHeader = `Basic ${btoa(`${email}:${password}`)}`;

  try {
    const res = await fetch(`${API_BASE}/api/auth/login`, {
      method:  "POST",
      headers: { Authorization: basicAuthHeader },
    });

    const data = await res.json();

    if (res.ok) {
      setOutput(loginOutput, data, "success");
      statusDot.className    = "status-dot connected";
      statusLabel.textContent = `conectado como ${data.user ?? email}`;
    } else {
      basicAuthHeader = "";
      setOutput(loginOutput, data, "error");
      setStatus(false);
    }
  } catch (err) {
    basicAuthHeader = "";
    setOutput(loginOutput, `Error de red: ${err.message}`, "error");
    setStatus(false);
  }
}

async function callHello() {
  if (!basicAuthHeader) {
    setOutput(helloOutput, "Inicia sesión primero.", "error");
    return;
  }

  setOutput(helloOutput, "Llamando /api/hello...");

  try {
    const res = await fetch(`${API_BASE}/api/hello`, {
      headers: { Authorization: basicAuthHeader },
    });

    const data = await res.json();

    if (res.ok) {
      setOutput(helloOutput, data, "success");
    } else {
      setOutput(helloOutput, data, "error");
    }
  } catch (err) {
    setOutput(helloOutput, `Error de red: ${err.message}`, "error");
    setStatus(false);
  }
}

regBtn.addEventListener("click",   register);
loginBtn.addEventListener("click", login);
helloBtn.addEventListener("click", callHello);

[regEmail, regPassword].forEach(el =>
  el.addEventListener("keydown", e => e.key === "Enter" && register())
);
[loginEmail, loginPassword].forEach(el =>
  el.addEventListener("keydown", e => e.key === "Enter" && login())
);