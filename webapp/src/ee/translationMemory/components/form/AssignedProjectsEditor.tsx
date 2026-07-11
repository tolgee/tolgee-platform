import {
  Alert,
  Autocomplete,
  Box,
  TextField as MuiTextField,
  Typography,
} from '@mui/material';
import { useFormikContext } from 'formik';
import { useMemo, useState } from 'react';
import { T, useTranslate } from '@tolgee/react';
import { useDebounce } from 'use-debounce';
import { usePreferredOrganization } from 'tg.globalContext/helpers';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import {
  PendingRemovalRow,
  TmAssignedProjectsTable,
} from 'tg.ee.module/translationMemory/components/form/TmAssignedProjectsTable';
import {
  CreateEditTranslationMemoryFormValues,
  Mode,
} from 'tg.ee.module/translationMemory/components/form/TranslationMemoryCreateEditForm';

const PROJECT_SEARCH_DEBOUNCE_MS = 300;

type Props = {
  disabled: boolean;
  mode: Mode;
  projectsOnly?: boolean;
  pendingRemovals: PendingRemovalRow[];
  onRequestRemove: (projectId: number, projectName: string) => void;
  onUndoRemove: (projectId: number) => void;
};

export const AssignedProjectsEditor = ({
  disabled,
  mode,
  projectsOnly,
  pendingRemovals,
  onRequestRemove,
  onUndoRemove,
}: Props) => {
  const { t } = useTranslate();
  const { values, setFieldValue } =
    useFormikContext<CreateEditTranslationMemoryFormValues>();
  const { preferredOrganization } = usePreferredOrganization();
  const organizationId = preferredOrganization!.id;

  const [projectSearch, setProjectSearch] = useState('');
  const [projectSearchDebounced] = useDebounce(
    projectSearch,
    PROJECT_SEARCH_DEBOUNCE_MS
  );

  const projectsLoadable = useApiInfiniteQuery({
    url: '/v2/organizations/{id}/projects',
    method: 'get',
    path: { id: organizationId },
    query: { search: projectSearchDebounced, size: 30 },
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
              search: projectSearchDebounced,
              size: 30,
              page: lastPage.page!.number! + 1,
            },
          };
        }
        return null;
      },
    },
  });

  const allOrgProjects =
    projectsLoadable.data?.pages.flatMap((p) => p._embedded?.projects ?? []) ??
    [];

  const baseLanguageTag = values.baseLanguage?.tag;
  const availableProjects = useMemo(() => {
    const assignedIds = new Set(
      values.assignedProjects.map((a) => a.projectId)
    );
    const removedIds = new Set(pendingRemovals.map((r) => r.projectId));
    return allOrgProjects.filter((p) => {
      if (assignedIds.has(p.id) || removedIds.has(p.id)) return false;
      // Only projects sharing the TM's base language can be assigned — the backend
      // rejects mismatched assignments. Hide them from the picker so the user does
      // not pick something that will fail at submit.
      if (baseLanguageTag && p.baseLanguage?.tag !== baseLanguageTag)
        return false;
      return true;
    });
  }, [
    values.assignedProjects,
    pendingRemovals,
    allOrgProjects,
    baseLanguageTag,
  ]);

  const addProject = (project: { id: number; name: string }) => {
    if (values.assignedProjects.some((a) => a.projectId === project.id)) return;
    setFieldValue('assignedProjects', [
      ...values.assignedProjects,
      {
        projectId: project.id,
        projectName: project.name,
        readAccess: true,
        writeAccess: true,
        penalty: null,
      },
    ]);
  };

  const toggleAccess = (
    projectId: number,
    field: 'readAccess' | 'writeAccess'
  ) => {
    setFieldValue(
      'assignedProjects',
      values.assignedProjects.map((a) =>
        a.projectId === projectId ? { ...a, [field]: !a[field] } : a
      )
    );
  };

  const updatePenalty = (projectId: number, value: number | null) => {
    setFieldValue(
      'assignedProjects',
      values.assignedProjects.map((a) =>
        a.projectId === projectId ? { ...a, penalty: value } : a
      )
    );
  };

  // Warn when projects are assigned but none has writeAccess — that TM has no virtual
  // content. Surfaced in create mode and in the wizard's "Manage projects" modal; hidden
  // in the full TM-settings dialog where users may have intentionally configured read-only
  // sharing alongside other knobs.
  const showNoWriteAlert =
    (mode === 'create' || projectsOnly) &&
    values.assignedProjects.length > 0 &&
    !values.assignedProjects.some((a) => a.writeAccess);

  return (
    <Box>
      <Typography
        variant="caption"
        color="text.secondary"
        sx={{
          textTransform: 'uppercase',
          letterSpacing: '0.04em',
          display: 'block',
          mb: 0.5,
        }}
      >
        {t('translation_memory_settings_shared_with', 'Shared with')}
      </Typography>
      <TmAssignedProjectsTable
        rows={values.assignedProjects}
        removedRows={pendingRemovals}
        defaultPenalty={values.defaultPenalty}
        onTogglePenalty={updatePenalty}
        onToggleAccess={toggleAccess}
        onRemove={onRequestRemove}
        onUndoRemove={onUndoRemove}
      />
      <Box mt={1}>
        <Autocomplete
          size="small"
          disabled={disabled}
          options={availableProjects}
          getOptionLabel={(option) => option.name}
          value={null}
          onChange={(_, newValue) => {
            if (newValue) {
              addProject({ id: newValue.id, name: newValue.name });
            }
          }}
          inputValue={projectSearch}
          onInputChange={(_, value) => setProjectSearch(value)}
          renderOption={(props, option) => (
            <li {...props} key={option.id} data-cy="tm-add-project-option">
              {option.name}
            </li>
          )}
          renderInput={(params) => (
            <MuiTextField
              {...params}
              placeholder={t(
                'tm_settings_add_project_placeholder',
                'Add project...'
              )}
              data-cy="tm-add-project-autocomplete"
            />
          )}
          noOptionsText={
            baseLanguageTag
              ? t(
                  'tm_settings_no_projects_for_base_language',
                  'No projects with this base language'
                )
              : t('tm_settings_no_projects_available', 'No available projects')
          }
          blurOnSelect
          clearOnBlur
        />
        {showNoWriteAlert && (
          <Alert
            severity="info"
            sx={{ mt: 1 }}
            data-cy="tm-no-write-access-notice"
          >
            <T
              keyName="translation_memory_no_write_access_notice"
              defaultValue="No project writes to this TM. Add or import entries after creation."
            />
          </Alert>
        )}
      </Box>
    </Box>
  );
};
