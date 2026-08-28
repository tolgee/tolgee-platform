import { ReactNode } from 'react';
import { Box } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';

import { CompactView } from 'tg.component/layout/CompactView';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { DisplayedRefusal } from 'tg.fixtures/refusal';

const MESSAGES: Record<DisplayedRefusal, ReactNode> = {
  'no-organization': (
    <T
      keyName="no_organization_message"
      defaultValue="You are not a member of any organization."
    />
  ),
  'server-disallows': (
    <T
      keyName="organization_create_disallowed_message"
      defaultValue="This server doesn't allow users to create organizations."
    />
  ),
  sso: <TranslatedError code="sso_user_cannot_create_organization" />,
  'billing-not-an-owner': (
    <T
      keyName="billing_requires_organization_owner"
      defaultValue="Billing is only available to owners of the organization."
    />
  ),
};

type Props = {
  reason: DisplayedRefusal;
};

export const NoPermissionsView = ({ reason }: Props) => {
  const { t } = useTranslate();

  return (
    <DashboardPage>
      <CompactView
        primaryContent={
          <Box data-cy="no-permissions-message" data-cy-reason={reason}>
            {MESSAGES[reason]}
          </Box>
        }
        title={
          <T keyName="no-permissions-title" defaultValue="No permission" />
        }
        windowTitle={t('no-permissions-title', 'No permission')}
      />
    </DashboardPage>
  );
};
