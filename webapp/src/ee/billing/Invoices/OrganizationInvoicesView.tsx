import { FunctionComponent } from 'react';
import { useTranslate } from '@tolgee/react';

import { BaseOrganizationSettingsView } from 'tg.views/organizations/components/BaseOrganizationSettingsView';
import { LINKS } from 'tg.constants/links';
import { useOrganization } from 'tg.views/organizations/useOrganization';
import { CustomerPortal } from '../CustomerPortal/CustomerPortal';
import { Invoices } from './Invoices';
import { styled } from '@mui/material';

const StyledWrapper = styled('div')`
  display: grid;
  grid-template-areas:
    'customerPortal'
    'invoices';
  grid-template-columns: 1fr;
  align-items: start;
  justify-content: space-between;
  gap: 32px 24px;
  margin-bottom: 32px;
  @media (max-width: 1400px) {
    grid-template-columns: 1fr;
    grid-template-areas:
      'usage'
      'customerPortal'
      'invoices';
  }
`;

export const OrganizationInvoicesView: FunctionComponent = () => {
  const organization = useOrganization();

  const { t } = useTranslate();

  const url = new URL(window.location.href);

  url.search = '';

  return (
    <BaseOrganizationSettingsView
      hideChildrenOnLoading={true}
      link={LINKS.ORGANIZATION_BILLING}
      navigation={[
        [
          t('organization_menu_invoices'),
          LINKS.ORGANIZATION_BILLING.build({ slug: organization!.slug }),
        ],
      ]}
      windowTitle={t({ key: 'organization_invoices_title', noWrap: true })}
      maxWidth="normal"
    >
      <StyledWrapper>
        <CustomerPortal />
        <Invoices />
      </StyledWrapper>
    </BaseOrganizationSettingsView>
  );
};
