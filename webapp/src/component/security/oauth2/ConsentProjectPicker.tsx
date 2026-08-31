import React from 'react';
import { T, useTranslate } from '@tolgee/react';
import {
  Box,
  FormControlLabel,
  Radio,
  RadioGroup,
  styled,
} from '@mui/material';

import { ProjectSearchSelect } from 'tg.component/projectSearchSelect/ProjectSearchSelect';
import { Project } from 'tg.component/projectSearchSelect/types';
import { ProjectChoice } from './consentProjectChoice';

const StyledSelect = styled(ProjectSearchSelect)`
  margin-left: ${({ theme }) => theme.spacing(4)};
`;

type Props = {
  value: ProjectChoice;
  onChange: (value: ProjectChoice) => void;
};

export const ConsentProjectPicker: React.FC<Props> = ({ value, onChange }) => {
  const { t } = useTranslate();

  const handleProjectChange = (projects: Project[]) =>
    onChange({ kind: 'one', project: projects[0] ?? null });

  return (
    <Box data-cy="oauth2-consent-project">
      <RadioGroup
        value={value.kind === 'unset' ? '' : value.kind}
        onChange={(e) =>
          onChange(
            e.target.value === 'all'
              ? { kind: 'all' }
              : { kind: 'one', project: null }
          )
        }
      >
        <FormControlLabel
          value="one"
          control={<Radio data-cy="oauth2-consent-project-one" />}
          label={
            <T
              keyName="oauth2_consent_single_project_option"
              defaultValue="A single project"
            />
          }
        />
        {value.kind === 'one' && (
          <StyledSelect
            single
            label={t('oauth2_consent_project_label', 'Project')}
            value={value.project ? [value.project] : []}
            onChange={handleProjectChange}
          />
        )}
        <FormControlLabel
          value="all"
          control={<Radio data-cy="oauth2-consent-project-all" />}
          label={
            <T
              keyName="oauth2_consent_all_projects_option"
              defaultValue="All projects"
            />
          }
        />
      </RadioGroup>
    </Box>
  );
};
