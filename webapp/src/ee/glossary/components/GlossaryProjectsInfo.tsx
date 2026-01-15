import { Box, styled, Tooltip, Typography } from '@mui/material';
import { useTranslate } from '@tolgee/react';
import { components } from 'tg.service/apiSchema.generated';
import { ProjectLink } from 'tg.component/ProjectLink';

type SimpleProjectModel = components['schemas']['SimpleProjectModel'];

const StyledWrapper = styled(Box)`
  display: flex;
  align-items: center;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  projects: SimpleProjectModel[];
  maxDisplay?: number;
};

export const GlossaryProjectsInfo: React.FC<Props> = ({
  projects,
  maxDisplay = 2,
}) => {
  const { t } = useTranslate();

  if (projects.length === 0) {
    return null;
  }

  const sortedProjects = projects.sort((a, b) => a.name.localeCompare(b.name));
  const displayedProjects = sortedProjects.slice(0, maxDisplay);
  const remainingProjects = sortedProjects.slice(maxDisplay);
  const hasMore = remainingProjects.length > 0;

  const content = (
    <StyledWrapper data-cy="glossary-projects-info">
      <Typography variant="body2" component="span">
        ({t('glossary_projects_used_in_label')}{' '}
        {displayedProjects.map((project, index) => (
          <span key={project.id}>
            {index > 0 && ', '}
            <ProjectLink project={project} />
          </span>
        ))}
        {hasMore && ', ...'})
      </Typography>
    </StyledWrapper>
  );

  if (hasMore) {
    return (
      <Tooltip
        placement="bottom-end"
        title={
          <Typography variant="body2" component="span">
            {remainingProjects.map((project) => (
              <div key={project.id}>
                <ProjectLink project={project} />
              </div>
            ))}
          </Typography>
        }
      >
        {content}
      </Tooltip>
    );
  }

  return content;
};
