import React, { FC, useEffect, useState } from 'react';
import { Helmet } from 'react-helmet';
import { useTheme } from '@mui/material';
import { GlobalError } from '../error/GlobalError';
import ConfirmationDialog from './common/ConfirmationDialog';
import { RootRouter } from './RootRouter';
import { MandatoryDataProvider } from './MandatoryDataProvider';
import { SensitiveOperationAuthDialog } from './SensitiveOperationAuthDialog';
import { Ga4Tag } from './Ga4Tag';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { globalContext } from 'tg.globalContext/globalActions';
import { GlobalLimitPopover } from 'tg.ee';

const GlobalConfirmation = () => {
  const state = useGlobalContext((c) => c.confirmationDialog);

  const [wasDisplayed, setWasDisplayed] = useState(false);

  const onCancel = () => {
    state?.onCancel?.();
    globalContext.actions?.closeConfirmation();
  };

  const onConfirm = () => {
    state?.onConfirm?.();
    globalContext.actions?.closeConfirmation();
  };

  useEffect(() => {
    setWasDisplayed(wasDisplayed || !!state);
  }, [!state]);

  if (!wasDisplayed) {
    return null;
  }

  return (
    <ConfirmationDialog
      open={!!state}
      {...state}
      onCancel={onCancel}
      onConfirm={onConfirm}
    />
  );
};

const Head: FC = () => {
  const theme = useTheme();

  return (
    <Helmet>
      <meta name="theme-color" content={theme.palette.navbar.background} />
    </Helmet>
  );
};
export class App extends React.Component {
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    globalContext.actions?.setGlobalError(error as GlobalError);
    throw error;
  }

  render() {
    return (
      <>
        <Head />
        <MandatoryDataProvider>
          <SensitiveOperationAuthDialog />
          <Ga4Tag />
          <RootRouter />
          <GlobalConfirmation />
          <GlobalLimitPopover />
        </MandatoryDataProvider>
      </>
    );
  }
}
