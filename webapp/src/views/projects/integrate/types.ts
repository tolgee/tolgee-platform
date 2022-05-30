import { default as React } from 'react';

export type Guide = {
  name: string;
  icon: React.FC<React.PropsWithChildren<any>>;
  guide: React.FC<React.PropsWithChildren<any>>;
};
