import { VariantType } from 'notistack';
import { ReactNode } from 'react';

export type SecurityDTO = {
  allowPrivate: boolean;
  jwtToken: string | undefined | null;
  loginErrorCode: string | null;
  allowRegistration: boolean;
};

export class Message {
  constructor(public text: ReactNode | string, public variant: VariantType) {}
}
