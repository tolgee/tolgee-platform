import { useState } from 'react';
import { useHistory } from 'react-router-dom';
import { IconButton, Menu, MenuItem } from '@mui/material';
import { DotsVertical } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { components } from 'tg.service/apiSchema.generated';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

type LanguageModel = components['schemas']['LanguageModel'];

type Props = {
  language: LanguageModel;
};

export const LanguageMenu: React.FC<Props> = ({ language }) => {
  const [anchorEl, setAnchorEl] = useState<HTMLElement | undefined>();
  const history = useHistory();
  const project = useProject();
  const { satisfiesPermission, satisfiesLanguageAccess } =
    useProjectPermissions();

  const closeWith = (action?: () => void) => (e) => {
    e?.stopPropagation();
    setAnchorEl(undefined);
    action?.();
  };

  const handleOpen = (e: React.MouseEvent<HTMLButtonElement, MouseEvent>) => {
    e.stopPropagation();
    setAnchorEl(e.target as HTMLElement);
  };

  const redirectToSettings = () => {
    history.push(
      LINKS.PROJECT_EDIT_LANGUAGE.build({
        [PARAMS.PROJECT_ID]: project.id,
        [PARAMS.LANGUAGE_ID]: language.id,
      })
    );
  };

  const redirectToExport = () => {
    history.push(
      LINKS.PROJECT_EXPORT.build({
        [PARAMS.PROJECT_ID]: project.id,
      }) + `?languages=${language.tag}`
    );
  };

  const canViewLanguage = satisfiesLanguageAccess(
    'translations.view',
    language.id
  );
  const canEditLanguages = satisfiesPermission('languages.edit');

  return (
    <>
      {(canViewLanguage || canEditLanguages) && (
        <IconButton
          onClick={handleOpen}
          data-cy="project-dashboard-language-menu"
        >
          <DotsVertical />
        </IconButton>
      )}
      <Menu anchorEl={anchorEl} open={Boolean(anchorEl)} onClose={closeWith()}>
        {canEditLanguages && (
          <MenuItem
            onClick={closeWith(redirectToSettings)}
            data-cy="project-dashboard-language-menu-settings"
          >
            <T keyName="language_settings_title" />
          </MenuItem>
        )}
        {canViewLanguage && (
          <MenuItem
            onClick={closeWith(redirectToExport)}
            data-cy="project-dashboard-language-menu-export"
          >
            <T keyName="export_translations_title" />
          </MenuItem>
        )}
      </Menu>
    </>
  );
};
