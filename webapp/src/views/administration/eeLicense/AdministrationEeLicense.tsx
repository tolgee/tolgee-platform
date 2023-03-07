import { useTranslate } from '@tolgee/react';
import { BaseView } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useApiQuery } from 'tg.service/http/useQueryApi';
import { styled } from '@mui/material';
import { AdministrationNav } from '../AdministrationNav';
import { SetupLicenceKey } from './SetupLicenceKey';
import { RefreshButton } from './RefreshButton';
import { ReleaseKeyButton } from './ReleaseKeyButton';

const StyledWrapper = styled('div')`
  display: flex;
  flex-direction: column;
  align-items: stretch;

  & .listWrapper > * > * + * {
    border-top: 1px solid ${({ theme }) => theme.palette.divider1.main};
  }
`;

export const AdministrationEeLicense = () => {
  const infoLoadable = useApiQuery({
    url: '/v2/ee-license/info',
    method: 'get',
  });

  const { t } = useTranslate();

  const info = infoLoadable.data;

  return (
    <StyledWrapper>
      <DashboardPage>
        <BaseView
          windowTitle={t('administration_ee_license')}
          containerMaxWidth="lg"
          allCentered
          hideChildrenOnLoading={false}
          loading={infoLoadable.isFetching}
        >
          <AdministrationNav />
          {info ? (
            <>
              <RefreshButton />
              <ReleaseKeyButton />
              <pre>{JSON.stringify(info, null, 2)}</pre>
            </>
          ) : (
            <SetupLicenceKey />
          )}
        </BaseView>
      </DashboardPage>
    </StyledWrapper>
  );
};
