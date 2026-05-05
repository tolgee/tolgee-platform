import React, { useMemo, useState } from 'react';
import {
  Autocomplete,
  Button,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { useDebounce } from 'use-debounce';

import {
  useApiInfiniteQuery,
  useApiMutation,
} from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import LoadingButton from 'tg.component/common/form/LoadingButton';

const PROJECT_SEARCH_DEBOUNCE_MS = 250;

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
};

type ProjectOption = {
  id: number;
  name: string;
};

export const CopyFromProjectDialog: React.VFC<Props> = ({
  open,
  onClose,
  onFinished,
  organizationId,
  translationMemoryId,
  sourceLanguageTag,
}) => {
  const { t } = useTranslate();

  const [search, setSearch] = useState('');
  const [debounced] = useDebounce(search, PROJECT_SEARCH_DEBOUNCE_MS);
  const [selected, setSelected] = useState<ProjectOption | null>(null);

  // Filter projects server-side to those whose base language matches the TM. The backend
  // rejects mismatched copies, so showing them in the picker would be a guaranteed-failure UX.
  const projectsLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{id}/projects',
    method: 'get',
    path: { id: organizationId },
    query: {
      search: debounced,
      size: 30,
      filterBaseLanguageTag: sourceLanguageTag,
    },
    options: {
      keepPreviousData: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { id: organizationId },
            query: {
              search: debounced,
              size: 30,
              filterBaseLanguageTag: sourceLanguageTag,
              page: lastPage.page!.number! + 1,
            },
          };
        }
        return null;
      },
    },
  });

  const options = useMemo<ProjectOption[]>(
    () =>
      projectsLoadable.data?.pages
        .flatMap((p) => p._embedded?.projects ?? [])
        .map((p) => ({ id: p.id, name: p.name })) ?? [],
    [projectsLoadable.data]
  );

  const copyMutation = useApiMutation({
    url: '/v2/organizations/{organizationId}/translation-memories/{translationMemoryId}/copy-from-project',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/translation-memories',
  });

  const handleConfirm = () => {
    if (!selected) return;
    copyMutation.mutate(
      {
        path: { organizationId, translationMemoryId },
        content: {
          'application/json': { sourceProjectId: selected.id },
        },
      },
      {
        onSuccess: (res) => {
          messageService.success(
            <T
              keyName="tm_empty_wizard_copy_success"
              defaultValue="Copied {copied, plural, one {# entry} other {# entries}} ({skipped} skipped)"
              params={{ copied: res.copied, skipped: res.skipped }}
            />
          );
          onFinished();
        },
      }
    );
  };

  return (
    <Dialog
      data-cy="tm-empty-wizard-copy-dialog"
      open={open}
      onClose={onClose}
      maxWidth="sm"
      fullWidth
    >
      <DialogTitle>
        <T
          keyName="tm_empty_wizard_copy_title"
          defaultValue="Copy from a project"
        />
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          <T
            keyName="tm_empty_wizard_copy_description"
            defaultValue="Copy entries from an existing project's translation memory. The original project TM is left untouched."
          />
        </Typography>
        <Autocomplete
          size="small"
          options={options}
          getOptionLabel={(option) => option.name}
          value={selected}
          onChange={(_, newValue) => setSelected(newValue)}
          inputValue={search}
          onInputChange={(_, value) => setSearch(value)}
          loading={projectsLoadable.isFetching}
          isOptionEqualToValue={(option, value) => option.id === value.id}
          noOptionsText={t(
            'tm_empty_wizard_copy_no_matching_projects',
            'No projects in this organization share this TM’s source language.'
          )}
          renderInput={(params) => (
            <TextField
              {...params}
              autoFocus
              data-cy="tm-empty-wizard-copy-project"
              label={
                <T
                  keyName="tm_empty_wizard_copy_project_label"
                  defaultValue="Source project"
                />
              }
              placeholder={t(
                'tm_empty_wizard_copy_project_placeholder',
                'Choose a project…'
              )}
              InputProps={{
                ...params.InputProps,
                endAdornment: (
                  <>
                    {projectsLoadable.isFetching && (
                      <CircularProgress size={16} />
                    )}
                    {params.InputProps.endAdornment}
                  </>
                ),
              }}
            />
          )}
        />
        <Typography
          variant="caption"
          color="text.secondary"
          sx={{ mt: 1, display: 'block' }}
        >
          <T
            keyName="tm_empty_wizard_copy_hint"
            defaultValue="All entries from the project's TM will be copied into this memory."
          />
        </Typography>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('global_cancel_button')}</Button>
        <LoadingButton
          color="primary"
          variant="contained"
          loading={copyMutation.isLoading}
          disabled={!selected}
          onClick={handleConfirm}
          data-cy="tm-empty-wizard-copy-submit"
        >
          <T
            keyName="tm_empty_wizard_copy_submit"
            defaultValue="Copy entries"
          />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
