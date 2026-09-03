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

function shortDate(dateMs) {
  return new Date(dateMs).toLocaleDateString("es-MX", { day: "2-digit", month: "short", year: "2-digit" });
}

function renderTotals(totals) {
  document.getElementById("total-balance").textContent = centsToDisplay(totals.availableCents);
  const breakdown = document.getElementById("hero-breakdown");
  breakdown.innerHTML = "";
  const rows = [["Capital", totals.capitalCents]];
  if (totals.debtCents !== 0) rows.push(["Deuda", totals.debtCents]);
  if (totals.pendingFixedExpensesCents !== 0) rows.push(["Gastos fijos pendientes", -totals.pendingFixedExpensesCents]);
  for (const [label, cents] of rows) {
    const row = document.createElement("div");
    row.className = "row";
    row.innerHTML = `<span>${escapeHtml(label)}</span><strong>${centsToDisplay(cents)}</strong>`;
    breakdown.appendChild(row);
  }
}

function renderAccounts(list) {
  const el = document.getElementById("accounts");
  el.innerHTML = "";
  for (const account of list) {
    const row = document.createElement("div");
    row.className = "account-row";
    row.innerHTML =
      `<span class="account-name"><span class="account-dot" style="background:${argbToCss(account.colorArgb)}"></span>${escapeHtml(account.name)}</span>` +
      `<span class="account-balance">${centsToDisplay(account.balanceCents)}</span>`;
    el.appendChild(row);
  }
}

function movementRowEl(m) {
  const tr = document.createElement("tr");

  const title = m.type === "transfer"
    ? `${m.fromAccountName ?? "?"} → ${m.toAccountName ?? "?"}`
    : (m.categoryName ?? "Sin categoría");
  const subtitleParts = m.type === "transfer" ? ["Transferencia"] : [m.accountName, m.note].filter(Boolean);

  const dateTd = document.createElement("td");
  dateTd.className = "mv-date";
  dateTd.textContent = shortDate(m.date);

  const infoTd = document.createElement("td");
  infoTd.innerHTML =
    `<div class="mv-title">${escapeHtml(title)}</div>` +
    `<div class="mv-subtitle">${escapeHtml(subtitleParts.join(" · "))}</div>`;

  const amountTd = document.createElement("td");
  amountTd.className = "num";
  const amountClass = m.type === "transfer" ? "neutral" : (m.amountCents < 0 ? "negative" : "positive");
  const amountSpan = document.createElement("span");
  amountSpan.className = "amount " + amountClass;
  amountSpan.textContent = m.type === "transfer" ? centsToDisplay(m.amountCents) : centsToDisplay(m.amountCents, true);
  amountTd.appendChild(amountSpan);

  const actionsTd = document.createElement("td");
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
  actionsTd.appendChild(del);

  tr.append(dateTd, infoTd, amountTd, actionsTd);
  return tr;
}

function renderMovements(list) {
  const body = document.getElementById("movements-body");
  body.innerHTML = "";
  if (list.length === 0) {
    const tr = document.createElement("tr");
    const td = document.createElement("td");
    td.colSpan = 4;
    td.className = "empty";
    td.textContent = "Todavía no hay movimientos.";
    tr.appendChild(td);
    body.appendChild(tr);
    return;
  }
  for (const m of list) body.appendChild(movementRowEl(m));
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
document.getElementById("add-btn").addEventListener("click", () => {
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

document.getElementById("export-csv").addEventListener("click", async (e) => {
  const btn = e.currentTarget;
  const originalText = btn.textContent;
  btn.disabled = true;
  btn.textContent = "Generando…";
  try {
    const csv = await api.exportCsv();
    const blob = new Blob([csv], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    const stamp = new Date().toISOString().slice(0, 10);
    a.href = url;
    a.download = `takat_${stamp}.csv`;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  } catch (err) {
    alert("No se pudo exportar: " + err.message);
  } finally {
    btn.disabled = false;
    btn.textContent = originalText;
  }
});

loadAll().catch((err) => {
  document.getElementById("movements-body").innerHTML =
    `<tr><td colspan="4" class="empty">Error: ${escapeHtml(err.message)}</td></tr>`;
});
