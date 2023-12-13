import React from 'react';
import { Box, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import clsx from 'clsx';

import { SettingsIconButton } from 'tg.component/common/buttons/SettingsIconButton';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { invalidateUrlPrefix, useApiQuery } from 'tg.service/http/useQueryApi';
import { MachineTranslation } from './MachineTranslation/MachineTranslation';
import { LanguageItem } from './LanguageItem';
import { useConfig } from 'tg.globalContext/helpers';
import {
  StyledLanguageTable,
  TABLE_CENTERED,
  TABLE_FIRST_CELL,
  TABLE_LAST_CELL,
  TABLE_TOP_ROW,
} from './tableStyles';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';
import { CreateSingleLanguage } from 'tg.component/languages/CreateSingleLanguage';
import { useQueryClient } from 'react-query';

export const ProjectSettingsLanguages = () => {
  const queryClient = useQueryClient();
  const project = useProject();
  const { t } = useTranslate();
  const config = useConfig();
  const { satisfiesPermission } = useProjectPermissions();

  const canEditLanguages = satisfiesPermission('languages.edit');

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 1000,
    },
  });

  const mtEnabled = Object.values(
    config?.machineTranslationServices.services
  ).some(({ enabled }) => enabled);

  return (
    <Box mb={6}>
      <QuickStartHighlight
        itemKey="add_language"
        message={t('quick_start_item_add_language_hint')}
        borderRadius="5px"
        offset={10}
      >
        <Box>
          {canEditLanguages && (
            <>
              <Box mt={4}>
                <Typography variant="h5">
                  <T keyName="create_language_title" />
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
            </>
          )}
          <StyledLanguageTable
            style={{ gridTemplateColumns: '1fr auto auto' }}
            data-cy="project-settings-languages"
          >
            <div className={TABLE_TOP_ROW} />
            <div className={clsx(TABLE_TOP_ROW, TABLE_CENTERED)}>
              {t('project_languages_base_language')}
            </div>
            <div className={TABLE_TOP_ROW} />

            {languagesLoadable.data?._embedded?.languages?.map((l) => (
              <React.Fragment key={l.id}>
                <div
                  className={TABLE_FIRST_CELL}
                  data-cy="project-settings-languages-list-name"
                >
                  <LanguageItem language={l} />
                </div>
                <div className={TABLE_CENTERED}>{l?.base ? '✓' : ''}</div>
                {canEditLanguages && (
                  <Box
                    className={TABLE_LAST_CELL}
                    mt={1}
                    mb={1}
                    data-cy="project-settings-languages-list-edit-button"
                    data-cy-language={l.tag}
                  >
                    <Link
                      to={LINKS.PROJECT_EDIT_LANGUAGE.build({
                        [PARAMS.PROJECT_ID]: project.id,
                        [PARAMS.LANGUAGE_ID]: l.id,
                      })}
                    >
                      <SettingsIconButton
                        disabled={!canEditLanguages}
                        size="small"
                        aria-label={`Settings ${l.name}`}
                      />
                    </Link>
                  </Box>
                )}
              </React.Fragment>
            ))}
          </StyledLanguageTable>
        </Box>
      </QuickStartHighlight>

      {mtEnabled && (
        <QuickStartHighlight
          itemKey="machine_translation"
          message={t('quick_start_item_machine_translation_hint')}
          borderRadius="5px"
          offset={10}
        >
          <Box>
            <Box mt={4} mb={2}>
              <Typography variant="h5">
                <T keyName="machine_translation_title" />
              </Typography>
            </Box>
            <MachineTranslation />
          </Box>
        </QuickStartHighlight>
      )}
    </Box>
  );
};
