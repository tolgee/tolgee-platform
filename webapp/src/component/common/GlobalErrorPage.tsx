import { Button, styled } from '@mui/material';
import Box from '@mui/material/Box';
import Typography from '@mui/material/Typography';
import { LINKS } from 'tg.constants/links';

import { GlobalError } from 'tg.error/GlobalError';
import { ApiError } from 'tg.service/http/ApiError';
import { tokenService } from 'tg.service/TokenService';
import { useErrorTranslation } from 'tg.translationTools/useErrorTranslation';

const StyledImage = styled('img')`
  filter: grayscale(50%);
  opacity: 0.7;
  max-width: 100%;
  width: 500px;
`;

type Props = {
  error: GlobalError | ApiError;
};

export default function GlobalErrorPage({ error }: Props) {
  const dev = process.env.NODE_ENV === 'development';
  const translateError = useErrorTranslation();

  return (
    <Box p={4}>
      <Box mb={5}>
        <Typography variant="h4">
          {error instanceof ApiError && error.code
            ? translateError(error.code, error.params)
            : 'Unexpected error occurred'}
        </Typography>
      </Box>

      {!dev && (
        <Box
          display="flex"
          justifyContent="center"
          flexDirection="column"
          alignItems="center"
        >
          <StyledImage src="/images/brokenMouse.svg" draggable="false" />
        </Box>
      )}

      {error instanceof GlobalError && error.publicInfo && (
        <Box mb={5}>
          <Typography variant="h4">{error.publicInfo}</Typography>
        </Box>
      )}

      <Box display="flex" justifyContent="center" p={3}>
        <Button
          size="large"
          variant="outlined"
          color="primary"
          onClick={() => {
            tokenService.disposeAllTokens();
            location.href = LINKS.LOGIN.build();
          }}
        >
          Start over!
        </Button>
      </Box>

      <Typography variant="body1">
        The error is logged and we will fix this soon. Now please try to reload
        this page.
      </Typography>
      {dev && (
        <Box mt={5}>
          {error instanceof GlobalError && error.debugInfo && (
            <>
              <Typography variant="h5">Debug information</Typography>
              <pre>{error.debugInfo}</pre>
            </>
          )}
          <Typography variant="h5">Stack trace</Typography>
          <pre>{error.stack}</pre>

          {error instanceof GlobalError && error.e && (
            <pre>{error.e && error.e.stack}</pre>
          )}
        </Box>
      )}
    </Box>
  );
}
