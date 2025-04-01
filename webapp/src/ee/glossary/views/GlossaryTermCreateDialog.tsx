import { Button, Dialog, DialogTitle, styled } from '@mui/material';
import { T, useTranslate } from '@tolgee/react';
import { Formik } from 'formik';

import { Validation } from 'tg.constants/GlobalValidationSchema';
import { useApiMutation } from 'tg.service/http/useQueryApi';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { messageService } from 'tg.service/MessageService';
import { GlossaryTermCreateForm } from 'tg.ee.module/glossary/views/GlossaryTermCreateFormDialog';
import { components } from 'tg.service/apiSchema.generated';

type CreateGlossaryTermRequest =
  components['schemas']['CreateGlossaryTermRequest'];

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
  glossaryId: number;
};

export const GlossaryTermCreateDialog = ({
  open,
  onClose,
  onFinished,
  organizationId,
  glossaryId,
}: Props) => {
  const { t } = useTranslate();

  const createGlossaryTerm = useApiMutation({
    url: '/v2/organizations/{organizationId}/glossaries/{id}/terms',
    method: 'post',
    invalidatePrefix: '/v2/organizations/{organizationId}/glossaries/{id}',
  });

  const initialValues: CreateGlossaryTermRequest = {
    text: '',
    description: undefined,
    flagNonTranslatable: false,
    flagCaseSensitive: false,
    flagAbbreviation: false,
    flagForbiddenTerm: false,
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm">
      <DialogTitle>
        <T keyName="glossary_term_create_title" />
      </DialogTitle>

      <Formik
        initialValues={initialValues}
        validationSchema={Validation.GLOSSARY_TERM_CREATE_FORM(t)}
        onSubmit={async (values: CreateGlossaryTermRequest) => {
          createGlossaryTerm.mutate(
            {
              path: {
                organizationId,
                id: glossaryId,
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
        }}
      >
        {({ submitForm }) => (
          <StyledContainer>
            <GlossaryTermCreateForm />
            <StyledActions>
              <Button onClick={onClose}>{t('global_cancel_button')}</Button>
              <LoadingButton
                onClick={submitForm}
                color="primary"
                variant="contained"
                loading={createGlossaryTerm.isLoading}
                data-cy="create-glossary-term-submit"
              >
                {t('glossary_term_create_submit_button')}
              </LoadingButton>
            </StyledActions>
          </StyledContainer>
        )}
      </Formik>
    </Dialog>
  );
};
