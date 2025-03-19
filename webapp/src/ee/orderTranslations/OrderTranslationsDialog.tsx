import {
  Alert,
  Box,
  Button,
  Checkbox,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  Step,
  StepContent,
  StepLabel,
  Stepper,
  styled,
  Typography,
  useTheme,
} from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';
import { useState } from 'react';
import { CheckCircle } from '@untitled-ui/icons-react';
import clsx from 'clsx';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import {
  useApiQuery,
  useBillingApiMutation,
  useBillingApiQuery,
} from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { FiltersInternal } from 'tg.views/projects/translations/TranslationFilters/tools';
import { User } from 'tg.component/UserAccount';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { useEnabledFeatures, useUser } from 'tg.globalContext/helpers';
import { TranslationAgency } from './TranslationAgency';
import { TranslationStateType } from 'tg.translationTools/useStateTranslation';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import {
  DEFAULT_STATE_FILTERS_REVIEW,
  DEFAULT_STATE_FILTERS_TRANSLATE,
  TaskCreateForm,
} from 'tg.ee.module/task/components/taskCreate/TaskCreateForm';
import { EmptyScopeDialog } from 'tg.ee.module/task/components/taskCreate/EmptyScopeDialog';
import { useTranslationFilters } from 'tg.views/projects/translations/TranslationFilters/useTranslationFilters';

type CreateTaskRequest = components['schemas']['CreateTaskRequest'];
type TaskType = CreateTaskRequest['type'];
type LanguageModel = components['schemas']['LanguageModel'];
type KeysScopeView = components['schemas']['KeysScopeView'];

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
  width: min(calc(100vw - 100px), 1000px);
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

export const OrderTranslationsDialog: React.FC<Props> = ({
  open,
  onClose,
  onFinished,
  projectId,
  allLanguages,
  initialValues,
}) => {
  const theme = useTheme();
  const { isEnabled } = useEnabledFeatures();
  const taskFeature = isEnabled('ORDER_TRANSLATION');
  const disabled = !taskFeature;
  const { t } = useTranslate();
  const user = useUser();

  const createTasksLoadable = useBillingApiMutation({
    url: '/v2/projects/{projectId}/billing/order-translation',
    method: 'post',
    invalidatePrefix: ['/v2/projects/{projectId}/tasks', '/v2/user-tasks'],
  });

  const [filters, setFilters] = useState<FiltersInternal>({});
  const { filtersQuery, ...actions } = useTranslationFilters({
    filters,
    setFilters,
  });
  const [_stateFilters, setStateFilters] = useState<TranslationStateType[]>();
  const [languages, setLanguages] = useState(initialValues?.languages ?? []);
  const [successMessage, setSuccessMessage] = useState(false);

  const [_step, setStep] = useState<number | undefined>(undefined);

  const step = disabled ? -1 : _step;

  const preferredAgencyLoadable = useBillingApiQuery({
    url: '/v2/projects/{projectId}/billing/order-translation/preferred-agency',
    method: 'get',
    path: {
      projectId,
    },
    options: {
      onSuccess(data) {
        setStep((step) => (data.preferredAgencyId !== null ? 1 : 0));
      },
    },
  });

  const agencyAlreadyContacted =
    typeof preferredAgencyLoadable.data?.preferredAgencyId === 'number';

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
      ...filtersQuery,
      languages: allLanguages.map((l) => l.tag),
    },
    options: {
      enabled: !initialValues?.selection,
    },
  });

  const selectedKeys =
    initialValues?.selection ?? selectedLoadable.data?.ids ?? [];

  const [scope, setScope] = useState<(KeysScopeView | undefined)[]>([]);
  const [emptyScope, setEmptyScope] = useState<LanguageModel | true>();

  const canBeSubmitted = scope.every(Boolean);

  function getStateFilters(taskType: TaskType) {
    if (_stateFilters) {
      return _stateFilters;
    }
    return taskType === 'TRANSLATE'
      ? DEFAULT_STATE_FILTERS_TRANSLATE
      : DEFAULT_STATE_FILTERS_REVIEW;
  }

  const isLoading =
    preferredAgencyLoadable.isLoading ||
    agenciesLoadable.isLoading ||
    step === undefined;

  function handleFinish() {
    setSuccessMessage(false);
    onFinished();
  }

  if (successMessage) {
    return (
      <Dialog open={true} onClose={handleFinish}>
        <DialogTitle data-cy="order-translation-confirmation">
          <T
            keyName="order_translation_success_message"
            params={{ count: languages.length }}
          />
        </DialogTitle>
        <DialogContent>
          <Alert severity="success" icon={<CheckCircle />}>
            <T keyName="order_translation_success_description" />
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button
            color="primary"
            variant="contained"
            onClick={handleFinish}
            data-cy="order-translation-confirmation-ok"
          >
            <T keyName="order_translation_success_ok_button" />
          </Button>
        </DialogActions>
      </Dialog>
    );
  }

  return (
    <Dialog open={open} onClose={onClose} maxWidth="xl">
      {!taskFeature && (
        <DisabledFeatureBanner
          customMessage={t('order_translation_feature_description')}
        />
      )}
      <StyledMainTitle>
        <T keyName="order_translations_title" />
      </StyledMainTitle>
      <StyledSubtitle>
        <T
          keyName="order_translations_keys_subtitle"
          params={{ value: selectedKeys.length }}
        />
      </StyledSubtitle>

      {isLoading ? (
        <BoxLoading />
      ) : (
        <Formik
          initialValues={{
            type: initialValues?.type ?? 'TRANSLATE',
            name: initialValues?.name ?? '',
            description: initialValues?.description ?? '',
            dueDate: initialValues?.dueDate ?? undefined,
            assignees: initialValues?.languageAssignees ?? {},
            agreeSharing: true,
            agreeInvite: agencyAlreadyContacted ? false : true,
            agencyId:
              preferredAgencyLoadable.data?.preferredAgencyId ?? undefined,
          }}
          validationSchema={Validation.CREATE_TASK_FORM(t)}
          onSubmit={async (values) => {
            const emptyScope = scope.findIndex((sc) => !sc?.keyCount);
            if (emptyScope != -1) {
              const language = allLanguages.find(
                (l) => l.id === languages[emptyScope]
              );
              setEmptyScope(language || true);
              return;
            }
            const data = languages.map(
              (languageId) =>
                ({
                  type: values.type,
                  name: values.name || undefined,
                  description: values.description,
                  languageId: languageId,
                  dueDate: values.dueDate,
                  assignees:
                    values.assignees[languageId]?.map((u) => u.id) ?? [],
                  keys: selectedKeys,
                } satisfies CreateTaskRequest)
            );
            const stateFilters = getStateFilters(values.type);
            createTasksLoadable.mutate(
              {
                path: { projectId },
                query: {
                  filterState: stateFilters.filter(
                    (i) => i !== 'OUTDATED' && i !== 'AUTO_TRANSLATED'
                  ),
                  filterOutdated: stateFilters.includes('OUTDATED'),
                },
                content: {
                  'application/json': {
                    tasks: data,
                    agencyId: values.agencyId!,
                    sendReadOnlyInvitation: values.agreeInvite,
                  },
                },
              },
              {
                onSuccess() {
                  setSuccessMessage(true);
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
                      className={clsx({ clickable: !disabled })}
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
                        <Box display="grid">
                          <FormControlLabel
                            data-cy="order-translation-sharing-details-consent-checkbox"
                            disabled={disabled}
                            label={t('order_translation_sharing_details_label')}
                            checked={values.agreeSharing}
                            onChange={() =>
                              setFieldValue(
                                'agreeSharing',
                                !values.agreeSharing
                              )
                            }
                            control={<Checkbox />}
                          />
                          <Typography variant="body2">
                            <T
                              keyName="order_translation_description"
                              params={{ email: user?.username }}
                            />
                          </Typography>
                        </Box>
                      </Box>
                    </StepContent>
                  </Step>

                  <Step key={2}>
                    <StepLabel role="button">
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
                          filterActions={actions}
                          stateFilters={getStateFilters(values.type)}
                          setStateFilters={setStateFilters}
                          projectId={projectId}
                          hideDueDate
                          hideAssignees
                          disabled={disabled}
                          onScopeChange={setScope}
                        />

                        <Box display="grid" mt={2}>
                          <FormControlLabel
                            sx={{ justifySelf: 'start' }}
                            disabled={disabled}
                            label={t(
                              'order_translation_invite_to_project_label',
                              { viewPermissionLabel: t('permission_type_view') }
                            )}
                            data-cy="order-translation-invitation-checkbox"
                            checked={values.agreeInvite}
                            onChange={() =>
                              setFieldValue('agreeInvite', !values.agreeInvite)
                            }
                            control={<Checkbox />}
                          />
                          <Box
                            mt={-1}
                            ml={4}
                            color={theme.palette.text.secondary}
                            fontSize={14}
                          >
                            {values.agreeInvite ? (
                              <T
                                keyName="order_translation_invite_to_project_detail"
                                params={{
                                  menuItem: t('project_menu_members'),
                                }}
                              />
                            ) : (
                              <>
                                <T
                                  keyName="order_translation_invite_to_project_statistics_info"
                                  params={{
                                    menuItem: t('project_menu_members'),
                                  }}
                                />{' '}
                                {!agencyAlreadyContacted && (
                                  <T
                                    keyName="order_translation_invite_to_project_warning"
                                    params={{
                                      menuItem: t('project_menu_members'),
                                    }}
                                  />
                                )}
                              </>
                            )}
                          </Box>
                        </Box>
                      </StyledStepContent>
                    </StepContent>
                  </Step>
                </Stepper>
                <StyledActions>
                  <Button onClick={onClose}>{t('global_cancel_button')}</Button>
                  {step === 0 ? (
                    <Button
                      onClick={() => setStep(1)}
                      color="primary"
                      variant="contained"
                      disabled={
                        values.agencyId === undefined || !values.agreeSharing
                      }
                      data-cy="order-translation-next"
                    >
                      {t('order_translation_next_button')}
                    </Button>
                  ) : (
                    <LoadingButton
                      disabled={
                        !languages.length || disabled || !canBeSubmitted
                      }
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
      )}
      {emptyScope && (
        <EmptyScopeDialog
          language={emptyScope}
          onClose={() => setEmptyScope(undefined)}
        />
      )}
    </Dialog>
  );
};
