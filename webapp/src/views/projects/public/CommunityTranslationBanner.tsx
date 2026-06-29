import { Box, styled, Typography } from '@mui/material';
import { Edit05, MessageTextSquare02 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { MouseIllustration } from 'tg.component/security/MouseIllustration';

const StyledBanner = styled('div')`
  display: grid;
  grid-template-columns: 1fr auto;
  gap: ${({ theme }) => theme.spacing(4)};
  align-items: center;
  padding: ${({ theme }) => theme.spacing(4, 0)};
  @container (max-width: 700px) {
    grid-template-columns: 1fr;
  }
`;

const StyledContent = styled('div')`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(1)};
  max-width: 540px;
`;

const StyledEyebrow = styled(Typography)`
  font-weight: 500;
  color: ${({ theme }) => theme.palette.text.secondary};
`;

const StyledHeading = styled(Typography)`
  font-size: 28px;
  font-weight: 700;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledBullets = styled('div')`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(0.5)};
  margin-top: ${({ theme }) => theme.spacing(1)};
`;

const StyledBullet = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  color: ${({ theme }) => theme.palette.text.secondary};
  & svg {
    color: ${({ theme }) => theme.palette.primary.main};
  }
`;

const StyledIllustration = styled(Box)`
  @container (max-width: 700px) {
    display: none;
  }
`;

export const CommunityTranslationBanner = () => {
  return (
    <StyledBanner data-cy="community-translation-banner">
      <StyledContent>
        <StyledEyebrow variant="body2">
          <T
            keyName="public_projects_banner_eyebrow"
            defaultValue="Community translations"
          />
        </StyledEyebrow>
        <StyledHeading variant="h1">
          <T
            keyName="public_projects_banner_heading"
            defaultValue="Help projects speak your language"
          />
        </StyledHeading>
        <Typography variant="body1" color="textSecondary">
          <T
            keyName="public_projects_banner_subtext"
            defaultValue="Projects of all kinds, from games to apps to tools, are looking for translators. Browse, pick a language, and start contributing."
          />
        </Typography>
        <StyledBullets>
          <StyledBullet>
            <Edit05 width={18} height={18} />
            <Typography variant="body2">
              <T
                keyName="public_projects_banner_bullet_suggest"
                defaultValue="Suggest better translation"
              />
            </Typography>
          </StyledBullet>
          <StyledBullet>
            <MessageTextSquare02 width={18} height={18} />
            <Typography variant="body2">
              <T
                keyName="public_projects_banner_bullet_comment"
                defaultValue="Comment on current translation"
              />
            </Typography>
          </StyledBullet>
        </StyledBullets>
      </StyledContent>
      <StyledIllustration>
        <MouseIllustration />
      </StyledIllustration>
    </StyledBanner>
  );
};
