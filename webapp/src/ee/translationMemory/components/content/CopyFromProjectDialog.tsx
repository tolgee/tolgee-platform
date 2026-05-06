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

  // Connect = create a shared-TM project assignment. read-only by default; users can flip
  // write access in TM Settings later. The TM's defaultPenalty applies (penalty omitted).
  const assignMutation = useApiMutation({
    url: '/v2/projects/{projectId}/translation-memories/{translationMemoryId}',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/translation-memories',
  });

  const handleConfirm = () => {
    if (!selected) return;
    assignMutation.mutate(
      {
        path: { projectId: selected.id, translationMemoryId },
        content: {
          'application/json': {
            readAccess: true,
            writeAccess: false,
          },
        },
      },
      {
        onSuccess: () => {
          messageService.success(
            <T
              keyName="tm_empty_wizard_connect_success"
              defaultValue="Project connected"
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
          keyName="tm_empty_wizard_connect_title"
          defaultValue="Connect a project"
        />
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          <T
            keyName="tm_empty_wizard_connect_description"
            defaultValue="The project's translations will appear in this memory automatically and stay in sync. You can connect more projects or disconnect any time in TM settings."
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
                keyName="tm_empty_wizard_connect_project_label"
                defaultValue="Project"
              />
            }
            searchPlaceholder={t(
              'tm_empty_wizard_connect_project_placeholder',
              'Choose a project…'
            )}
          />
        </Box>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>{t('global_cancel_button')}</Button>
        <LoadingButton
          color="primary"
          variant="contained"
          loading={assignMutation.isLoading}
          disabled={!selected}
          onClick={handleConfirm}
          data-cy="tm-empty-wizard-copy-submit"
        >
          <T
            keyName="tm_empty_wizard_connect_submit"
            defaultValue="Connect"
          />
        </LoadingButton>
      </DialogActions>
    </Dialog>
  );
};
