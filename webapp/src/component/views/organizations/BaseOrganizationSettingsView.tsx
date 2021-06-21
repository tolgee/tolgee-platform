import { useRouteMatch } from 'react-router-dom';
import { BaseView, BaseViewProps } from '../../layout/BaseView';
import { FunctionComponent, PropsWithChildren } from 'react';
import { Box, Grid, Typography } from '@material-ui/core';
import { OrganizationSettingsMenu } from './components/OrganizationSettingsMenu';
import { useGetOrganization } from '../../../service/hooks/Organization';
import UserOrganizationSettingsSubtitleLink from './components/UserOrganizationSettingsSubtitleLink';
import { PARAMS } from '../../../constants/links';

export const BaseOrganizationSettingsView: FunctionComponent<BaseViewProps> = ({
  children,
  title,
  loading,
  ...otherProps
}: PropsWithChildren<BaseViewProps>) => {
  const match = useRouteMatch();
  const organizationSlug = match.params[PARAMS.ORGANIZATION_SLUG];

  const organization = useGetOrganization(organizationSlug);

  return (
    <BaseView
      {...otherProps}
      containerMaxWidth="md"
      loading={organization.isLoading || loading}
      customHeader={
        <>
          <Typography variant="h5">{organization.data?.name}</Typography>
          <UserOrganizationSettingsSubtitleLink isUser={false} />
        </>
      }
      hideChildrenOnLoading={false}
    >
      <Grid container>
        <Grid item lg={3} md={4}>
          <Box mr={4} mb={4}>
            <OrganizationSettingsMenu />
          </Box>
        </Grid>
        <Grid item lg={9} md={8} sm={12} xs={12}>
          {title && (
            <Box mb={2}>
              <Box>
                <Typography variant="h6">{title}</Typography>
              </Box>
            </Box>
          )}
          {children}
        </Grid>
      </Grid>
    </BaseView>
  );
};
