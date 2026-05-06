import React, { useState } from 'react';
import {
  Box,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
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
import { InfiniteSearchSelect } from 'tg.component/searchSelect/InfiniteSearchSelect';
import { SelectItem } from 'tg.component/searchSelect/SelectItem';
import { components } from 'tg.service/apiSchema.generated';

const PROJECT_SEARCH_DEBOUNCE_MS = 500;

type ProjectModel = components['schemas']['ProjectModel'];
type SelectedProjectModel = {
  id: number;
  name: string;
};

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  organizationId: number;
  translationMemoryId: number;
  sourceLanguageTag: string;
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
  const [searchDebounced] = useDebounce(search, PROJECT_SEARCH_DEBOUNCE_MS);
  const [selected, setSelected] = useState<SelectedProjectModel | undefined>(
    undefined
  );

  // Filter projects server-side to those whose base language matches the TM. The backend
  // rejects mismatched copies, so showing them in the picker would be a guaranteed-failure UX.
  const query = {
    search: searchDebounced,
    size: 30,
    filterBaseLanguageTag: sourceLanguageTag,
  };
  const dataLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{id}/projects',
    method: 'get',
    path: { id: organizationId },
    query,
    options: {
      keepPreviousData: true,
      refetchOnMount: true,
      noGlobalLoading: true,
      getNextPageParam: (lastPage) => {
        if (
          lastPage.page &&
          lastPage.page.number! < lastPage.page.totalPages! - 1
        ) {
          return {
            path: { id: organizationId },
            query: {
              ...query,
              page: lastPage.page!.number! + 1,
            },
          };
        }
        return null;
      },
    },
  });

  const data = dataLoadable.data?.pages.flatMap(
    (p) => p._embedded?.projects ?? []
  );

  const handleFetchMore = () => {
    if (dataLoadable.hasNextPage && !dataLoadable.isFetching) {
      dataLoadable.fetchNextPage();
    }
  };

  const setSelectedProject = (item: ProjectModel) => {
    setSelected({ id: item.id, name: item.name });
  };

  function renderItem(props: object, item: ProjectModel) {
    const isSelected = selected?.id === item.id;
    return (
      <SelectItem
        {...props}
        data-cy="tm-empty-wizard-copy-project-item"
        selected={isSelected}
        label={item.name}
        onClick={() => setSelectedProject(item)}
      />
    );
  }

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
        <Box data-cy="tm-empty-wizard-copy-project">
          <InfiniteSearchSelect
            items={data}
            selected={selected}
            queryResult={dataLoadable}
            itemKey={(item) => item.id}
            search={search}
            onClearSelected={() => setSelected(undefined)}
            onSearchChange={setSearch}
            onFetchMore={handleFetchMore}
            renderItem={renderItem}
            labelItem={(item) => item.name}
            label={
              <T
                keyName="tm_empty_wizard_copy_project_label"
                defaultValue="Source project"
              />
            }
            searchPlaceholder={t(
              'tm_empty_wizard_copy_project_placeholder',
              'Choose a project…'
            )}
          />
        </Box>
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
