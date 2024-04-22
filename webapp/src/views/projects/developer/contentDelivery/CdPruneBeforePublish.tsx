import { Field } from 'formik';
import { useTranslate } from '@tolgee/react';
import { Checkbox, FormControlLabel, Tooltip, styled } from '@mui/material';
import { Help } from '@mui/icons-material';
import { FC } from 'react';

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

export const CdPruneBeforePublish: FC<Props> = ({ className }) => {
  const { t } = useTranslate();

  return (
    <Field name="pruneBeforePublish">
      {({ field }) => {
        return (
          <FormControlLabel
            className={className}
            label={
              <StyledLabel>
                <div>
                  {t(
                    'content_delivery_translations_prune_before_publish_label'
                  )}
                </div>
                <Tooltip
                  title={t(
                    'content_delivery_translations_prune_before_publish_hint'
                  )}
                >
                  <StyledHelpIcon />
                </Tooltip>
              </StyledLabel>
            }
            control={
              <>
                <Checkbox
                  {...field}
                  checked={field.value}
                  data-cy="content-delivery-prune-before-publish-checkbox"
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
