import { ReactNode } from 'react';
import { VariantType } from 'notistack';

export type SecurityDTO = {
  allowPrivate: boolean;
  jwtToken: string | undefined | null;
  adminJwtToken: string | undefined | null;
  loginErrorCode: string | null;
  allowRegistration: boolean;
};

export class Message {
  constructor(public text: ReactNode | string, public variant: VariantType) {}
}
