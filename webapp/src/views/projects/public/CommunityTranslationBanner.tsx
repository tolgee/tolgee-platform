import { alpha, styled, Typography } from '@mui/material';
import { Edit05, MessageTextSquare02 } from '@untitled-ui/icons-react';
import { T } from '@tolgee/react';

import { SPLIT_CONTENT_BREAK_POINT } from 'tg.component/layout/CompactView';
import { PUBLIC_CONTENT_MAX_WIDTH } from './publicProjectsLayout';

const StyledBanner = styled('div')`
  position: relative;
  width: 100%;
  background: linear-gradient(
    to right,
    ${({ theme }) => alpha(theme.palette.background.default, 0.12)},
    ${({ theme }) => alpha(theme.palette.primary.main, 0.12)} 69%,
    ${({ theme }) => alpha(theme.palette.background.default, 0.12)}
  );
`;

const StyledInner = styled('div')`
  position: relative;
  max-width: ${PUBLIC_CONTENT_MAX_WIDTH}px;
  margin: 0 auto;
  padding: ${({ theme }) => theme.spacing(5, 2)};
`;

const StyledContent = styled('div')`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(2.5)};
  padding-right: 260px;
  @media ${SPLIT_CONTENT_BREAK_POINT} {
    padding-right: 0;
  }
`;

const StyledEyebrow = styled(Typography)`
  font-size: 24px;
  font-weight: 600;
  line-height: 1.235;
  letter-spacing: 0.25px;
  color: ${({ theme }) => theme.palette.text.primary};
`;

const StyledHeading = styled(Typography)`
  font-size: 40px;
  font-weight: 700;
  line-height: 1.167;
  letter-spacing: -1.5px;
  color: ${({ theme }) => theme.palette.primary.main};
`;

const StyledSubtext = styled(Typography)`
  max-width: 561px;
`;

const StyledBullets = styled('div')`
  display: flex;
  flex-direction: column;
  gap: ${({ theme }) => theme.spacing(0.5)};
`;

const StyledBullet = styled('div')`
  display: flex;
  align-items: center;
  gap: ${({ theme }) => theme.spacing(1)};
  color: ${({ theme }) => theme.palette.text.primary};
  & svg {
    color: ${({ theme }) => theme.palette.primary.main};
    flex-shrink: 0;
  }
`;

const StyledMouse = styled('img')`
  position: absolute;
  right: ${({ theme }) => theme.spacing(2)};
  bottom: -15px;
  height: 190px;
  pointer-events: none;
  user-select: none;
  @media ${SPLIT_CONTENT_BREAK_POINT} {
    display: none;
  }
`;

export const CommunityTranslationBanner = () => {
  return (
    <StyledBanner data-cy="community-translation-banner">
      <StyledInner>
        <StyledContent>
          <div>
            <StyledEyebrow>
              <T
                keyName="public_projects_banner_eyebrow"
              />
            </StyledEyebrow>
            <StyledHeading variant="h1">
              <T
                keyName="public_projects_banner_heading"
              />
            </StyledHeading>
          </div>
          <StyledSubtext variant="body1">
            <T
              keyName="public_projects_banner_subtext"
            />
          </StyledSubtext>
          <StyledBullets>
            <StyledBullet>
              <Edit05 width={21} height={22} />
              <Typography variant="body1">
                <T
                  keyName="public_projects_banner_bullet_suggest"
                />
              </Typography>
            </StyledBullet>
            <StyledBullet>
              <MessageTextSquare02 width={21} height={22} />
              <Typography variant="body1">
                <T
                  keyName="public_projects_banner_bullet_comment"
                />
              </Typography>
            </StyledBullet>
          </StyledBullets>
        </StyledContent>
        <StyledMouse src="/images/communityMouse.svg" alt="" />
      </StyledInner>
    </StyledBanner>
  );
};
