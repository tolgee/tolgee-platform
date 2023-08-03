import { useState } from 'react';
import { Box, Select, MenuItem, FormControl, InputLabel } from '@mui/material';
import { useTranslate } from '@tolgee/react';

import { useApiMutation } from 'tg.service/http/useQueryApi';
import { useProject } from 'tg.hooks/useProject';

import { useTranslationsSelector } from '../context/TranslationsContext';
import { OperationProps } from './types';
import { BatchOperationsSubmit } from './components/BatchOperationsSubmit';
import { OperationContainer } from './components/OperationContainer';
import { BatchOperationsLanguagesSelect } from './components/BatchOperationsLanguagesSelect';

type Props = OperationProps;

export const OperationCopyTranslations = ({ disabled, onStart }: Props) => {
  const project = useProject();
  const allLanguages = useTranslationsSelector((c) => c.languages) || [];
  const selection = useTranslationsSelector((c) => c.selection);
  const { t } = useTranslate();

  const [sourceLanguage, setSourceLanguage] = useState<string | null>(null);
  const [selectedLangs, setSelectedLangs] = useState<string[]>([]);

  function handleChangeSource(source: string | null) {
    setSourceLanguage(source);
    setSelectedLangs((langs) => langs?.filter((l) => l !== source));
  }

  const batchLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/start-batch-job/copy-translations',
    method: 'post',
  });

  function handleSubmit() {
    batchLoadable.mutate(
      {
        path: { projectId: project.id },
        content: {
          'application/json': {
            keyIds: selection,
            sourceLanguageId: allLanguages!.find(
              (l) => l.tag === sourceLanguage
            )!.id,
            targetLanguageIds: allLanguages
              ?.filter((l) => selectedLangs?.includes(l.tag))
              .map((l) => l.id),
          },
        },
      },
      {
        onSuccess(data) {
          onStart(data);
        },
      }
    );
  }

  return (
    <OperationContainer flexWrap="wrap">
      <OperationContainer alignItems="center">
        <Box>{t('batch_operations_copy_from_label')}</Box>
        <FormControl
          variant="outlined"
          size="small"
          data-cy="batch-operation-copy-source-select"
        >
          {!sourceLanguage && (
            <InputLabel focused={false} shrink={false}>
              {t('batch_operations_copy_source_language_placeholder')}
            </InputLabel>
          )}
          <Select
            value={sourceLanguage}
            onChange={(e) => handleChangeSource(e.target.value)}
            size="small"
            sx={{ width: 150 }}
            MenuProps={{
              anchorOrigin: {
                vertical: 'top',
                horizontal: 'center',
              },
              transformOrigin: {
                vertical: 'bottom',
                horizontal: 'center',
              },
            }}
          >
            {allLanguages.map((l) => (
              <MenuItem
                key={l.id}
                value={l.tag}
                data-cy="batch-operation-copy-source-select-item"
              >
                {l.name}
              </MenuItem>
            ))}
          </Select>
        </FormControl>
      </OperationContainer>
      <OperationContainer alignItems="center">
        <Box>{t('batch_operations_copy_to_label')}</Box>
        <BatchOperationsLanguagesSelect
          languages={allLanguages || []}
          value={selectedLangs || []}
          onChange={setSelectedLangs}
          disabledLanguages={allLanguages
            .filter((l) => l.tag === sourceLanguage)
            .map((l) => l.id)}
          languagePermission="translations.edit"
        />
        <BatchOperationsSubmit
          loading={batchLoadable.isLoading}
          disabled={disabled || !sourceLanguage || selectedLangs.length === 0}
          onClick={handleSubmit}
        />
      </OperationContainer>
    </OperationContainer>
  );
};
