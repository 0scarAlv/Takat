// QR pairing for the Takat PC-access panel. Mirrors network/PairingManager.kt and
// network/crypto/SessionCrypto.kt on the phone side exactly: ECDH (P-256) establishes a shared
// secret, HKDF-SHA256 derives a session key from it, AES-256-GCM encrypts everything from here on
// — including this very polling response. No TLS, no server involved beyond the phone itself.
//
// This uses vendored pure-JS crypto (@noble/curves, @noble/hashes, @noble/ciphers) instead of the
// browser's native Web Crypto (crypto.subtle): SubtleCrypto is only available in a "secure
// context" (https:, or literally localhost) — a plain http://<lan-ip> origin doesn't qualify, so
// crypto.subtle is undefined there. crypto.getRandomValues has no such restriction and is what
// these libraries use internally for randomness.

import { p256 } from "@noble/curves/nist.js";
import { hkdf } from "@noble/hashes/hkdf.js";
import { sha256 } from "@noble/hashes/sha2.js";
import { gcm } from "@noble/ciphers/aes.js";
import { randomBytes } from "@noble/ciphers/utils.js";

const STORAGE_KEY = "takat.device";
const PAIRING_INFO = new TextEncoder().encode("takat-pairing-v1");

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

function decryptEnvelope(key, envelope) {
  const iv = base64ToBytes(envelope.iv);
  const ciphertext = base64ToBytes(envelope.ciphertext);
  const plaintext = gcm(key, iv).decrypt(ciphertext);
  return JSON.parse(new TextDecoder().decode(plaintext));
}

function setStatus(text, cls) {
  const el = document.getElementById("status");
  el.textContent = text;
  el.className = cls || "";
}

async function main() {
  const existing = localStorage.getItem(STORAGE_KEY);
  if (existing) {
    window.location.href = "/dashboard.html";
    return;
  }

  const secretKey = p256.utils.randomSecretKey();
  const publicKeyRaw = p256.getPublicKey(secretKey, false); // uncompressed: 0x04 || X || Y

  const initResponse = await fetch("/pair/init", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ publicKey: bytesToBase64(publicKeyRaw) })
  }).then((r) => r.json());

  const pairingId = initResponse.pairingId;
  const serverPublicKeyRaw = base64ToBytes(initResponse.publicKey);

  // getSharedSecret returns the full encoded point, not just the SEC1 x-coordinate — slice out X
  // (bytes 1..33) to match the plain ECDH secret javax.crypto.KeyAgreement produces on the phone.
  const sharedPoint = p256.getSharedSecret(secretKey, serverPublicKeyRaw, false);
  const sharedSecretX = sharedPoint.slice(1, 33);

  const sessionKey = hkdf(
    sha256,
    sharedSecretX,
    new TextEncoder().encode(pairingId),
    PAIRING_INFO,
    32
  );

  document.getElementById("qr").src = `/pair/qr.png?pairingId=${encodeURIComponent(pairingId)}`;
  setStatus("Esperando a que aceptes en el teléfono…");

  const pollTimer = setInterval(async () => {
    try {
      const envelope = await fetch(`/pair/status?pairingId=${encodeURIComponent(pairingId)}`).then((r) => r.json());
      const payload = decryptEnvelope(sessionKey, envelope);
      if (payload.status === "approved") {
        clearInterval(pollTimer);
        localStorage.setItem(STORAGE_KEY, JSON.stringify({
          deviceToken: payload.deviceToken,
          secret: bytesToBase64(sessionKey)
        }));
        setStatus("¡Vinculado! Abriendo Takat…", "approved");
        document.getElementById("qr").style.display = "none";
        window.location.href = "/dashboard.html";
      }
    } catch (err) {
      clearInterval(pollTimer);
      setStatus("El código expiró, recarga la página.", "error");
    }
  }, 1500);
}

main().catch((err) => setStatus("Error: " + err.message, "error"));
