import React from 'react';
import { ListItemLink } from '../../common/list/ListItemLink';
import { LINKS, PARAMS } from '../../../constants/links';
import ListItemText from '@material-ui/core/ListItemText';
import { Button, Chip } from '@material-ui/core';
import { ProjectPermissionType } from '../../../service/response.types';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import { Link } from 'react-router-dom';
import { T } from '@tolgee/react';
import { components } from '../../../service/apiSchema';

const ProjectListItem = (r: components['schemas']['ProjectModel']) => {
  return (
    <ListItemLink
      key={r.id}
      to={LINKS.REPOSITORY_TRANSLATIONS.build({ [PARAMS.REPOSITORY_ID]: r.id })}
    >
      <ListItemText>
        {r.name}{' '}
        <Chip
          data-cy="project-list-owner"
          size="small"
          label={r.organizationOwnerName || r.userOwner?.name}
        />
      </ListItemText>
      {r.computedPermissions === ProjectPermissionType.MANAGE && (
        <ListItemSecondaryAction>
          <Button
            data-cy="project-settings-button"
            component={Link}
            size="small"
            variant="outlined"
            to={LINKS.REPOSITORY_EDIT.build({ [PARAMS.REPOSITORY_ID]: r.id })}
          >
            <T>project_settings_button</T>
          </Button>
        </ListItemSecondaryAction>
      )}
    </ListItemLink>
  );
};

export default ProjectListItem;
