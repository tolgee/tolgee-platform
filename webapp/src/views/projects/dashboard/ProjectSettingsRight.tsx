import { Settings01 } from '@untitled-ui/icons-react';
import { IconButton, Tooltip, styled } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { Link } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { components } from 'tg.service/apiSchema.generated';

type ProjectModel = components['schemas']['ProjectModel'];

const StyledContainer = styled('div')`
  display: flex;
  gap: 12px;
  align-items: baseline;
`;

type Props = {
  project: ProjectModel;
};

export const ProjectSettingsRight = ({ project }: Props) => {
  const { t } = useTranslate();
  return (
    <StyledContainer>
      <Tooltip title={t('project_dashboard_settings_link')} disableInteractive>
        <IconButton
          component={Link}
          to={LINKS.PROJECT_EDIT.build({ [PARAMS.PROJECT_ID]: project.id })}
        >
          <Settings01 />
        </IconButton>
      </Tooltip>
    </StyledContainer>
  );
};
