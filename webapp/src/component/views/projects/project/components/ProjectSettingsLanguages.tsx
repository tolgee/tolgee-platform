import { useState } from 'react';
import { PaginatedHateoasList } from '../../../../common/list/PaginatedHateoasList';
import { Box, ListItem, Typography } from '@material-ui/core';
import ListItemText from '@material-ui/core/ListItemText';
import { FlagImage } from '../../../../languages/FlagImage';
import ListItemSecondaryAction from '@material-ui/core/ListItemSecondaryAction';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from '../../../../../constants/links';
import { SettingsIconButton } from '../../../../common/buttons/SettingsIconButton';
import { T } from '@tolgee/react';
import { CreateSingleLanguage } from '../../../../languages/CreateSingleLanguage';
import { useProject } from '../../../../../hooks/useProject';
import { useApiQuery } from '../../../../../service/http/useQueryApi';

export const ProjectSettingsLanguages = () => {
  const project = useProject();

  const [page, setPage] = useState(0);
  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: {
      pageable: {
        page,
      },
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
            languagesLoadable.refetch();
          }}
        />
      </Box>
    </>
  );
};
