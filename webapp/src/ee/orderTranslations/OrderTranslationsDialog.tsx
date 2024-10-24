import {
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogTitle,
  FormControlLabel,
  FormGroup,
  Step,
  StepContent,
  StepLabel,
  Stepper,
  styled,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';
import { useState } from 'react';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import {
  useApiMutation,
  useApiQuery,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { FiltersType } from 'tg.component/translation/translationFilters/tools';
import { User } from 'tg.component/UserAccount';
import { TaskCreateForm } from 'tg.ee/task/components/taskCreate/TaskCreateForm';
import { TranslationStateType } from 'tg.ee/task/components/taskCreate/TranslationStateFilter';
import { TranslationAgency } from './TranslationAgency';
import { BoxLoading } from 'tg.component/common/BoxLoading';

type CreateTaskRequest = components['schemas']['CreateTaskRequest'];
type TaskType = CreateTaskRequest['type'];
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
  width: min(calc(100vw - 64px), 1000px);
`;

const StyledActions = styled('div')`
  display: flex;
  gap: 8px;
  padding-top: 24px;
  justify-content: end;
`;

const StyledStepLabel = styled(StepLabel)`
  &.clickable {
    cursor: pointer;
  }
`;

const StyledStepContent = styled(Box)`
  display: grid;
  padding-top: ${({ theme }) => theme.spacing(1)};
  gap: ${({ theme }) => theme.spacing(0.5, 3)};
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

export const OrderTranslationsDialog = ({
  open,
  onClose,
  onFinished,
  projectId,
  allLanguages,
  initialValues,
}: Props) => {
  const { t } = useTranslate();

  const createTasksLoadable = useApiMutation({
    url: '/v2/projects/{projectId}/tasks/create-translation-order',
    method: 'post',
    invalidatePrefix: ['/v2/projects/{projectId}/tasks', '/v2/user-tasks'],
  });

  const [filters, setFilters] = useState<FiltersType>({});
  const [stateFilters, setStateFilters] = useState<TranslationStateType[]>([]);
  const [languages, setLanguages] = useState(initialValues?.languages ?? []);
  const [step, setStep] = useState(0);

  const agenciesLoadable = useBillingApiQuery({
    url: '/v2/billing/translation-agency',
    method: 'get',
    query: {
      size: 1000,
    },
  });

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

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xl">
      <StyledMainTitle>
        <T keyName="order_translations_title" />
      </StyledMainTitle>
      <StyledSubtitle>
        <T
          keyName="order_translations_keys_subtitle"
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
          agreeSharing: true,
          agreeInvite: true,
          agencyId: undefined as number | undefined,
        }}
        validationSchema={Validation.CREATE_TASK_FORM(t)}
        onSubmit={async (values) => {
          const data = languages.map(
            (languageId) =>
              ({
                type: values.type,
                name: values.name,
                description: values.description,
                languageId: languageId,
                dueDate: values.dueDate,
                assignees: values.assignees[languageId]?.map((u) => u.id) ?? [],
                keys: selectedKeys,
              } satisfies CreateTaskRequest)
          );
          createTasksLoadable.mutate(
            {
              path: { projectId },
              query: {
                filterState: stateFilters.filter((i) => i !== 'OUTDATED'),
                filterOutdated: stateFilters.includes('OUTDATED'),
              },
              content: {
                'application/json': { tasks: data, agencyId: values.agencyId! },
              },
            },
            {
              onSuccess() {
                messageService.success(
                  <T
                    keyName="order_translation_success_message"
                    params={{ count: languages.length }}
                  />
                );
                onFinished();
              },
            }
          );
        }}
      >
        {({ submitForm, values, setFieldValue }) => {
          const selectedAgency =
            agenciesLoadable.data?._embedded?.translationAgencies?.find(
              (a) => a.id === values.agencyId
            );
          return (
            <StyledContainer>
              <Stepper orientation="vertical" activeStep={step}>
                <Step key={1}>
                  <StyledStepLabel
                    className="clickable"
                    onClick={() => setStep(0)}
                    role="button"
                  >
                    <Box display="flex" alignItems="center" gap={1}>
                      <Box>
                        <T keyName="order_translation_choose_translation_agency_title" />
                      </Box>
                      {selectedAgency &&
                        (selectedAgency.avatar ? (
                          <img
                            src={selectedAgency.avatar.thumbnail}
                            height={22}
                            alt={selectedAgency.name}
                          />
                        ) : (
                          <Box>{`(${selectedAgency.name})`}</Box>
                        ))}
                    </Box>
                  </StyledStepLabel>
                  <StepContent>
                    {agenciesLoadable.isLoading ? (
                      <BoxLoading />
                    ) : (
                      <Box display="grid" gap="20px">
                        {agenciesLoadable.data?._embedded?.translationAgencies?.map(
                          (agency, i) => (
                            <TranslationAgency
                              key={i}
                              agency={agency}
                              selected={values.agencyId === agency.id}
                              onSelect={(id) => setFieldValue('agencyId', id)}
                            />
                          )
                        )}
                      </Box>
                    )}
                  </StepContent>
                </Step>

                <Step key={2}>
                  <StepLabel
                    className="clickable"
                    onClick={() => setStep(0)}
                    role="button"
                  >
                    <T keyName="order_translation_create_task_title" />
                  </StepLabel>
                  <StepContent>
                    <StyledStepContent>
                      <TaskCreateForm
                        selectedKeys={selectedKeys}
                        languages={languages}
                        setLanguages={setLanguages}
                        allLanguages={allLanguages}
                        filters={filters}
                        setFilters={
                          initialValues?.selection ? setFilters : undefined
                        }
                        stateFilters={stateFilters}
                        setStateFilters={setStateFilters}
                        projectId={projectId}
                        hideDueDate
                        hideAssignees
                      />

                      <FormGroup
                        sx={{
                          mt: 2,
                          ml: 2,
                          display: 'grid',
                          justifyItems: 'start',
                        }}
                      >
                        <FormControlLabel
                          label={t('order_translation_sharing_details_label')}
                          checked={values.agreeSharing}
                          onChange={() =>
                            setFieldValue('agreeSharing', !values.agreeSharing)
                          }
                          control={<Checkbox />}
                        />
                        <FormControlLabel
                          label={t('order_translation_invite_to_project_label')}
                          checked={values.agreeInvite}
                          onChange={() =>
                            setFieldValue('agreeInvite', !values.agreeInvite)
                          }
                          control={<Checkbox />}
                        />
                      </FormGroup>
                    </StyledStepContent>
                  </StepContent>
                </Step>
              </Stepper>
              <StyledActions>
                <Button onClick={onClose}>{t('global_cancel_button')}</Button>
                {step !== 1 ? (
                  <Button
                    onClick={() => setStep(1)}
                    color="primary"
                    variant="contained"
                    disabled={values.agencyId === undefined}
                    data-cy="order-translation-next"
                  >
                    {t('order_translation_next_button')}
                  </Button>
                ) : (
                  <LoadingButton
                    disabled={!languages.length}
                    onClick={submitForm}
                    color="primary"
                    variant="contained"
                    loading={createTasksLoadable.isLoading}
                    data-cy="order-translation-submit"
                  >
                    {t('order_translation_submit_button')}
                  </LoadingButton>
                )}
              </StyledActions>
            </StyledContainer>
          );
        }}
      </Formik>
    </Dialog>
  );
};
