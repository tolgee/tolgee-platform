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

const StyledContainer = styled('div')`
  flex-direction: column;
  flex-grow: 1;
  align-items: center;
  margin: ${({ theme }) => theme.spacing(1)} auto;
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
  padding: ${({ theme }) => theme.spacing(4, 0)};
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

const StyledFooter = styled('div')`
  padding: ${({ theme }) => theme.spacing(1, 7)};
`;

type Props = {
  windowTitle: string;
  backLink?: string;
  alerts?: React.ReactNode;
  title: React.ReactNode;
  content: React.ReactNode;
  footer?: React.ReactNode;
  maxWidth?: number;
};

export const CompactView: React.FC<Props> = ({
  windowTitle,
  content,
  title,
  footer,
  alerts,
  backLink,
  maxWidth = 430,
}) => {
  useWindowTitle(windowTitle);

  return (
    <>
      <GlobalStyles
        styles={(theme) => ({
          body: {
            backgroundColor: theme.palette.emphasis[100] + ' !important',
          },
        })}
      />
      <StyledContainer style={{ maxWidth }}>
        <StyledAlerts>{alerts}</StyledAlerts>
        <StyledPaper>
          <StyledVerticalSpace>
            {backLink && (
              <IconButton to={backLink} component={Link} size="medium">
                <ArrowBack />
              </IconButton>
            )}
          </StyledVerticalSpace>
          <StyledContent>
            <Typography color="textSecondary" variant="h5">
              {title}
            </Typography>
            <div>{content}</div>
          </StyledContent>
          <StyledVerticalSpace />
        </StyledPaper>
        <StyledFooter>{footer}</StyledFooter>
      </StyledContainer>
    </>
  );
};
