export function getSiteUrl() {
  const value = process.env.NEXT_PUBLIC_SITE_URL?.trim() || "http://localhost:3000";
  return value.replace(/\/$/, "");
}
