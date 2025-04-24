import { Button, Dialog, DialogTitle, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { components } from 'tg.service/apiSchema.generated';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { DisabledFeatureBanner } from 'tg.component/common/DisabledFeatureBanner';
import { GlossaryCreateForm } from 'tg.ee.module/glossary/views/GlossaryCreateForm';
import { messageService } from 'tg.service/MessageService';
import { useEffect, useRef, useState } from 'react';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import Box from '@mui/material/Box';
import { languageInfo } from '@tginternal/language-util/lib/generated/languageInfo';
import { useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';

type CreateGlossaryRequest = components['schemas']['CreateGlossaryRequest'];
type UpdateGlossaryRequest = components['schemas']['UpdateGlossaryRequest'];
type CreateGlossaryForm = {
  name: string;
  baseLanguage:
    | {
        tag: string;
      }
    | undefined;
  assignedProjects: {
    id: number;
  }[];
};

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

const StyledLoading = styled(Box)`
  display: flex;
  justify-content: center;
  align-items: center;
  height: 350px;
`;

const glossaryInitialValues: CreateGlossaryForm = {
  name: '',
  baseLanguage: undefined,
  assignedProjects: [],
};

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  organizationId: number;
  organizationSlug: string;
  editGlossaryId?: number;
};

export const GlossaryCreateEditDialog = ({
  open,
  onClose,
  onFinished,
  organizationId,
  organizationSlug,
  editGlossaryId,
}: Props) => {
  const initialGlossaryId = useRef(editGlossaryId).current;

  useEffect(() => {
    if (editGlossaryId !== initialGlossaryId) {
      // eslint-disable-next-line no-console
      console.warn('Changing `editGlossaryId` after mount is not supported.');
    }
  }, [editGlossaryId]);

  const { t } = useTranslate();
  const history = useHistory();

  const { isEnabled } = useEnabledFeatures();
  const glossaryFeature = isEnabled('GLOSSARY');

  const [saveIsLoading, save] = (
    initialGlossaryId === undefined
      ? () => {
          const mutation = useApiMutation({
            url: '/v2/organizations/{organizationId}/glossaries',
            method: 'post',
            invalidatePrefix: '/v2/organizations/{organizationId}/glossaries',
          });
          const save = async (values: CreateGlossaryForm) => {
            const data: CreateGlossaryRequest = {
              name: values.name,
              baseLanguageTag: values.baseLanguage!.tag,
              assignedProjects: values.assignedProjects?.map(({ id }) => id),
            };

            mutation.mutate(
              {
                path: {
                  organizationId,
                },
                content: {
                  'application/json': data,
                },
              },
              {
                onSuccess({ id }) {
                  messageService.success(
                    <T keyName="glossary_create_success_message" />
                  );
                  history.push(
                    LINKS.ORGANIZATION_GLOSSARY.build({
                      [PARAMS.GLOSSARY_ID]: id,
                      [PARAMS.ORGANIZATION_SLUG]: organizationSlug,
                    })
                  );
                  onFinished();
                },
              }
            );
          };
          return [mutation.isLoading, save] as const;
        }
      : () => {
          const mutation = useApiMutation({
            url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
            method: 'put',
            invalidatePrefix: '/v2/organizations/{organizationId}/glossaries',
          });
          const save = async (values: CreateGlossaryForm) => {
            const data: UpdateGlossaryRequest = {
              name: values.name,
              baseLanguageTag: values.baseLanguage!.tag,
              assignedProjects:
                values.assignedProjects?.map(({ id }) => id) || [],
            };

            mutation.mutate(
              {
                path: {
                  organizationId,
                  glossaryId: initialGlossaryId,
                },
                content: {
                  'application/json': data,
                },
              },
              {
                onSuccess() {
                  messageService.success(
                    <T keyName="glossary_edit_success_message" />
                  );
                  onFinished();
                },
              }
            );
          };
          return [mutation.isLoading, save] as const;
        }
  )();

  const [initialValues, setInitialValues] = useState(
    initialGlossaryId === undefined ? glossaryInitialValues : undefined
  );

  useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
    method: 'get',
    path: {
      organizationId,
      glossaryId: initialGlossaryId ?? -1,
    },
    options: {
      enabled: initialGlossaryId !== undefined,
      onSuccess(data) {
        const language = data.baseLanguageTag
          ? {
              tag: data.baseLanguageTag,
              flagEmoji: languageInfo[data.baseLanguageTag]?.flags?.[0] || '',
              name:
                languageInfo[data.baseLanguageTag]?.englishName ||
                data.baseLanguageTag,
            }
          : undefined;
        setInitialValues?.({
          name: data.name,
          baseLanguage: language,
          assignedProjects:
            data.assignedProjects._embedded?.projects?.map((p) => ({
              id: p.id,
              name: p.name,
            })) || [],
        });
      },
      onError(e) {
        onClose();
      },
    },
  });

  const form =
    initialValues !== undefined ? (
      <Formik
        initialValues={initialValues}
        validationSchema={Validation.GLOSSARY_CREATE_FORM(t)}
        onSubmit={save}
      >
        {({ submitForm, values }) => {
          return (
            <StyledContainer>
              <GlossaryCreateForm
                disabled={!glossaryFeature}
                organizationId={organizationId}
                withAssignedProjects
              />
              <StyledActions>
                <Button onClick={onClose}>{t('global_cancel_button')}</Button>
                <LoadingButton
                  disabled={!glossaryFeature}
                  onClick={submitForm}
                  color="primary"
                  variant="contained"
                  loading={saveIsLoading}
                  data-cy="create-glossary-submit"
                >
                  {initialGlossaryId === undefined
                    ? t('create_glossary_submit_button')
                    : t('edit_glossary_submit_button')}
                </LoadingButton>
              </StyledActions>
            </StyledContainer>
          );
        }}
      </Formik>
    ) : (
      <StyledLoading>
        <SpinnerProgress />
      </StyledLoading>
    );

  return (
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="sm"
      onClick={(e) => e.stopPropagation()}
    >
      {!glossaryFeature && (
        <DisabledFeatureBanner
          customMessage={t('glossaries_feature_description')}
        />
      )}
      <DialogTitle>
        {initialGlossaryId === undefined ? (
          <T keyName="glossary_create_title" />
        ) : (
          <T keyName="glossary_edit_title" />
        )}
      </DialogTitle>

      {form}
    </Dialog>
  );
};
