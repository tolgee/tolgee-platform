const LOOPBACK = /^(::1|0{1,4}(:0{1,4}){6}:1|127\.)/i;
const PRIVATE_V4 = /^(10\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)/;
// fc00::/7 (unique local) and fe80::/10 (link local)
const PRIVATE_V6 = /^(f[cd]|fe[89ab])/i;

/**
 * A GeoIP database resolves nothing for these, so they would otherwise surface as a raw address -
 * and `0:0:0:0:0:0:0:1` tells a user far less than "Local network" does.
 */
export function isLocalIp(ip: string): boolean {
  const value = ip.trim();
  return (
    LOOPBACK.test(value) || PRIVATE_V4.test(value) || PRIVATE_V6.test(value)
  );
}
