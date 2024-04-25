import {
  Typography,
  Paper,
  IconButton,
  GlobalStyles,
  styled,
} from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import { Link } from 'react-router-dom';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { CompactFooter } from './CompactFooter';

const StyledContainer = styled('div')`
  width: 100%;
  min-height: 100%;
  display: grid;
  align-items: space-between;
  justify-items: stretch;
  grid-template-rows: 1fr auto;
  padding: 0 32px 0 32px;
`;

const StyledInner = styled('div')`
  flex-direction: column;
  flex-grow: 1;
  align-items: stretch;
  justify-self: center;
  margin: ${({ theme }) => theme.spacing(1, 0, 6, 0)};
  padding: ${({ theme }) => theme.spacing(0, 4, 0, 4)};
  @media (max-width: 800px) {
    padding: 0;
  }
`;

const StyledAlerts = styled('div')`
  display: flex;
  flex-direction: column;
  min-height: 100px;
  @media (max-width: 800px) {
    min-height: 55px;
  }
  justify-content: flex-end;
`;

const StyledPaper = styled(Paper)`
  display: flex;
  align-items: stretch;
  padding: ${({ theme }) => theme.spacing(4, 0, 6, 0)};
  margin-top: ${({ theme }) => theme.spacing(2)};
  position: relative;
  overflow: hidden;
`;

const StyledVerticalSpace = styled('div')`
  display: flex;
  width: ${({ theme }) => theme.spacing(7)};
  align-items: flex-start;
  justify-content: center;
  margin-top: -7px;
  flex-shrink: 0;
`;

const StyledContent = styled('div')`
  flex-grow: 1;
`;

type Props = {
  windowTitle: string;
  backLink?: string | (() => void);
  alerts?: React.ReactNode;
  title?: React.ReactNode;
  content: React.ReactNode;
  maxWidth?: number;
};

export const CompactView: React.FC<Props> = ({
  windowTitle,
  content,
  title,
  alerts,
  backLink,
  maxWidth = 430,
}) => {
  useWindowTitle(windowTitle);

  const buttonProps =
    typeof backLink === 'function'
      ? { onClick: backLink }
      : { to: backLink || '', component: Link };

  return (
    <StyledContainer>
      <GlobalStyles
        styles={(theme) => ({
          body: {
            backgroundColor: theme.palette.emphasis[50] + ' !important',
          },
        })}
      />
      <StyledInner style={{ width: `min(${maxWidth}px, 100%)` }}>
        <StyledAlerts>{alerts}</StyledAlerts>
        <StyledPaper>
          <StyledVerticalSpace>
            {backLink && (
              <IconButton {...buttonProps} size="medium">
                <ArrowBack />
              </IconButton>
            )}
          </StyledVerticalSpace>
          <StyledContent>
            {title && (
              <Typography color="textSecondary" variant="h5">
                {title}
              </Typography>
            )}
            <div>{content}</div>
          </StyledContent>
          <StyledVerticalSpace />
        </StyledPaper>
      </StyledInner>
      <CompactFooter />
    </StyledContainer>
  );
};
