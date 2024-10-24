import { Button, Dialog, DialogTitle, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';
import { useState } from 'react';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { FiltersType } from 'tg.component/translation/translationFilters/tools';
import { User } from 'tg.component/UserAccount';

import { TranslationStateType } from './TranslationStateFilter';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { PaidFeatureBanner } from 'tg.ee/common/PaidFeatureBanner';
import { TaskCreateForm } from './TaskCreateForm';

type TaskType = components['schemas']['TaskModel']['type'];
type LanguageModel = components['schemas']['LanguageModel'];

const StyledMainTitle = styled(DialogTitle)`
  padding-bottom: 0px;
`;

const StyledSubtitle = styled('div')`
  padding: ${({ theme }) => theme.spacing(0, 3, 2, 3)};
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledContainer = styled('div')`
  display: grid;
  padding: ${({ theme }) => theme.spacing(3)};
  gap: ${({ theme }) => theme.spacing(0.5, 3)};
  padding-top: ${({ theme }) => theme.spacing(1)};
  width: min(calc(100vw - 64px), 800px);
`;

const StyledActions = styled('div')`
  display: flex;
  gap: 8px;
  padding-top: 24px;
  justify-content: end;
`;

export type InitialValues = {
  type: TaskType;
  name: string;
  description: string;
  languages: number[];
  dueDate: number;
  languageAssignees: Record<number, User[]>;
  selection: number[];
};

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  projectId: number;
  allLanguages: LanguageModel[];
  initialValues?: Partial<InitialValues>;
};

export const TaskCreateDialog = ({
  open,
  onClose,
  onFinished,
  projectId,
  allLanguages,
  initialValues,
}: Props) => {
  const { t } = useTranslate();

  const createTasksLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/create-multiple-tasks',
    method: 'post',
    invalidatePrefix: ['/v2/projects/{projectId}/tasks', '/v2/user-tasks'],
  });

  const [filters, setFilters] = useState<FiltersType>({});
  const [stateFilters, setStateFilters] = useState<TranslationStateType[]>([]);
  const [languages, setLanguages] = useState(initialValues?.languages ?? []);
  const { features } = useEnabledFeatures();

  const taskFeature = features.includes('TASKS');

  const selectedLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/translations/select-all',
    method: 'get',
    path: { projectId },
    query: {
      ...filters,
      languages: allLanguages.map((l) => l.tag),
    },
    options: {
      enabled: !initialValues?.selection,
    },
  });

  const selectedKeys =
    initialValues?.selection ?? selectedLoadable.data?.ids ?? [];

  const disabled = !taskFeature;

  return (
    <Dialog open={open} onClose={onClose} maxWidth="lg">
      {!taskFeature && (
        <PaidFeatureBanner customMessage={t('tasks_feature_description')} />
      )}
      <StyledMainTitle>
        <T keyName="batch_operation_create_task_title" />
      </StyledMainTitle>
      <StyledSubtitle>
        <T
          keyName="batch_operation_create_task_keys_subtitle"
          params={{ value: selectedKeys.length }}
        />
      </StyledSubtitle>

      <Formik
        initialValues={{
          type: initialValues?.type ?? 'TRANSLATE',
          name: initialValues?.name ?? '',
          description: initialValues?.description ?? '',
          dueDate: initialValues?.dueDate ?? undefined,
          assignees: initialValues?.languageAssignees ?? {},
        }}
        validationSchema={Validation.CREATE_TASK_FORM(t)}
        onSubmit={async (values) => {
          const data = languages.map((languageId) => ({
            type: values.type,
            name: values.name,
            description: values.description,
            languageId: languageId,
            dueDate: values.dueDate,
            assignees: values.assignees[languageId]?.map((u) => u.id) ?? [],
            keys: selectedKeys,
          }));
          createTasksLoadable.mutate(
            {
              path: { projectId },
              query: {
                filterState: stateFilters.filter((i) => i !== 'OUTDATED'),
                filterOutdated: stateFilters.includes('OUTDATED'),
              },
              content: {
                'application/json': { tasks: data },
              },
            },
            {
              onSuccess() {
                messageService.success(
                  <T
                    keyName="create_task_success_message"
                    params={{ count: languages.length }}
                  />
                );
                onFinished();
              },
            }
          );
        }}
      >
        {({ submitForm }) => {
          return (
            <StyledContainer>
              <TaskCreateForm
                selectedKeys={selectedKeys}
                languages={languages}
                setLanguages={setLanguages}
                allLanguages={allLanguages}
                filters={filters}
                setFilters={initialValues?.selection ? setFilters : undefined}
                stateFilters={stateFilters}
                setStateFilters={setStateFilters}
                projectId={projectId}
                disabled={disabled}
              />
              <StyledActions>
                <Button onClick={onClose}>{t('global_cancel_button')}</Button>
                <LoadingButton
                  disabled={!languages.length || !taskFeature}
                  onClick={submitForm}
                  color="primary"
                  variant="contained"
                  loading={createTasksLoadable.isLoading}
                  data-cy="create-task-submit"
                >
                  {t('create_task_submit_button')}
                </LoadingButton>
              </StyledActions>
            </StyledContainer>
          );
        }}
      </Formik>
    </Dialog>
  );
};
