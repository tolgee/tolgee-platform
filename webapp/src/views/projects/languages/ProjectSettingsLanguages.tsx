import React from 'react';
import { Box, Typography } from '@material-ui/core';
import { T, useTranslate } from '@tolgee/react';
import { useQueryClient } from 'react-query';
import { Link } from 'react-router-dom';
import clsx from 'clsx';

import { SettingsIconButton } from 'tg.component/common/buttons/SettingsIconButton';
import { CreateSingleLanguage } from 'tg.component/languages/CreateSingleLanguage';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { invalidateUrlPrefix, useApiQuery } from 'tg.service/http/useQueryApi';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { MachineTranslation } from './MachineTranslation/MachineTranslation';
import { LanguageItem } from './LanguageItem';
import { useTableStyles } from './tableStyles';
import { AutoTranslations } from './AutoTranslations/AutoTranslations';

export const ProjectSettingsLanguages = () => {
  const tableClasses = useTableStyles();
  const queryClient = useQueryClient();
  const project = useProject();
  const t = useTranslate();

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 1000,
    },
  });

  useGlobalLoading(languagesLoadable.isLoading);

  return (
    <Box mb={6}>
      <Box mt={4}>
        <Typography variant="h5">
          <T>create_language_title</T>
        </Typography>
      </Box>
      <Box mb={2}>
        <CreateSingleLanguage
          autoFocus={false}
          onCancel={() => {}}
          onCreated={() => {
            invalidateUrlPrefix(queryClient, '/v2/project');
          }}
        />
      </Box>
      <div
        className={tableClasses.table}
        style={{ gridTemplateColumns: '1fr auto auto' }}
        data-cy="project-settings-languages"
      >
        <div className={tableClasses.topRow} />
        <div className={clsx(tableClasses.topRow, tableClasses.centered)}>
          {t('project_languages_base_language')}
        </div>
        <div className={tableClasses.topRow} />

        {languagesLoadable.data?._embedded?.languages?.map((l) => (
          <React.Fragment key={l.id}>
            <div
              className={tableClasses.firstCell}
              data-cy="project-settings-languages-list-name"
            >
              <LanguageItem language={l} />
            </div>
            <div className={tableClasses.centered}>{l?.base ? '✓' : ''}</div>
            <Box
              className={tableClasses.lastCell}
              mt={1}
              mb={1}
              data-cy="project-settings-languages-list-edit-button"
            >
              <Link
                to={LINKS.PROJECT_EDIT_LANGUAGE.build({
                  [PARAMS.PROJECT_ID]: project.id,
                  [PARAMS.LANGUAGE_ID]: l.id,
                })}
              >
                <SettingsIconButton
                  size="small"
                  aria-label={`Settings ${l.name}`}
                />
              </Link>
            </Box>
          </React.Fragment>
        ))}
      </div>
      <Box mt={4} mb={2}>
        <Typography variant="h5">
          <T>machine_translation_title</T>
        </Typography>
      </Box>
      <MachineTranslation />

      <Box mt={4} mb={0}>
        <Typography variant="h5">
          <T>machine_translation_new_keys_title</T>
        </Typography>
      </Box>
      <AutoTranslations />
    </Box>
  );
};
