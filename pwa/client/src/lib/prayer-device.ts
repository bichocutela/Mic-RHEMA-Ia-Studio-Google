const DEVICE_ID_KEY = "micrhema:prayer-device-id";
const DEVICE_SECRET_KEY = "micrhema:prayer-device-secret";

export type PrayerDeviceIdentity = { deviceId: string; deviceSecret: string };

function randomId(prefix: string) {
  const webCrypto = globalThis.crypto;
  if (typeof webCrypto.randomUUID === "function") return `${prefix}_${webCrypto.randomUUID()}`;
  const bytes = new Uint8Array(24);
  webCrypto.getRandomValues(bytes);
  return `${prefix}_${Array.from(bytes, (value) => value.toString(16).padStart(2, "0")).join("")}`;
}

export function getPrayerDeviceIdentity(): PrayerDeviceIdentity {
  let deviceId = localStorage.getItem(DEVICE_ID_KEY) || "";
  let deviceSecret = localStorage.getItem(DEVICE_SECRET_KEY) || "";
  if (!deviceId) {
    deviceId = randomId("device");
    localStorage.setItem(DEVICE_ID_KEY, deviceId);
  }
  if (!deviceSecret) {
    deviceSecret = randomId("secret");
    localStorage.setItem(DEVICE_SECRET_KEY, deviceSecret);
  }
  return { deviceId, deviceSecret };
}
