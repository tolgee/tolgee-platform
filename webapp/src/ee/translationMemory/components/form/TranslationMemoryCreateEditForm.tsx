import { Button, Stack, styled } from '@mui/material';
import { Formik } from 'formik';
import { useMemo, useState } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { useTranslate } from '@tolgee/react';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import {
  AssignedProjectRow,
  PendingRemovalRow,
} from 'tg.ee.module/translationMemory/components/form/TmAssignedProjectsTable';
import { AssignedProjectsEditor } from 'tg.ee.module/translationMemory/components/form/AssignedProjectsEditor';
import { confirmProjectDisconnect } from 'tg.ee.module/translationMemory/components/form/confirmProjectDisconnect';
import { BaseLanguageFieldWithHint } from 'tg.ee.module/translationMemory/components/form/fields/BaseLanguageFieldWithHint';
import { DefaultPenaltyField } from 'tg.ee.module/translationMemory/components/form/fields/DefaultPenaltyField';
import { WriteOnlyReviewedField } from 'tg.ee.module/translationMemory/components/form/fields/WriteOnlyReviewedField';

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

export type Mode = 'create' | 'edit';

type Props = {
  mode: Mode;
  initialValues?: CreateEditTranslationMemoryFormValues;
  onClose: () => void;
  onSave: (
    values: CreateEditTranslationMemoryFormValues,
    pendingRemovals: PendingRemovalRow[]
  ) => void;
  isSaving: boolean;
  /** When true, only the assigned-projects section + actions render. Used by the wizard. */
  projectsOnly?: boolean;
};

export const TranslationMemoryCreateEditForm = ({
  mode,
  initialValues,
  onClose,
  onSave,
  isSaving,
  projectsOnly,
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
      validationSchema={Validation.TRANSLATION_MEMORY_CREATE_EDIT}
      onSubmit={(values) => onSave(values, pendingRemovals)}
    >
      {({ submitForm, values, setFieldValue }) => {
        const requestRemoveProject = (
          projectId: number,
          projectName: string
        ) => {
          if (mode === 'edit' && originalProjectIds.has(projectId)) {
            // Saved assignment → confirm before queuing a server-side DELETE on save
            confirmProjectDisconnect(projectName, () => {
              setPendingRemovals((prev) => [
                ...prev,
                { projectId, projectName },
              ]);
              setFieldValue(
                'assignedProjects',
                values.assignedProjects.filter((a) => a.projectId !== projectId)
              );
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
              {!projectsOnly && (
                <>
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
                  <WriteOnlyReviewedField disabled={!featureEnabled} />
                </>
              )}
              <AssignedProjectsEditor
                disabled={!featureEnabled}
                mode={mode}
                projectsOnly={projectsOnly}
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
