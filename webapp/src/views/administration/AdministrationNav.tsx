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
          data-cy="administration-tabs-organizations"
          component={Link}
          to={LINKS.ADMINISTRATION_ORGANIZATIONS.build()}
          label={<T keyName="administration_organizations" />}
          value={LINKS.ADMINISTRATION_ORGANIZATIONS.build()}
        />
        <Tab
          data-cy="administration-tabs-users"
          component={Link}
          to={LINKS.ADMINISTRATION_USERS.build()}
          label={<T keyName="administration_users" />}
          value={LINKS.ADMINISTRATION_USERS.build()}
        />
      </Tabs>
    </Box>
  );
};
