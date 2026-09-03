import { api, isPaired } from "./api-client.js";
import { centsToDisplay, parseAmountToCents, argbToCss } from "./money.js";

if (!isPaired()) {
  window.location.href = "/";
}

let accounts = [];
let categories = [];
let currentType = "expense"; // "expense" | "income" | "transfer"

function escapeHtml(text) {
  const div = document.createElement("div");
  div.textContent = text ?? "";
  return div.innerHTML;
}

function dayLabel(dateMs) {
  const d = new Date(dateMs);
  const today = new Date();
  const startOfDay = (x) => new Date(x.getFullYear(), x.getMonth(), x.getDate()).getTime();
  const diffDays = Math.round((startOfDay(today) - startOfDay(d)) / 86400000);
  if (diffDays === 0) return "Hoy";
  if (diffDays === 1) return "Ayer";
  return d.toLocaleDateString("es-MX", { day: "2-digit", month: "long", year: diffDays > 300 ? "numeric" : undefined });
}

function renderTotals(totals) {
  document.getElementById("total-balance").textContent = centsToDisplay(totals.availableCents);
  const breakdown = document.getElementById("hero-breakdown");
  breakdown.innerHTML = "";
  const chips = [];
  if (totals.debtCents !== 0) chips.push(["Deuda", totals.debtCents]);
  if (totals.pendingFixedExpensesCents !== 0) chips.push(["Gastos fijos pendientes", -totals.pendingFixedExpensesCents]);
  for (const [label, cents] of chips) {
    const chip = document.createElement("span");
    chip.className = "chip";
    chip.innerHTML = `${escapeHtml(label)}: <strong>${centsToDisplay(cents)}</strong>`;
    breakdown.appendChild(chip);
  }
}

function renderAccounts(list) {
  const el = document.getElementById("accounts");
  el.innerHTML = "";
  for (const account of list) {
    const card = document.createElement("div");
    card.className = "account-card";
    card.innerHTML =
      `<div class="account-name"><span class="account-dot" style="background:${argbToCss(account.colorArgb)}"></span>${escapeHtml(account.name)}</div>` +
      `<div class="account-balance">${centsToDisplay(account.balanceCents)}</div>`;
    el.appendChild(card);
  }
}

function movementRowEl(m) {
  const row = document.createElement("div");
  row.className = "movement-row";

  const title = m.type === "transfer"
    ? `${m.fromAccountName ?? "?"} → ${m.toAccountName ?? "?"}`
    : (m.categoryName ?? "Sin categoría");
  const subtitleParts = m.type === "transfer" ? ["Transferencia"] : [m.accountName, m.note].filter(Boolean);

  const info = document.createElement("div");
  info.className = "movement-info";
  info.innerHTML =
    `<div class="movement-title">${escapeHtml(title)}</div>` +
    `<div class="movement-subtitle">${escapeHtml(subtitleParts.join(" · "))}</div>`;

  const amount = document.createElement("div");
  const amountClass = m.type === "transfer" ? "neutral" : (m.amountCents < 0 ? "negative" : "positive");
  amount.className = "movement-amount " + amountClass;
  amount.textContent = m.type === "transfer" ? centsToDisplay(m.amountCents) : centsToDisplay(m.amountCents, true);

  row.appendChild(info);
  row.appendChild(amount);

  const del = document.createElement("button");
  del.className = "delete-btn";
  del.textContent = "✕";
  del.title = "Eliminar";
  del.addEventListener("click", async () => {
    if (!confirm("¿Eliminar este movimiento?")) return;
    if (m.type === "transaction") await api.deleteTransaction(m.id);
    else await api.deleteTransfer(m.id);
    await loadAll();
  });
  row.appendChild(del);

  return row;
}

function renderMovements(list) {
  const el = document.getElementById("movements");
  el.innerHTML = "";
  if (list.length === 0) {
    el.innerHTML = '<p class="empty">Todavía no hay movimientos.</p>';
    return;
  }
  let lastLabel = null;
  for (const m of list) {
    const label = dayLabel(m.date);
    if (label !== lastLabel) {
      const header = document.createElement("div");
      header.className = "date-header";
      header.textContent = label;
      el.appendChild(header);
      lastLabel = label;
    }
    el.appendChild(movementRowEl(m));
  }
}

function populateCategorySelect() {
  const wantKind = currentType === "income" ? "INCOME" : "EXPENSE";
  const filtered = categories.filter((c) => c.kind === wantKind || c.kind === "BOTH");
  const select = document.getElementById("f-category");
  select.innerHTML = '<option value="">Sin categoría</option>' +
    filtered.map((c) => `<option value="${c.id}">${escapeHtml(c.name)}</option>`).join("");
}

function setType(type) {
  currentType = type;
  document.querySelectorAll(".type-toggle button").forEach((b) => b.classList.toggle("active", b.dataset.type === type));
  const isTransfer = type === "transfer";
  document.getElementById("fields-single").style.display = isTransfer ? "none" : "";
  document.getElementById("fields-transfer").style.display = isTransfer ? "" : "none";
  if (!isTransfer) populateCategorySelect();
}

function populateForm() {
  const options = accounts.map((a) => `<option value="${a.id}">${escapeHtml(a.name)}</option>`).join("");
  document.getElementById("f-account").innerHTML = options;
  document.getElementById("f-from-account").innerHTML = options;
  document.getElementById("f-to-account").innerHTML = options;
  populateCategorySelect();
}

async function loadAll() {
  const [accountsRes, categoriesRes, movementsRes, totalsRes] = await Promise.all([
    api.accounts(),
    api.categories(),
    api.movements(),
    api.totals()
  ]);
  accounts = accountsRes;
  categories = categoriesRes;
  renderTotals(totalsRes);
  renderAccounts(accounts);
  renderMovements(movementsRes);
  populateForm();
}

document.querySelectorAll(".type-toggle button").forEach((btn) => {
  btn.addEventListener("click", () => setType(btn.dataset.type));
});

const dialog = document.getElementById("add-dialog");
document.getElementById("fab").addEventListener("click", () => {
  document.getElementById("add-form").reset();
  setType("expense");
  document.getElementById("f-date").value = new Date().toISOString().slice(0, 10);
  dialog.showModal();
});
document.getElementById("f-cancel").addEventListener("click", () => dialog.close());

document.getElementById("add-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const amountCents = parseAmountToCents(document.getElementById("f-amount").value);
  if (amountCents === null || amountCents === 0) {
    alert("Ingresá un monto válido.");
    return;
  }
  const note = document.getElementById("f-note").value.trim() || null;
  const dateValue = document.getElementById("f-date").value;
  const date = new Date(dateValue + "T12:00:00").getTime();

  try {
    if (currentType === "transfer") {
      const fromAccountId = Number(document.getElementById("f-from-account").value);
      const toAccountId = Number(document.getElementById("f-to-account").value);
      if (fromAccountId === toAccountId) {
        alert("Elegí dos cuentas distintas.");
        return;
      }
      await api.addTransfer({ fromAccountId, toAccountId, amountCents, note, date });
    } else {
      const accountId = Number(document.getElementById("f-account").value);
      const categoryValue = document.getElementById("f-category").value;
      const categoryId = categoryValue ? Number(categoryValue) : null;
      const signedAmount = currentType === "expense" ? -amountCents : amountCents;
      await api.addTransaction({ accountId, categoryId, amountCents: signedAmount, note, date });
    }
    dialog.close();
    await loadAll();
  } catch (err) {
    alert("No se pudo guardar: " + err.message);
  }
});

loadAll().catch((err) => {
  document.getElementById("movements").innerHTML = `<p class="empty">Error: ${escapeHtml(err.message)}</p>`;
});
