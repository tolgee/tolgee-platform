import { useState } from 'react';
import { Box, ListItem, Typography } from '@material-ui/core';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import ListItemText from '@material-ui/core/ListItemText';
import { T } from '@tolgee/react';
import { useQueryClient } from 'react-query';
import { Link } from 'react-router-dom';

import { SettingsIconButton } from 'tg.component/common/buttons/SettingsIconButton';
import { PaginatedHateoasList } from 'tg.component/common/list/PaginatedHateoasList';
import { CreateSingleLanguage } from 'tg.component/languages/CreateSingleLanguage';
import { FlagImage } from 'tg.component/languages/FlagImage';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { invalidateUrlPrefix, useApiQuery } from 'tg.service/http/useQueryApi';

export const ProjectSettingsLanguages = () => {
  const queryClient = useQueryClient();
  const project = useProject();

  const [page, setPage] = useState(0);
  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: {
      page,
    },
  });

  return (
    <>
      <PaginatedHateoasList
        loadable={languagesLoadable}
        onPageChange={setPage}
        renderItem={(l) => (
          <ListItem key={l.id} data-cy="project-settings-languages-list-item">
            <ListItemText>
              <Box display="inline-flex" justifyContent="center">
                <Box mr={1} display="inline-flex" justifyContent="center">
                  <FlagImage width={20} flagEmoji={l.flagEmoji || '🏁'} />
                </Box>
                {l.name} | {l.originalName} ({l.tag})
              </Box>
            </ListItemText>
            <ListItemSecondaryAction>
              <Link
                data-cy="project-settings-languages-list-edit-button"
                to={LINKS.PROJECT_LANGUAGE_EDIT.build({
                  [PARAMS.PROJECT_ID]: project.id,
                  [PARAMS.LANGUAGE_ID]: l.id,
                })}
              >
                <SettingsIconButton />
              </Link>
            </ListItemSecondaryAction>
          </ListItem>
        )}
      />

      <Box mt={4}>
        <Typography variant="h5">
          <T>create_language_title</T>
        </Typography>
      </Box>
      <Box mt={2} minHeight={400}>
        <CreateSingleLanguage
          autoFocus={false}
          onCancel={() => {}}
          onCreated={() => {
            invalidateUrlPrefix(queryClient, '/v2/project');
          }}
        />
      </Box>
    </>
  );
};
