import { useTranslate } from '@tolgee/react';
import { Box, styled } from '@mui/material';

import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { SetupLicenceKey } from '../../eeLicense/SetupLicenceKey';
import { ActiveEeLicense } from '../../eeLicense/ActiveEeLicense';
import { BaseAdministrationView } from 'tg.views/administration/components/BaseAdministrationView';
import { LINKS } from 'tg.constants/links';
import { EeLicenseHint } from '../../eeLicense/EeLicenseHint';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1};
  }
`;

export const AdministrationEeLicenseView = () => {
  const infoLoadable = useApiQuery({
    url: '/v2/ee-license/info',
    method: 'get',
  });

  const { t } = useTranslate();

  const info = infoLoadable.data;

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseAdministrationView
          windowTitle={t('administration_ee_license')}
          navigation={[
            [
              t('administration_ee_license'),
              LINKS.ADMINISTRATION_EE_LICENSE.build(),
            ],
          ]}
          allCentered
          hideChildrenOnLoading={false}
          loading={infoLoadable.isFetching}
        >
          <Box display="grid" gap={2}>
            {info ? <ActiveEeLicense info={info} /> : <SetupLicenceKey />}
            <EeLicenseHint />
          </Box>
        </BaseAdministrationView>
      </DashboardPage>
    </StyledWrapper>
  );
};
