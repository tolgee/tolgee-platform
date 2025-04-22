import { Button, Dialog, DialogTitle, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useApiMutation, useApiQuery } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { messageService } from 'tg.service/MessageService';
import { GlossaryTermCreateUpdateForm } from 'tg.ee.module/glossary/views/GlossaryTermCreateUpdateForm';
import { components } from 'tg.service/apiSchema.generated';
import { useEffect, useRef, useState } from 'react';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import Box from '@mui/material/Box';

type CreateGlossaryTermWithTranslationRequest =
  components['schemas']['CreateGlossaryTermWithTranslationRequest'];
type UpdateGlossaryTermWithTranslationRequest =
  components['schemas']['UpdateGlossaryTermWithTranslationRequest'];
type CreateOrUpdateGlossaryTermRequest =
  CreateGlossaryTermWithTranslationRequest &
    UpdateGlossaryTermWithTranslationRequest;

type GlossaryTermModel = Omit<components['schemas']['GlossaryTermModel'], 'id'>;
type GlossaryTermTranslationModel = Omit<
  components['schemas']['GlossaryTermTranslationModel'],
  'languageTag'
>;

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

type Props = {
  open: boolean;
  onClose: () => void;
  onFinished: () => void;
  organizationId: number;
  glossaryId: number;
  /** When undefined - create new Term; Otherwise edit existing Term
   * Immutable prop!
   * Don't switch between editing and creating
   * mode after initialization
   */
  editTermId?: number;
};

const translationInitialValues: GlossaryTermTranslationModel = {
  text: '',
};

const termInitialValues: GlossaryTermModel = {
  description: undefined,
  flagNonTranslatable: false,
  flagCaseSensitive: false,
  flagAbbreviation: false,
  flagForbiddenTerm: false,
};

export const GlossaryTermCreateUpdateDialog = ({
  open,
  onClose,
  onFinished,
  organizationId,
  glossaryId,
  editTermId,
}: Props) => {
  const initialTermId = useRef(editTermId).current;

  useEffect(() => {
    if (editTermId !== initialTermId) {
      // eslint-disable-next-line no-console
      console.warn('Changing `editTermId` after mount is not supported.');
    }
  }, [editTermId]);

  const { t } = useTranslate();

  const [initialTranslationValues, setInitialTranslationValues] = useState(
    initialTermId === undefined ? translationInitialValues : undefined
  );

  const [initialTermValues, setInitialTermValues] = useState(
    initialTermId === undefined ? termInitialValues : undefined
  );

  const initialValues: CreateOrUpdateGlossaryTermRequest | undefined =
    initialTranslationValues &&
      initialTermValues && {
        text: initialTranslationValues.text ?? '',
        description: initialTermValues.description,
        flagNonTranslatable: initialTermValues.flagNonTranslatable,
        flagCaseSensitive: initialTermValues.flagCaseSensitive,
        flagAbbreviation: initialTermValues.flagAbbreviation,
        flagForbiddenTerm: initialTermValues.flagForbiddenTerm,
      };

  const [saveIsLoading, save] = (
    initialTermId === undefined
      ? () => {
          const mutation = useApiMutation({
            url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms',
            method: 'post',
            invalidatePrefix:
              '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
          });
          const save = async (values: CreateOrUpdateGlossaryTermRequest) => {
            mutation.mutate(
              {
                path: {
                  organizationId,
                  glossaryId,
                },
                content: {
                  'application/json': values,
                },
              },
              {
                onSuccess() {
                  messageService.success(
                    <T keyName="glossary_term_create_success_message" />
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
            url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/{termId}',
            method: 'put',
            invalidatePrefix:
              '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
          });
          const save = async (values: CreateOrUpdateGlossaryTermRequest) => {
            const triggerSave = async () => {
              mutation.mutate(
                {
                  path: {
                    organizationId,
                    glossaryId,
                    termId: initialTermId,
                  },
                  content: {
                    'application/json': values,
                  },
                },
                {
                  onSuccess() {
                    messageService.success(
                      <T keyName="glossary_term_edit_success_message" />
                    );
                    onFinished();
                  },
                }
              );
            };

            if (
              initialTermValues?.flagNonTranslatable === false &&
              values.flagNonTranslatable
            ) {
              // User is changing term to non-translatable - we will delete all translations of this term
              // TODO: confirmation dialog - all term translations will be deleted
              return;
            }

            triggerSave();
          };
          return [mutation.isLoading, save] as const;
        }
  )();

  const glossaryQuery = useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}',
    method: 'get',
    path: {
      organizationId,
      glossaryId,
    },
    options: {
      enabled: initialTermId !== undefined,
      onError(e) {
        onClose();
      },
    },
  });

  useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/{termId}',
    method: 'get',
    path: {
      organizationId,
      glossaryId,
      termId: initialTermId ?? -1,
    },
    options: {
      enabled: initialTermId !== undefined,
      onSuccess(data) {
        setInitialTermValues?.(data);
      },
      onError(e) {
        onClose();
      },
    },
  });

  useApiQuery({
    url: '/v2/organizations/{organizationId}/glossaries/{glossaryId}/terms/{termId}/translations/{languageTag}',
    method: 'get',
    path: {
      organizationId,
      glossaryId,
      termId: initialTermId ?? -1,
      languageTag: glossaryQuery.data?.baseLanguageTag ?? '',
    },
    options: {
      enabled:
        initialTermId !== undefined &&
        glossaryQuery.data?.baseLanguageTag !== undefined,
      onSuccess(data) {
        setInitialTranslationValues?.(data);
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
        validationSchema={Validation.GLOSSARY_TERM_CREATE_FORM(t)}
        onSubmit={save}
      >
        {({ submitForm }) => (
          <StyledContainer>
            <GlossaryTermCreateUpdateForm />
            <StyledActions>
              <Button onClick={onClose}>{t('global_cancel_button')}</Button>
              <LoadingButton
                onClick={submitForm}
                color="primary"
                variant="contained"
                loading={saveIsLoading}
                data-cy="create-glossary-term-submit"
              >
                {t('glossary_term_create_submit_button')}
              </LoadingButton>
            </StyledActions>
          </StyledContainer>
        )}
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
      fullWidth
      onClick={(e) => e.stopPropagation()}
    >
      <DialogTitle>
        {initialTermId === undefined ? (
          <T keyName="glossary_term_create_title" />
        ) : (
          <T keyName="glossary_term_edit_title" />
        )}
      </DialogTitle>

      {form}
    </Dialog>
  );
};
