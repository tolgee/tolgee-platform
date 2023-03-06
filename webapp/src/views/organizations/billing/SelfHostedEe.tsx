import { useTranslate } from '@tolgee/react';
import { Button } from '@mui/material';
import { SelfHostedEeDialog } from './SelfHostedEeDialog';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useOrganization } from '../useOrganization';
import { matchPath, useHistory } from 'react-router-dom';

export const SelfHostedEe = () => {
  const { t } = useTranslate();

  const history = useHistory();

  const organization = useOrganization()!;

  const isSelfHostedRoute = matchPath(location.pathname, {
    path: LINKS.ORGANIZATION_BILLING_SELF_HOSTED_EE.template,
    exact: true,
    strict: true,
  });

  return (
    <>
      <Button
        onClick={() => {
          history.push(
            LINKS.ORGANIZATION_BILLING_SELF_HOSTED_EE.build({
              [PARAMS.ORGANIZATION_SLUG]: organization.slug,
            })
          );
        }}
      >
        {t('organization-billing-get-self-hosted-key')}
      </Button>

      <SelfHostedEeDialog
        open={!!isSelfHostedRoute?.isExact}
        onClose={() =>
          history.push(
            LINKS.ORGANIZATION_BILLING.build({
              [PARAMS.ORGANIZATION_SLUG]: organization.slug,
            })
          )
        }
      />
    </>
  );
};
