import React from 'react';
import { Switch, Box, Tooltip } from '@mui/material';
import { useFormikContext } from 'formik';
import { T, useTranslate } from '@tolgee/react';
import { styled } from '@mui/material';

const StyledContainer = styled(Box)`
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
`;

const Label = styled('div')`
  font-size: 14px;
`;

type Props = {
  className?: string;
};

export const PreserveFormatSpecifiersSelector: React.FC<Props> = ({ className }) => {
  const { t } = useTranslate();
  const formik = useFormikContext<{ preserveFormatSpecifiers?: boolean }>();
  
  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    formik.setFieldValue('preserveFormatSpecifiers', e.target.checked);
  };

  return (
    <StyledContainer className={className}>
      <Tooltip
        title={t('export_preserve_format_specifiers_description')}
      >
        <Label>
          <T keyName="export_preserve_format_specifiers_label" />
        </Label>
      </Tooltip>
      <Switch
        checked={formik.values.preserveFormatSpecifiers || false}
        onChange={handleChange}
        color="primary"
        name="preserveFormatSpecifiers"
      />
    </StyledContainer>
  );
};