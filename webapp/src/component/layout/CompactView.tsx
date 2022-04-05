import {
  Typography,
  Paper,
  IconButton,
  GlobalStyles,
  styled,
} from '@mui/material';
import { ArrowBack } from '@mui/icons-material';
import { Link } from 'react-router-dom';

const StyledContainer = styled('div')`
  flex-direction: column;
  flex-grow: 1;
  align-items: center;
  margin: ${({ theme }) => theme.spacing(1)} auto;
  max-width: 430px;
`;

const StyledAlerts = styled('div')`
  display: flex;
  flex-direction: column;
  min-height: 100px;
  justify-content: flex-end;
`;

const StyledPaper = styled(Paper)`
  display: flex;
  align-items: stretch;
  padding: ${({ theme }) => theme.spacing(4, 0)};
  margin-top: ${({ theme }) => theme.spacing(2)};
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
  backLink?: string;
  alerts?: React.ReactNode;
  title: React.ReactNode;
  content: React.ReactNode;
  footer?: React.ReactNode;
};

export const CompactView: React.FC<Props> = ({
  content,
  title,
  footer,
  alerts,
  backLink,
}) => {
  return (
    <>
      <GlobalStyles
        styles={(theme) => ({
          body: {
            backgroundColor:
              theme.palette.extraLightBackground.main + ' !important',
          },
        })}
      />
      <StyledContainer>
        <StyledAlerts>{alerts}</StyledAlerts>
        <StyledPaper>
          <StyledVerticalSpace>
            {backLink && (
              <IconButton to={backLink} component={Link} size="large">
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
