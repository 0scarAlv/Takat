// Encrypted API client for the Takat PC-access panel. Every /api request/response body is
// AES-256-GCM-encrypted with the device secret established at pairing time (see pair.js and
// network/LocalApiServer.kt on the phone side) — a fresh random IV per message, no key reuse.

import { gcm } from "@noble/ciphers/aes.js";
import { randomBytes } from "@noble/ciphers/utils.js";

const STORAGE_KEY = "takat.device";

function bytesToBase64(bytes) {
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary);
}

function base64ToBytes(b64) {
  const binary = atob(b64);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
  return bytes;
}

export function getDevice() {
  const raw = localStorage.getItem(STORAGE_KEY);
  return raw ? JSON.parse(raw) : null;
}

export function isPaired() {
  return getDevice() !== null;
}

export function forgetDevice() {
  localStorage.removeItem(STORAGE_KEY);
}

async function requestText(method, path, body) {
  const device = getDevice();
  if (!device) throw new Error("No hay una PC vinculada.");
  const secret = base64ToBytes(device.secret);

  const headers = { "X-Takat-Device": device.deviceToken };
  let requestBody;
  if (body !== undefined) {
    const nonce = randomBytes(12);
    const plaintext = new TextEncoder().encode(JSON.stringify(body));
    const ciphertext = gcm(secret, nonce).encrypt(plaintext);
    requestBody = JSON.stringify({ iv: bytesToBase64(nonce), ciphertext: bytesToBase64(ciphertext) });
    headers["Content-Type"] = "application/json";
  }

  const res = await fetch(path, { method, headers, body: requestBody });
  if (res.status === 401) {
    forgetDevice();
    window.location.href = "/";
    throw new Error("La vinculación ya no es válida, volvé a escanear el código.");
  }
  if (!res.ok) throw new Error(`${method} ${path} falló (${res.status})`);

  const envelope = await res.json();
  const iv = base64ToBytes(envelope.iv);
  const ciphertext = base64ToBytes(envelope.ciphertext);
  const plaintext = gcm(secret, iv).decrypt(ciphertext);
  return new TextDecoder().decode(plaintext);
}

async function request(method, path, body) {
  return JSON.parse(await requestText(method, path, body));
}

export const api = {
  accounts: () => request("GET", "/api/accounts"),
  categories: () => request("GET", "/api/categories"),
  movements: () => request("GET", "/api/movements"),
  totals: () => request("GET", "/api/totals"),
  addTransaction: (body) => request("POST", "/api/transactions", body),
  deleteTransaction: (id) => request("DELETE", `/api/transactions/${id}`),
  addTransfer: (body) => request("POST", "/api/transfers", body),
  deleteTransfer: (id) => request("DELETE", `/api/transfers/${id}`),
  exportCsv: () => requestText("GET", "/api/export.csv")
};
