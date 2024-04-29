import { ReactNode } from 'react';
import { VariantType } from 'notistack';

import { globalContext } from 'tg.globalContext/globalActions';

export const messageService = {
  yell(message: ReactNode | string, variant: VariantType) {
    globalContext.actions?.showMessage({ text: message, variant });
  },

  success(message: ReactNode) {
    this.yell(message, 'success');
  },

  error(message: ReactNode) {
    this.yell(message, 'error');
  },
};
