import { Typography, Paper, styled, useMediaQuery, Box } from '@mui/material';
import clsx from 'clsx';

import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { CompactFooter } from './CompactFooter';

export const SPLIT_CONTENT_BREAK_POINT = '(max-width: 900px)';
export const FULL_PAGE_BREAK_POINT = '(max-width: 550px)';

const StyledContainer = styled('div')`
  width: 100%;
  min-height: 100%;
  display: grid;
  align-items: space-between;
  justify-items: stretch;
  grid-template-rows: 1fr auto;
  padding: 0 32px 0 32px;
  @media ${FULL_PAGE_BREAK_POINT} {
    padding: 0px;
    background: ${({ theme }) => theme.palette.login.backgroundPrimary};
  }
`;

const StyledInner = styled('div')`
  flex-direction: column;
  flex-grow: 1;
  align-items: stretch;
  justify-self: center;
  margin: ${({ theme }) => theme.spacing(1, 0, 6, 0)};
  padding: ${({ theme }) => theme.spacing(0, 4, 0, 4)};
  @media ${FULL_PAGE_BREAK_POINT} {
    padding: 0px;
    margin: 0px;
    width: 100% !important;
  }
`;

const StyledAlerts = styled('div')`
  display: flex;
  flex-direction: column;
  min-height: 70px;
  @media ${FULL_PAGE_BREAK_POINT} {
    min-height: 50px;
  }
  justify-content: center;
`;

const StyledPaper = styled(Paper)`
  display: grid;
  grid-template-columns: 1fr;
  background: ${({ theme }) => theme.palette.tokens.background['paper-1']};

  &.split {
    grid-template-columns: 1fr 1fr;
  }

  @media ${FULL_PAGE_BREAK_POINT} {
    box-shadow: none;
    background: none;
  }
`;

const StyledContent = styled('div')`
  display: grid;
  padding: 60px;
  @media ${SPLIT_CONTENT_BREAK_POINT} {
    padding: 35px;
  }
`;

const StyledPrimaryContent = styled(StyledContent)`
  display: grid;
  align-content: start;
  padding: 60px;

  &.split {
    padding-right: 45px;
  }
`;

const StyledSecondaryContent = styled(StyledContent)`
  align-content: end;

  &.split {
    padding-left: 45px;
  }
`;

type Props = {
  windowTitle: string;
  alerts?: React.ReactNode;
  title?: React.ReactNode;
  subtitle?: React.ReactNode;
  primaryContent: React.ReactNode;
  secondaryContent?: React.ReactNode;
  maxWidth?: number;
};

export const CompactView: React.FC<Props> = ({
  windowTitle,
  primaryContent,
  secondaryContent,
  title,
  subtitle,
  alerts,
  maxWidth = 550,
}) => {
  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  useWindowTitle(windowTitle);

  const split = !isSmall && Boolean(secondaryContent);

  return (
    <StyledContainer className={clsx({ split })}>
      <StyledInner style={{ width: `min(${maxWidth}px, 100%)` }}>
        <StyledAlerts>{alerts}</StyledAlerts>
        <StyledPaper className={clsx({ split })}>
          <StyledPrimaryContent className={clsx({ split })}>
            {title && <Typography variant="h4">{title}</Typography>}
            {subtitle && (
              <Typography
                sx={{ mt: 0.5 }}
                color="textSecondary"
                variant="body2"
              >
                {subtitle}
              </Typography>
            )}
            <Box mt={2}>{primaryContent}</Box>
          </StyledPrimaryContent>
          {secondaryContent && (
            <StyledSecondaryContent className={clsx({ split })}>
              {secondaryContent}
            </StyledSecondaryContent>
          )}
        </StyledPaper>
      </StyledInner>
      <CompactFooter />
    </StyledContainer>
  );
};
