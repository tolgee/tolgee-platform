import { Link, useLocation } from 'react-router-dom';
import { Box, Tab, Tabs } from '@mui/material';
import { LINKS } from 'tg.constants/links';
import { T } from '@tolgee/react';

export const AdministrationNav = () => {
  const location = useLocation();

  return (
    <Box mb={2}>
      <Tabs value={location.pathname}>
        <Tab
          component={Link}
          to={LINKS.ADMINISTRATION_ORGANIZATIONS.build()}
          label={<T>administration_organizations</T>}
          value={LINKS.ADMINISTRATION_ORGANIZATIONS.build()}
        />
        <Tab
          component={Link}
          to={LINKS.ADMINISTRATION_USERS.build()}
          label={<T>administration_users</T>}
          value={LINKS.ADMINISTRATION_USERS.build()}
        />
      </Tabs>
    </Box>
  );
};
