import { styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { LINKS } from 'tg.constants/links';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { CarryOversSection } from './CarryOversSection';
import { OrgInvoicesSection } from './OrgInvoicesSection';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  gap: 40px;
`;

export const AdministrationInvoicesView = () => {
  const { t } = useTranslate();

  return (
    <DashboardPage>
      <BaseAdministrationView
        windowTitle={t('administration_invoices', 'Invoices')}
        navigation={[
          [
            t('administration_organizations'),
            LINKS.ADMINISTRATION_ORGANIZATIONS.build(),
          ],
        ]}
        allCentered
        hideChildrenOnLoading={false}
      >
        <StyledWrapper>
          <OrgInvoicesSection />
          <CarryOversSection />
        </StyledWrapper>
      </BaseAdministrationView>
    </DashboardPage>
  );
};
