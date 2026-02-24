import { Trash01, X } from '@untitled-ui/icons-react';
import { IconButton, styled, Typography } from '@mui/material';
import { T } from '@tolgee/react';
import { useHistory } from 'react-router-dom';
import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';

const StyledBanner = styled('div')`
  background: ${({ theme }) =>
    theme.palette.mode === 'dark' ? 'rgba(211, 47, 47, 0.08)' : '#fef2f2'};
  border: 1px solid
    ${({ theme }) =>
      theme.palette.mode === 'dark' ? 'rgba(211, 47, 47, 0.2)' : '#fecaca'};
  border-radius: 8px;
  padding: 8px 12px;
  display: grid;
  grid-template-columns: auto 1fr auto;
  align-items: center;
  gap: 12px;
`;

const StyledIconWrapper = styled('div')`
  display: flex;
  align-items: center;
  gap: 8px;
  color: ${({ theme }) => theme.palette.error.main};
`;

export const TrashBanner = () => {
  const history = useHistory();
  const project = useProject();

  const handleClose = () => {
    history.push(
      LINKS.PROJECT_TRANSLATIONS.build({
        [PARAMS.PROJECT_ID]: project.id,
      })
    );
  };

  return (
    <StyledBanner>
      <StyledIconWrapper>
        <Trash01 width={20} height={20} />
        <Typography variant="subtitle2" color="error">
          <T keyName="trash_view_title" />
        </Typography>
      </StyledIconWrapper>

      <Typography variant="body2" color="textSecondary">
        <T keyName="trash_banner_message" />
      </Typography>

      <IconButton
        size="small"
        onClick={handleClose}
        data-cy="trash-banner-close"
        aria-label="Close banner"
      >
        <X width={18} height={18} />
      </IconButton>
    </StyledBanner>
  );
};
