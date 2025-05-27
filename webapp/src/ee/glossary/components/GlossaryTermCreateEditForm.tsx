import { components } from 'tg.service/apiSchema.generated';
import { Button, styled } from '@mui/material';
import Box from '@mui/material/Box';
import { Formik } from 'formik';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { GlossaryTermCreateEditFields } from 'tg.ee.module/glossary/components/GlossaryTermCreateEditFields';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import React from 'react';
import { useTranslate } from '@tolgee/react';

type CreateGlossaryTermWithTranslationRequest =
  components['schemas']['CreateGlossaryTermWithTranslationRequest'];
type UpdateGlossaryTermWithTranslationRequest =
  components['schemas']['UpdateGlossaryTermWithTranslationRequest'];
export type CreateOrUpdateGlossaryTermRequest =
  CreateGlossaryTermWithTranslationRequest &
    UpdateGlossaryTermWithTranslationRequest;

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
  initialValues?: CreateOrUpdateGlossaryTermRequest;
  onClose: () => void;
  onSave: (values: CreateOrUpdateGlossaryTermRequest) => void;
  onDelete?: () => void;
  isSaving: boolean;
};

export const GlossaryTermCreateEditForm = ({
  initialValues,
  onClose,
  onSave,
  onDelete,
  isSaving,
}: Props) => {
  const { t } = useTranslate();

  if (initialValues === undefined) {
    return (
      <StyledLoading>
        <SpinnerProgress />
      </StyledLoading>
    );
  }

  return (
    <Formik
      initialValues={initialValues}
      validationSchema={Validation.GLOSSARY_TERM_CREATE_FORM(t)}
      onSubmit={onSave}
    >
      {({ submitForm }) => (
        <StyledContainer>
          <GlossaryTermCreateEditFields />
          <StyledActions>
            {onDelete && (
              <>
                <Button variant="outlined" onClick={onDelete}>
                  {t('global_delete_button')}
                </Button>
                <Box flexGrow={1} />
              </>
            )}
            <Button onClick={onClose} data-cy="create-glossary-term-cancel">
              {t('global_cancel_button')}
            </Button>
            <LoadingButton
              onClick={submitForm}
              color="primary"
              variant="contained"
              loading={isSaving}
              data-cy="create-glossary-term-submit"
            >
              {t('glossary_term_create_submit_button')}
            </LoadingButton>
          </StyledActions>
        </StyledContainer>
      )}
    </Formik>
  );
};
