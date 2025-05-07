import { VFC } from 'react';
import { TextField } from 'tg.component/common/form/fields/TextField';
import Box from '@mui/material/Box';
import { useTranslate } from '@tolgee/react';
import { AssignedProjectsSelect } from 'tg.ee.module/glossary/components/AssignedProjectsSelect';
import { GlossaryBaseLanguageSelect } from 'tg.ee.module/glossary/components/GlossaryBaseLanguageSelect';

type Props = {
  disabled?: boolean;
  organizationId: number;
  withAssignedProjects?: boolean;
};

export const GlossaryCreateForm: VFC<Props> = ({
  disabled,
  organizationId,
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
      <GlossaryBaseLanguageSelect
        name="baseLanguage"
        organizationId={organizationId}
        disabled={disabled}
      />
      {withAssignedProjects && (
        <AssignedProjectsSelect
          name="assignedProjects"
          organizationId={organizationId}
          disabled={disabled}
        />
      )}
    </Box>
  );
};
