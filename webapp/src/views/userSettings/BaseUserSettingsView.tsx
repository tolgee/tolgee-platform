import { FunctionComponent, PropsWithChildren } from 'react';
import { Box, Grid, Typography } from '@material-ui/core';

import { BaseView, BaseViewProps } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';
import { useConfig } from 'tg.hooks/useConfig';
import { useUser } from 'tg.hooks/useUser';

import UserOrganizationSettingsSubtitleLink from '../organizations/components/UserOrganizationSettingsSubtitleLink';
import { UserSettingsMenu } from './UserSettingsMenu';

export const BaseUserSettingsView: FunctionComponent<BaseViewProps> = ({
  children,
  title,
  ...otherProps
}: PropsWithChildren<BaseViewProps>) => {
  const user = useUser();
  const config = useConfig();

  const pageHeader = config.authentication ? (
    <>
      <Typography variant="h5">{user?.name}</Typography>
      <UserOrganizationSettingsSubtitleLink isUser={true} />
    </>
  ) : (
    <>
      <Typography variant="h5">{title}</Typography>
    </>
  );

  return (
    <DashboardPage>
      <BaseView
        {...otherProps}
        containerMaxWidth="md"
        customHeader={pageHeader}
      >
        <Grid container>
          {config.authentication && (
            <Grid item lg={3} md={4} sm={12} xs={12}>
              <Box mr={4} mb={4}>
                <UserSettingsMenu />
              </Box>
            </Grid>
          )}
          <Grid item lg md sm xs>
            <Box>
              {config.authentication && (
                <Box mb={2}>
                  <Typography variant="h6">{title}</Typography>
                </Box>
              )}
              {children}
            </Box>
          </Grid>
        </Grid>
      </BaseView>
    </DashboardPage>
  );
};
