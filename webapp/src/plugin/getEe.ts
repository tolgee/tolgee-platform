import { eePlugin } from '../eePlugin.local';

export function getEe() {
  const ee = eePlugin.ee;
  if (!ee) {
    throw new Error('EE plugin not found');
  }
  return ee;
}
