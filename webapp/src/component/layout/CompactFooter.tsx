import { Link, Box, styled } from '@mui/material';

import { ReactComponent as TwitterLogo } from 'tg.svgs/social/twitter.svg';
import { ReactComponent as FacebookLogo } from 'tg.svgs/social/facebook.svg';
import { ReactComponent as GitHubLogo } from 'tg.svgs/social/github.svg';
import { ReactComponent as LinkedInLogo } from 'tg.svgs/social/linkedin.svg';
import { ReactComponent as SlackLogo } from 'tg.svgs/social/slack.svg';
import { ReactComponent as DiscussionsLogo } from 'tg.svgs/social/discussions.svg';

const StyledContainer = styled('div')`
  display: grid;
  border-top: 1px solid ${({ theme }) => theme.palette.divider};
  padding: ${({ theme }) => theme.spacing(2, 4)};
  justify-items: center;
  position: relative;
`;

const StyledContent = styled('div')`
  display: grid;
  width: min(800px, 100%);
  grid-template-columns: repeat(auto-fit, minmax(230px, 1fr));
`;

const StyledSocial = styled(Box)`
  padding: ${({ theme }) => theme.spacing(1, 3)};
`;

const StyledLink = styled(Link)`
  display: inline-flex;
  gap: 8px;
  align-items: center;
  color: ${({ theme }) => theme.palette.text.secondary};
  transition: color 50ms ease-in-out;
  &:hover {
    color: ${({ theme }) => theme.palette.text.primary};
  }
`;

export const CompactFooter = () => {
  return (
    <StyledContainer>
      <StyledContent>
        <StyledSocial>
          <StyledLink
            href="https://twitter.com/tolgee_i18n"
            target="_blank"
            rel="noopener noreferrer"
          >
            <TwitterLogo />
            <span>Twitter</span>
          </StyledLink>
        </StyledSocial>
        <StyledSocial>
          <StyledLink
            href="https://www.facebook.com/Tolgee.i18n"
            target="_blank"
            rel="noopener noreferrer"
          >
            <FacebookLogo />
            <span>Facebook</span>
          </StyledLink>
        </StyledSocial>
        <StyledSocial>
          <StyledLink
            href="https://github.com/tolgee/tolgee-platform"
            target="_blank"
            rel="noopener noreferrer"
          >
            <GitHubLogo />
            <span>GitHub</span>
          </StyledLink>
        </StyledSocial>
        <StyledSocial>
          <StyledLink
            href="https://www.linkedin.com/company/tolgee/"
            target="_blank"
            rel="noopener noreferrer"
          >
            <LinkedInLogo />
            <span>LinkedIn</span>
          </StyledLink>
        </StyledSocial>
        <StyledSocial>
          <StyledLink
            href="https://tolg.ee/slack"
            target="_blank"
            rel="noopener noreferrer"
          >
            <SlackLogo />
            <span>Slack</span>
          </StyledLink>
        </StyledSocial>
        <StyledSocial>
          <StyledLink
            href="https://github.com/tolgee/tolgee-platform/discussions"
            target="_blank"
            rel="noopener noreferrer"
          >
            <DiscussionsLogo />
            <span>Discussions</span>
          </StyledLink>
        </StyledSocial>
      </StyledContent>
    </StyledContainer>
  );
};
