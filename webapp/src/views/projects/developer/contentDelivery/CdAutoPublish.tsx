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
  height: 17px;
  width: 17px;
`;

type Props = {
  className?: string;
};

export const CdAutoPublish: React.FC<Props> = ({ className }) => {
  const { t } = useTranslate();

  return (
    <Field name="autoPublish">
      {({ field }) => {
        return (
          <FormControlLabel
            className={className}
            label={
              <StyledLabel>
                <div>{t('export_translations_auto_publish_label')}</div>
                <Tooltip title={t('export_translations_auto_publish_hint')}>
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
                  checked={field.value}
                  data-cy="content-delivery-auto-publish-checkbox"
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
