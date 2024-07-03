import { Edit02 } from '@untitled-ui/icons-react';
import { Box, IconButton, Link as MuiLink, styled } from '@mui/material';
import ReactMarkdown from 'react-markdown';
import { Link } from 'react-router-dom';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';

const StyledContainer = styled('div')`
  display: grid;
  gap: 5px;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  border-radius: 10px;
  position: relative;
`;

const StyledContent = styled('div')`
  margin: 0px 12px;
`;

const StyledTileEdit = styled(Box)`
  position: absolute;
  top: 0px;
  right: 0px;
  padding: 5px;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

type Props = {
  description: string;
};

export const ProjectDescription: React.FC<Props> = ({ description }) => {
  const project = useProject();
  const permissions = useProjectPermissions();
  const canManage = permissions.satisfiesPermission('project.edit');

  return (
    <StyledContainer data-cy="project-dashboard-description">
      {canManage && (
        <StyledTileEdit>
          <IconButton
            component={Link}
            color="inherit"
            to={LINKS.PROJECT_EDIT.build({ [PARAMS.PROJECT_ID]: project.id })}
          >
            <Edit02 width={20} height={20} />
          </IconButton>
        </StyledTileEdit>
      )}
      <StyledContent>
        <ReactMarkdown
          components={{
            a: (props) => (
              <MuiLink
                href={props.href || ''}
                target="_blank"
                rel="nofollow noreferrer noopener"
              >
                {props.children}
              </MuiLink>
            ),
          }}
        >
          {description}
        </ReactMarkdown>
      </StyledContent>
    </StyledContainer>
  );
};
