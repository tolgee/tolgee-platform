import React, { useState } from 'react';
import { Box, Button, Typography } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import clsx from 'clsx';
import { useQueryClient } from 'react-query';

import { SettingsIconButton } from 'tg.component/common/buttons/SettingsIconButton';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { invalidateUrlPrefix, useApiQuery } from 'tg.service/http/useQueryApi';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { QuickStartHighlight } from 'tg.component/layout/QuickStartGuide/QuickStartHighlight';

import { LanguageItem } from '../../../component/languages/LanguageItem';
import {
  StyledLanguageTable,
  TABLE_CENTERED,
  TABLE_FIRST_CELL,
  TABLE_LAST_CELL,
  TABLE_TOP_ROW,
} from '../../../component/languages/tableStyles';
import { Plus } from '@untitled-ui/icons-react';
import { LanguagesAddDialog } from 'tg.component/languages/LanguagesAddDialog';

export const ProjectLanguages = () => {
  const queryClient = useQueryClient();
  const project = useProject();
  const { t } = useTranslate();
  const { satisfiesPermission } = useProjectPermissions();
  const [addLanguageOpen, setAddLanguageOpen] = useState(false);

  function handleClose() {
    setAddLanguageOpen(false);
  }

  const canEditLanguages = satisfiesPermission('languages.edit');

  const languagesLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/languages',
    method: 'get',
    path: { projectId: project.id },
    query: {
      size: 1000,
    },
  });

  return (
    <Box mb={6}>
      <QuickStartHighlight
        itemKey="add_language"
        message={t('quick_start_item_add_language_hint')}
        borderRadius="5px"
        offset={10}
      >
        <Box>
          <Box
            mt={4}
            mb={3}
            display="flex"
            justifyContent="space-between"
            alignItems="center"
          >
            <Typography variant="h5">
              <T keyName="project_languages_title" />
            </Typography>
            {canEditLanguages && (
              <Button
                color="primary"
                variant="contained"
                startIcon={<Plus width={19} height={19} />}
                onClick={() => setAddLanguageOpen(true)}
                data-cy="project-settings-languages-add"
              >
                {t('project_languages_add_button')}
              </Button>
            )}
          </Box>

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
      {addLanguageOpen && languagesLoadable.data && (
        <LanguagesAddDialog
          onClose={handleClose}
          onCreated={() => {
            handleClose();
          }}
          onChangesMade={() => {
            invalidateUrlPrefix(queryClient, '/v2/project');
          }}
          existingLanguages={
            languagesLoadable.data._embedded?.languages?.map((l) => l.tag) || []
          }
        />
      )}
    </Box>
  );
};
