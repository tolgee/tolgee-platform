import {
  Typography,
  Paper,
  GlobalStyles,
  styled,
  useMediaQuery,
} from '@mui/material';
import clsx from 'clsx';

import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { CompactFooter } from './CompactFooter';
import {
  SPLIT_CONTENT_BREAK_POINT,
  FULL_PAGE_BREAK_POINT,
} from 'tg.component/security/SplitContent';

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
  min-height: 100px;
  @media ${FULL_PAGE_BREAK_POINT} {
    min-height: 50px;
  }
  justify-content: center;
`;

const StyledPaper = styled(Paper)`
  display: grid;
  grid-template-columns: 1fr;
  background: ${({ theme }) => theme.palette.login.backgroundPrimary};
  &.split {
    grid-template-columns: 1fr 1fr;
  }
  @media ${FULL_PAGE_BREAK_POINT} {
    background: ${({ theme }) => theme.palette.login.backgroundFloating};
    box-shadow: none;
  }
`;

const StyledContent = styled('div')`
  display: grid;
  padding: 60px;
  @media ${SPLIT_CONTENT_BREAK_POINT} {
    padding: 45px;
  }
`;

const StyledPrimaryContent = styled(StyledContent)`
  display: grid;
  padding: 60px;
  &.split {
    padding-right: 45px;
  }
`;

const StyledSecondaryContent = styled(StyledContent)`
  background: ${({ theme }) => theme.palette.login.backgroundSecondary};
  &.split {
    padding-left: 45px;
  }
`;

type Props = {
  windowTitle: string;
  alerts?: React.ReactNode;
  title: React.ReactNode;
  primaryContent: React.ReactNode;
  secondaryContent?: React.ReactNode;
  maxWidth?: number;
};

export const CompactView: React.FC<Props> = ({
  windowTitle,
  primaryContent,
  secondaryContent,
  title,
  alerts,
  maxWidth = 430,
}) => {
  const isSmall = useMediaQuery(SPLIT_CONTENT_BREAK_POINT);

  useWindowTitle(windowTitle);

  const split = !isSmall && Boolean(secondaryContent);

  return (
    <StyledContainer className={clsx({ split })}>
      <GlobalStyles
        styles={(theme) => ({
          body: {
            backgroundColor:
              theme.palette.login.backgroundFloating + ' !important',
          },
        })}
      />
      <StyledInner style={{ width: `min(${maxWidth}px, 100%)` }}>
        <StyledAlerts>{alerts}</StyledAlerts>
        <StyledPaper className={clsx({ split })}>
          <StyledPrimaryContent className={clsx({ split })}>
            <Typography
              color="textSecondary"
              variant="h5"
              sx={{ marginBottom: '20px' }}
            >
              {title}
            </Typography>
            <div>{primaryContent}</div>
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
