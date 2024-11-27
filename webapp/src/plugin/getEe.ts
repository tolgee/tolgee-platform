import { eePlugin } from '../eePlugin.ee';

export const getEe = () => {
  const ee = eePlugin.ee;
  if (!ee) {
    throw new Error('EE plugin not found');
  }
  return ee;
};
