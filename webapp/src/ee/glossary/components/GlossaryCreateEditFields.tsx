import { VFC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import Box from '@mui/material/Box';
import { T, useTranslate } from '@tolgee/react';
import { AssignedProjectsSelect } from 'tg.ee.module/glossary/components/AssignedProjectsSelect';
import { GlossaryBaseLanguageSelect } from 'tg.ee.module/glossary/components/GlossaryBaseLanguageSelect';
import { Alert } from '@mui/material';

type Props = {
  disabled?: boolean;
  withAssignedProjects?: boolean;
};

export const GlossaryCreateEditFields: VFC<Props> = ({
  disabled,
  withAssignedProjects = false,
}) => {
  const { t } = useTranslate();

  return (
    <Box display="grid">
      <TextField
        name="name"
        autoFocus
        label={t('create_glossary_field_name')}
        placeholder={t('glossary_default_name')}
        data-cy="create-glossary-field-name"
        disabled={disabled}
      />
      {withAssignedProjects && (
        <AssignedProjectsSelect name="assignedProjects" disabled={disabled} />
      )}
      <GlossaryBaseLanguageSelect
        name="baseLanguage"
        assignedProjectsName={
          withAssignedProjects ? 'assignedProjects' : undefined
        }
        disabled={disabled}
      />
      <Alert severity="info">
        <Box>
          <T keyName="create_edit_glossary_languages_note" />
        </Box>
      </Alert>
    </Box>
  );
};
