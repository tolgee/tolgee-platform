import { FunctionComponent, PropsWithChildren } from 'react';
import { Box, Grid } from '@mui/material';

import { BaseView, BaseViewProps } from 'tg.component/layout/BaseView';
import { DashboardPage } from 'tg.component/layout/DashboardPage';

export const BaseSettingsView: FunctionComponent<BaseViewProps> = ({
  children,
  ...otherProps
}: PropsWithChildren<BaseViewProps>) => {
  return (
    <DashboardPage>
      <BaseView {...otherProps} containerMaxWidth="md">
        <Grid container>
          <Grid item lg md sm xs>
            <Box>{children}</Box>
          </Grid>
        </Grid>
      </BaseView>
    </DashboardPage>
  );
};
