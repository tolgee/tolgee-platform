import { Button, styled } from '@mui/material';
import Box from '@mui/material/Box';
import { Formik } from 'formik';
import { Validation } from 'tg.constants/GlobalValidationSchema';
import { GlossaryCreateEditFields } from 'tg.ee.module/glossary/components/GlossaryCreateEditFields';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { SpinnerProgress } from 'tg.component/SpinnerProgress';
import { useEnabledFeatures } from 'tg.globalContext/helpers';
import { useTranslate } from '@tolgee/react';

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

export type CreateEditGlossaryFormValues = {
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

type Props = {
  initialValues?: CreateEditGlossaryFormValues;
  onClose: () => void;
  onSave: (values: CreateEditGlossaryFormValues) => void;
  isSaving: boolean;
  isEditing?: boolean;
};

export const GlossaryCreateEditForm = ({
  initialValues,
  onClose,
  onSave,
  isSaving,
  isEditing,
}: Props) => {
  const { t } = useTranslate();

  const { isEnabled } = useEnabledFeatures();
  const glossaryFeatureEnabled = isEnabled('GLOSSARY');

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
      enableReinitialize
      validationSchema={Validation.GLOSSARY_CREATE_FORM(t)}
      onSubmit={onSave}
    >
      {({ submitForm, values }) => {
        return (
          <StyledContainer>
            <GlossaryCreateEditFields
              disabled={!glossaryFeatureEnabled}
              withAssignedProjects
            />
            <StyledActions>
              <Button onClick={onClose} data-cy="create-edit-glossary-cancel">
                {t('global_cancel_button')}
              </Button>
              <LoadingButton
                disabled={!glossaryFeatureEnabled}
                onClick={submitForm}
                color="primary"
                variant="contained"
                loading={isSaving}
                data-cy="create-edit-glossary-submit"
              >
                {isEditing
                  ? t('edit_glossary_submit_button')
                  : t('create_glossary_submit_button')}
              </LoadingButton>
            </StyledActions>
          </StyledContainer>
        );
      }}
    </Formik>
  );
};
