import React, { FC } from 'react';
import { Box } from '@mui/material';
import LoadingButton from 'tg.component/common/form/LoadingButton';
import { useTranslate } from '@tolgee/react';

type CloudPlanSaveButtonProps = {
  loading: boolean | undefined;
};

export const PlanSaveButton: FC<CloudPlanSaveButtonProps> = ({ loading }) => {
  const { t } = useTranslate();

  return (
    <>
      <Box display="flex" justifyContent="end" mt={4}>
        <LoadingButton
          loading={loading}
          variant="contained"
          color="primary"
          type="submit"
          data-cy="form-submit-button"
        >
          {t('global_form_save')}
        </LoadingButton>
      </Box>
    </>
  );
};
