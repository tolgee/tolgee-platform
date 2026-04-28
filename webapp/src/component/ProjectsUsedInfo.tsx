import { Box, styled, Tooltip, Typography } from '@mui/material';
import { ProjectLink } from 'tg.component/ProjectLink';
import { useMemo } from 'react';

type ProjectRef = { id: number; name: string };

const StyledWrapper = styled(Box)`
  display: flex;
  align-items: center;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  projects: ProjectRef[];
  label: React.ReactNode;
  maxDisplay?: number;
  'data-cy'?: string;
};

export const ProjectsUsedInfo: React.FC<Props> = ({
  projects,
  label,
  maxDisplay = 2,
  'data-cy': dataCy,
}) => {
  const sortedProjects = useMemo(
    () => [...projects].sort((a, b) => a.name.localeCompare(b.name)),
    [projects]
  );
  const displayedProjects = sortedProjects.slice(0, maxDisplay);
  const remainingProjects = sortedProjects.slice(maxDisplay);
  const hasMore = remainingProjects.length > 0;

  if (displayedProjects.length === 0) {
    return null;
  }

  const content = (
    <StyledWrapper data-cy={dataCy}>
      <Typography variant="body2" component="span">
        ({label}{' '}
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
