import React, { FC, useEffect, useState } from 'react';
import { Helmet } from 'react-helmet';
import { useTheme } from '@mui/material';
import {
  useOrganizationUsage,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { GlobalError } from '../error/GlobalError';
import ConfirmationDialog from './common/ConfirmationDialog';
import { PlanLimitPopover } from './billing/PlanLimitPopover';
import { RootRouter } from './RootRouter';
import { MandatoryDataProvider } from './MandatoryDataProvider';
import { SensitiveOperationAuthDialog } from './SensitiveOperationAuthDialog';
import { Ga4Tag } from './Ga4Tag';
import { SpendingLimitExceededPopover } from './billing/SpendingLimitExceeded';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { globalContext } from 'tg.globalContext/globalActions';

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

const GlobalLimitPopover = () => {
  const { planLimitErrors, spendingLimitErrors } = useOrganizationUsage();
  const [planLimitErrOpen, setPlanLimitErrOpen] = useState(false);
  const [spendingLimitErrOpen, setSpendingLimitErrOpen] = useState(false);

  useEffect(() => {
    if (planLimitErrors === 1) {
      setPlanLimitErrOpen(true);
    }
  }, [planLimitErrors]);

  useEffect(() => {
    if (spendingLimitErrors > 0) {
      setSpendingLimitErrOpen(true);
    }
  }, [spendingLimitErrors]);

  const { preferredOrganization } = usePreferredOrganization();

  return preferredOrganization ? (
    <>
      <PlanLimitPopover
        open={planLimitErrOpen}
        onClose={() => setPlanLimitErrOpen(false)}
      />
      <SpendingLimitExceededPopover
        open={spendingLimitErrOpen}
        onClose={() => setSpendingLimitErrOpen(false)}
      />
    </>
  ) : null;
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
