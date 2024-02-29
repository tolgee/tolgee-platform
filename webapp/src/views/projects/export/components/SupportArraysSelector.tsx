import { Field } from 'formik';
import { useTranslate } from '@tolgee/react';
import { Checkbox, FormControlLabel, Tooltip, styled } from '@mui/material';
import { Help } from '@mui/icons-material';

const StyledLabel = styled('div')`
  display: flex;
  gap: 5px;
  align-items: center;
`;

const StyledHelpIcon = styled(Help)`
  font-size: 17px;
`;

type Props = {
  className?: string;
};

export const SupportArraysSelector: React.FC<Props> = ({ className }) => {
  const { t } = useTranslate();

  return (
    <Field name="supportArrays">
      {({ field }) => {
        return (
          <FormControlLabel
            className={className}
            label={
              <StyledLabel>
                <div>{t('export_translations_support_arrays_label')}</div>
                <Tooltip title={t('export_translations_support_arrays_hint')}>
                  <StyledHelpIcon />
                </Tooltip>
              </StyledLabel>
            }
            control={
              <>
                <Checkbox
                  {...field}
                  checked={field.value || false}
                  data-cy="export-support_arrays-selector"
                  variant="standard"
                />
              </>
            }
          />
        );
      }}
    </Field>
  );
};
