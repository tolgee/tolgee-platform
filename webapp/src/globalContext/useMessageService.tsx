import { ReactNode } from 'react';
import { IconButton, styled, touchRippleClasses } from '@mui/material';
import { VariantType, useSnackbar, closeSnackbar } from 'notistack';
import { X } from '@untitled-ui/icons-react';

const StyledButton = styled(IconButton)`
  color: white;
  &:hover {
    background-color: #ffffff1b;
  }
  .${touchRippleClasses.child} {
    background-color: white;
  }
`;

export type Message = {
  text: ReactNode | string;
  variant: VariantType;
};

const action = (snackbarId) => (
  <>
    <StyledButton
      className="notistack-CloseButton"
      size="small"
      onClick={() => {
        closeSnackbar(snackbarId);
      }}
    >
      <X />
    </StyledButton>
  </>
);

export const useMessageService = () => {
  const { enqueueSnackbar } = useSnackbar();

  const actions = {
    showMessage(m: Message) {
      enqueueSnackbar(m.text, {
        variant: m.variant,
        action,
        style: { maxWidth: 700 },
      });
    },
  };

  return { actions };
};
