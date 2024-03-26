import { VariantType, useSnackbar } from 'notistack';
import { ReactNode } from 'react';

export type Message = {
  text: ReactNode | string;
  variant: VariantType;
};

export const useMessageService = () => {
  const { enqueueSnackbar } = useSnackbar();

  const actions = {
    showMessage(m: Message) {
      enqueueSnackbar(m.text, { variant: m.variant });
    },
  };

  return { actions };
};
