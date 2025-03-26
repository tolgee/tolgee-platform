import { Box, Container, Paper } from '@mui/material';
import { GlobalError } from 'tg.error/GlobalError';
import GlobalErrorPage from './GlobalErrorPage';
import { ApiError } from 'tg.service/http/ApiError';

type Props = {
  error: GlobalError | ApiError;
};

export const GlobalErrorView = ({ error }: Props) => {
  const dev = process.env.NODE_ENV === 'development';

  return (
    <Container maxWidth={dev ? 'lg' : 'sm'}>
      <Box mt={5}>
        <Paper>
          <GlobalErrorPage error={error} />
        </Paper>
      </Box>
    </Container>
  );
};
