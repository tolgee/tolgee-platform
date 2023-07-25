import React, { FC, useEffect, useState } from 'react';
import { useSelector } from 'react-redux';
import { Redirect } from 'react-router-dom';
import { container } from 'tsyringe';
import { Helmet } from 'react-helmet';
import { useTheme } from '@mui/material';
import {
  useOrganizationUsage,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { GlobalError } from '../error/GlobalError';
import { AppState } from '../store';
import { ErrorActions } from '../store/global/ErrorActions';
import { GlobalActions } from '../store/global/GlobalActions';
import { RedirectionActions } from '../store/global/RedirectionActions';
import ConfirmationDialog from './common/ConfirmationDialog';
import SnackBar from './common/SnackBar';
import { Chatwoot } from './Chatwoot';
import { PlanLimitPopover } from './billing/PlanLimitPopover';
import { RootRouter } from './RootRouter';
import { MandatoryDataProvider } from './MandatoryDataProvider';
import { SensitiveOperationAuthDialog } from './SensitiveOperationAuthDialog';
import { Ga4Tag } from './Ga4Tag';
import { SpendingLimitExceededPopover } from './billing/SpendingLimitExceeded';

const errorActions = container.resolve(ErrorActions);
const redirectionActions = container.resolve(RedirectionActions);

const Redirection = () => {
  const redirectionState = useSelector((state: AppState) => state.redirection);

  useEffect(() => {
    if (redirectionState.to) {
      redirectionActions.redirectDone.dispatch();
    }
  });

  if (redirectionState.to) {
    return <Redirect to={redirectionState.to} />;
  }

  return null;
};

const GlobalConfirmation = () => {
  const state = useSelector(
    (state: AppState) => state.global.confirmationDialog
  );

  const [wasDisplayed, setWasDisplayed] = useState(false);

  const actions = container.resolve(GlobalActions);

  const onCancel = () => {
    state?.onCancel?.();
    actions.closeConfirmation.dispatch();
  };

  const onConfirm = () => {
    state?.onConfirm?.();
    actions.closeConfirmation.dispatch();
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
      <meta name="theme-color" content={theme.palette.navbarBackground.main} />
    </Helmet>
  );
};
export class App extends React.Component {
  componentDidCatch(error: Error, errorInfo: React.ErrorInfo): void {
    errorActions.globalError.dispatch(error as GlobalError);
    throw error;
  }
  render() {
    return (
      <>
        <Head />
        <Redirection />
        <Chatwoot />
        <Ga4Tag />
        <SensitiveOperationAuthDialog />
        <MandatoryDataProvider>
          <RootRouter />
          <SnackBar />
          <GlobalConfirmation />
          <GlobalLimitPopover />
        </MandatoryDataProvider>
      </>
    );
  }
}
