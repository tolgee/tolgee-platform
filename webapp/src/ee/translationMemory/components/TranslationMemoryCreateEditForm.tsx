import {
  Autocomplete,
  Box,
  Button,
  FormControlLabel,
  Stack,
  Switch,
  TextField as MuiTextField,
  Typography,
  styled,
} from '@mui/material';
import { Formik, useFormikContext } from 'formik';
import { useMemo, useState } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { LabelHint } from 'tg.component/common/LabelHint';
import {
  useEnabledFeatures,
  usePreferredOrganization,
} from 'tg.globalContext/helpers';
import { T, useTranslate } from '@tolgee/react';
import { BaseLanguageSelect } from 'tg.component/languages/BaseLanguageSelect';
import { useApiInfiniteQuery } from 'tg.service/http/useQueryApi';
import { useDebounce } from 'use-debounce';
import * as Yup from 'yup';
import {
  AssignedProjectRow,
  PendingRemovalRow,
  TmAssignedProjectsTable,
} from 'tg.ee.module/translationMemory/views/TmAssignedProjectsTable';
import { confirmation } from 'tg.hooks/confirmation';

const StyledContainer = styled('div')`
  padding: ${({ theme }) => theme.spacing(3)};
  padding-top: ${({ theme }) => theme.spacing(1)};
  width: min(calc(100vw - 64px), 600px);
`;

const StyledActions = styled('div')`
  display: flex;
  gap: 8px;
  padding-top: 24px;
  justify-content: end;
`;

export type CreateEditTranslationMemoryFormValues = {
  name: string;
  baseLanguage:
    | {
        tag: string;
      }
    | undefined;
  defaultPenalty: number;
  writeOnlyReviewed: boolean;
  assignedProjects: AssignedProjectRow[];
};

const validationSchema = Yup.object().shape({
  name: Yup.string().min(1).required(),
  baseLanguage: Yup.object()
    .required()
    .shape({
      tag: Yup.string().min(1).required(),
    }),
  defaultPenalty: Yup.number().min(0).max(100).required(),
  writeOnlyReviewed: Yup.boolean().required(),
});

type Mode = 'create' | 'edit';

type Props = {
  mode: Mode;
  /**
   * In edit mode, identifies the kind of TM being edited. Drives whether the
   * "only accept reviewed translations" switch is editable (PROJECT) or locked
   * with a tooltip (SHARED — stored entries seeded under one rule, can't change).
   * Ignored in create mode (the value is the create-time choice).
   */
  tmType?: 'PROJECT' | 'SHARED';
  initialValues?: CreateEditTranslationMemoryFormValues;
  onClose: () => void;
  onSave: (
    values: CreateEditTranslationMemoryFormValues,
    pendingRemovals: PendingRemovalRow[]
  ) => void;
  isSaving: boolean;
};

export const TranslationMemoryCreateEditForm = ({
  mode,
  tmType,
  initialValues,
  onClose,
  onSave,
  isSaving,
}: Props) => {
  const { t } = useTranslate();

  const { isEnabled } = useEnabledFeatures();
  const featureEnabled = isEnabled('TRANSLATION_MEMORY');

  // IDs that were already saved on the server when the form opened. Used in edit mode to
  // distinguish a "remove" of a saved assignment (needs server-side DELETE on save) from
  // removal of a row the user just added in this session (just drop from local state).
  const originalProjectIds = useMemo(
    () =>
      new Set(initialValues?.assignedProjects.map((a) => a.projectId) ?? []),
    [initialValues]
  );

  const [pendingRemovals, setPendingRemovals] = useState<PendingRemovalRow[]>(
    []
  );

  if (initialValues === undefined) {
    return null;
  }

  return (
    <Formik
      initialValues={initialValues}
      enableReinitialize
      validationSchema={validationSchema}
      onSubmit={(values) => onSave(values, pendingRemovals)}
    >
      {({ submitForm, values, setFieldValue }) => {
        const requestRemoveProject = (
          projectId: number,
          projectName: string
        ) => {
          if (mode === 'edit' && originalProjectIds.has(projectId)) {
            // Saved assignment → confirm before queuing a server-side DELETE on save
            confirmation({
              title: (
                <T
                  keyName="tm_settings_disconnect_project_title"
                  defaultValue="Disconnect {projectName}"
                  params={{ projectName }}
                />
              ),
              message: (
                <T
                  keyName="tm_settings_remove_project_message"
                  defaultValue="This project will be disconnected from the translation memory."
                />
              ),
              onConfirm: () => {
                setPendingRemovals((prev) => [
                  ...prev,
                  { projectId, projectName },
                ]);
                setFieldValue(
                  'assignedProjects',
                  values.assignedProjects.filter(
                    (a) => a.projectId !== projectId
                  )
                );
              },
            });
            return;
          }
          // Newly added in this session → just drop from values
          setFieldValue(
            'assignedProjects',
            values.assignedProjects.filter((a) => a.projectId !== projectId)
          );
        };

        const undoRemoval = (projectId: number) => {
          const removal = pendingRemovals.find(
            (r) => r.projectId === projectId
          );
          if (!removal) return;
          setPendingRemovals((prev) =>
            prev.filter((r) => r.projectId !== projectId)
          );
          setFieldValue('assignedProjects', [
            ...values.assignedProjects,
            {
              projectId: removal.projectId,
              projectName: removal.projectName,
              readAccess: true,
              writeAccess: true,
              penalty: null,
            },
          ]);
        };

        return (
          <StyledContainer>
            <Stack spacing={3}>
              <TextField
                name="name"
                autoFocus
                label={t('translation_memory_field_name', 'Name')}
                data-cy="create-translation-memory-field-name"
                disabled={!featureEnabled}
                minHeight={false}
              />
              <BaseLanguageFieldWithHint disabled={!featureEnabled} />
              <DefaultPenaltyField disabled={!featureEnabled} />
              <WriteOnlyReviewedField
                disabled={!featureEnabled}
                mode={mode}
                tmType={tmType}
              />
              <AssignedProjectsEditor
                disabled={!featureEnabled}
                pendingRemovals={pendingRemovals}
                onRequestRemove={requestRemoveProject}
                onUndoRemove={undoRemoval}
              />
            </Stack>
            <StyledActions>
              <Button
                onClick={onClose}
                data-cy="create-edit-translation-memory-cancel"
              >
                {t('global_cancel_button')}
              </Button>
              <LoadingButton
                disabled={!featureEnabled}
                onClick={submitForm}
                color="primary"
                variant="contained"
                loading={isSaving}
                data-cy="create-edit-translation-memory-submit"
              >
                {mode === 'edit'
                  ? t('global_form_save')
                  : t('global_form_create')}
              </LoadingButton>
            </StyledActions>
          </StyledContainer>
        );
      }}
    </Formik>
  );
};

const BaseLanguageFieldWithHint = ({ disabled }: { disabled: boolean }) => {
  const { t } = useTranslate();
  const { values } = useFormikContext<CreateEditTranslationMemoryFormValues>();
  const hasAssignedProjects = values.assignedProjects.length > 0;
  return (
    <BaseLanguageSelect
      name="baseLanguage"
      disabled={disabled || hasAssignedProjects}
      minHeight={false}
      label={
        <LabelHint
          title={
            hasAssignedProjects ? (
              <T
                keyName="translation_memory_settings_base_language_locked_hint"
                defaultValue="Base language is locked while projects are assigned. Remove all projects first to change it."
              />
            ) : (
              <T
                keyName="translation_memory_settings_base_language_hint"
                defaultValue="Must be the same across all projects using this TM."
              />
            )
          }
        >
          {t('field_base_language', 'Base language')}
        </LabelHint>
      }
    />
  );
};

const DefaultPenaltyField = ({ disabled }: { disabled: boolean }) => {
  const { t } = useTranslate();
  return (
    <TextField
      name="defaultPenalty"
      label={
        <LabelHint
          title={
            <T
              keyName="translation_memory_settings_default_penalty_hint"
              defaultValue="Lowers match score by this many points. Applied to every project using this TM unless overridden below."
            />
          }
        >
          {t('translation_memory_settings_default_penalty', 'Default penalty')}
        </LabelHint>
      }
      size="small"
      disabled={disabled}
      sx={{ width: 200 }}
      minHeight={false}
      inputProps={{
        inputMode: 'numeric',
        'data-cy': 'tm-settings-default-penalty',
      }}
      InputProps={{
        endAdornment: (
          <Typography variant="body2" color="text.secondary">
            %
          </Typography>
        ),
      }}
    />
  );
};

const WriteOnlyReviewedField = ({
  disabled,
  mode,
  tmType,
}: {
  disabled: boolean;
  mode: Mode;
  tmType?: 'PROJECT' | 'SHARED';
}) => {
  const { values, setFieldValue } =
    useFormikContext<CreateEditTranslationMemoryFormValues>();

  // Locked when editing a SHARED TM — flipping post-creation would leave the stored entries
  // inconsistent (some seeded under "all states", some under "reviewed only"). PROJECT TMs
  // are virtual-only so they can be edited freely; create mode is the moment of choice and
  // is always editable.
  const locked = mode === 'edit' && tmType === 'SHARED';
  const isProjectTmEdit = mode === 'edit' && tmType === 'PROJECT';

  // Static-key branches — Tolgee's `tolgee extract` step rejects keys derived from a runtime
  // expression. Two distinct UX flavours (project-edit vs create/shared-edit), each spelt out
  // explicitly so the extractor can pick them up.
  const label = isProjectTmEdit ? (
    <LabelHint
      title={
        <T
          keyName="project_tm_only_include_reviewed_hint"
          defaultValue="Only translations in the Reviewed state are offered as TM suggestions. Other translations are excluded until they are reviewed."
        />
      }
    >
      <T
        keyName="project_tm_only_include_reviewed_label"
        defaultValue="Only include reviewed translations"
      />
    </LabelHint>
  ) : locked ? (
    <LabelHint
      title={
        <T
          keyName="translation_memory_settings_write_only_reviewed_locked_tooltip"
          defaultValue="This setting is fixed at creation time. Create a new shared TM if you need different behaviour."
        />
      }
    >
      <T
        keyName="translation_memory_settings_write_only_reviewed"
        defaultValue="Only accept reviewed translations"
      />
    </LabelHint>
  ) : (
    <LabelHint
      title={
        <T
          keyName="translation_memory_settings_write_only_reviewed_hint"
          defaultValue="Translations flow in only when marked Reviewed. If a reviewed translation is later un-reviewed, its entry is removed. TMX import and direct edits in this TM bypass the filter."
        />
      }
    >
      <T
        keyName="translation_memory_settings_write_only_reviewed"
        defaultValue="Only accept reviewed translations"
      />
    </LabelHint>
  );

  return (
    <FormControlLabel
      control={
        <Switch
          checked={values.writeOnlyReviewed}
          onChange={(_, v) => setFieldValue('writeOnlyReviewed', v)}
          disabled={disabled || locked}
          data-cy="tm-settings-write-only-reviewed"
        />
      }
      label={label}
    />
  );
};

type AssignedProjectsEditorProps = {
  disabled: boolean;
  pendingRemovals: PendingRemovalRow[];
  onRequestRemove: (projectId: number, projectName: string) => void;
  onUndoRemove: (projectId: number) => void;
};

const AssignedProjectsEditor = ({
  disabled,
  pendingRemovals,
  onRequestRemove,
  onUndoRemove,
}: AssignedProjectsEditorProps) => {
  const { t } = useTranslate();
  const { values, setFieldValue } =
    useFormikContext<CreateEditTranslationMemoryFormValues>();
  const { preferredOrganization } = usePreferredOrganization();
  const organizationId = preferredOrganization!.id;

  const [projectSearch, setProjectSearch] = useState('');
  const [projectSearchDebounced] = useDebounce(projectSearch, 300);

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
        {t('translation_memory_settings_used_in_projects', 'Used in projects')}
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
      </Box>
    </Box>
  );
};
