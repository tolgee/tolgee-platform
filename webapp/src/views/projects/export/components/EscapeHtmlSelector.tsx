import { Field } from 'formik';
import { useTranslate } from '@tolgee/react';
import {
  Box,
  Checkbox,
  FormControlLabel,
  Tooltip,
  styled,
} from '@mui/material';
import { HelpCircle } from '@untitled-ui/icons-react';

const StyledLabel = styled('div')`
  display: flex;
  gap: 5px;
  align-items: center;
`;

const StyledHelpIcon = styled(HelpCircle)`
  width: 17px;
  height: 17px;
`;

type Props = {
  className?: string;
};

export const EscapeHtmlSelector: React.FC<Props> = ({ className }) => {
  const { t } = useTranslate();

  return (
    <Field name="escapeHtml">
      {({ field }) => {
        return (
          <FormControlLabel
            className={className}
            label={
              <StyledLabel>
                <div>{t('export_translations_escape_html_label')}</div>
                <Tooltip title={t('export_translations_escape_html_hint')}>
                  <Box display="flex">
                    <StyledHelpIcon />
                  </Box>
                </Tooltip>
              </StyledLabel>
            }
            control={
              <>
                <Checkbox
                  {...field}
                  checked={field.value || false}
                  data-cy="export-escape_html-selector"
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
