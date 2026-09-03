// Mirrors util/Money.kt's formatting (es-MX: comma thousands, dot decimal) so amounts look the
// same on the web panel as in the Android app.

export function centsToDisplay(cents, showSign = false) {
  const negative = cents < 0;
  const abs = Math.abs(cents);
  const whole = Math.floor(abs / 100);
  const fraction = abs % 100;
  const grouped = whole.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
  const sign = negative ? "-" : showSign ? "+" : "";
  return `${sign}$ ${grouped}.${fraction.toString().padStart(2, "0")}`;
}

export function parseAmountToCents(text) {
  const cleaned = text.trim().replace(",", ".");
  if (!cleaned) return null;
  const value = parseFloat(cleaned);
  if (Number.isNaN(value) || value < 0) return null;
  return Math.round(value * 100);
}

export function argbToCss(colorArgb) {
  const rgb = colorArgb & 0xffffff;
  return "#" + rgb.toString(16).padStart(6, "0");
}
