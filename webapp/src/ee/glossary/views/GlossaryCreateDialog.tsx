import { Button, Dialog, DialogTitle, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { GlossaryCreateForm } from 'tg.ee.module/glossary/views/GlossaryCreateForm';

type CreateGlossaryRequest = components['schemas']['CreateGlossaryRequest'];

// const StyledSubtitle = styled('div')`
//   padding: ${({ theme }) => theme.spacing(0, 3, 2, 3)};
//   color: ${({ theme }) => theme.palette.text.secondary};
// `;

const StyledContainer = styled('div')`
  display: grid;
  padding: ${({ theme }) => theme.spacing(3)};
  gap: ${({ theme }) => theme.spacing(0.5, 3)};
  padding-top: ${({ theme }) => theme.spacing(1)};
  width: min(calc(100vw - 64px), 600px);
`;

const StyledActions = styled('div')`
  display: flex;
  gap: 8px;
  padding-top: 24px;
  justify-content: end;
`;

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  organizationId: number;
};

export const GlossaryCreateDialog = ({
  open,
  onClose,
  onFinished,
  organizationId,
}: Props) => {
  const { t } = useTranslate();

  const { isEnabled } = useEnabledFeatures();
  const glossaryFeature = isEnabled('GLOSSARY');

  // const availableLanguages = useApiQuery({
  //   url: '/v2/organizations/{organizationId}/all-languages',
  //   method: 'get',
  //   path: { organizationId: organization!.id },
  // });

  const createGlossary = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/glossaries',
  });

  const canBeSubmitted = true; // scope.every(Boolean);

  const initialValues: CreateGlossaryRequest = {
    name: '',
    baseLanguageCode: 'en',
    assignedProjects: [],
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm">
      {!glossaryFeature && (
        <DisabledFeatureBanner
          customMessage={t('glossaries_feature_description')}
        />
      )}
      <DialogTitle>
        <T keyName="glossary_create_title" />
      </DialogTitle>

      <Formik
        initialValues={initialValues}
        validationSchema={Validation.GLOSSARY_CREATE_FORM(t)}
        onSubmit={async (values) => {
          // const data = languages.map((languageId) => ({
          //   type: values.type,
          //   name: values.name || undefined,
          //   description: values.description,
          //   languageId: languageId,
          //   dueDate: values.dueDate,
          //   assignees: values.assignees[languageId]?.map((u) => u.id) ?? [],
          //   keys: selectedKeys,
          // }));
          //
          // const stateFilters = getStateFilters(values.type);
          //
          // createTasksLoadable.mutate(
          //   {
          //     path: { projectId },
          //     query: {
          //       filterState: stateFilters.filter(
          //         (i) => i !== 'OUTDATED'
          //       ) as StateType[],
          //       filterOutdated: stateFilters.includes('OUTDATED'),
          //     },
          //     content: {
          //       'application/json': { tasks: data },
          //     },
          //   },
          //   {
          //     onSuccess() {
          //       messageService.success(
          //         <T
          //           keyName="create_task_success_message"
          //           params={{ count: languages.length }}
          //         />
          //       );
          //       onFinished();
          //     },
          //   }
          // );
        }}
      >
        {({ submitForm, values }) => {
          return (
            <StyledContainer>
              <GlossaryCreateForm disabled={!glossaryFeature} />
              <StyledActions>
                <Button onClick={onClose}>{t('global_cancel_button')}</Button>
                <LoadingButton
                  disabled={!glossaryFeature || !canBeSubmitted}
                  onClick={submitForm}
                  color="primary"
                  variant="contained"
                  loading={createGlossary.isLoading}
                  data-cy="create-glossary-submit"
                >
                  {t('create_glossary_submit_button')}
                </LoadingButton>
              </StyledActions>
            </StyledContainer>
          );
        }}
      </Formik>
    </Dialog>
  );
};
