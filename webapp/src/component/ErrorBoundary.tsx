import React from 'react';
import { Container, Box, Paper } from '@mui/material';
import GlobalErrorPage from './common/GlobalErrorPage';

export default class ErrorBoundary extends React.Component<
  {
    children: React.ReactNode;
  },
  {
    hasError: boolean;
    error: any;
  }
> {
  constructor(props) {
    super(props);
    this.state = { hasError: false, error: null };
  }

  static getDerivedStateFromError(error) {
    // Update state so the next render will show the fallback UI.
    return { hasError: true, error: error };
  }

  render() {
    const dev = process.env.NODE_ENV === 'development';

    if (this.state.hasError) {
      return (
        <Container maxWidth={dev ? 'lg' : 'sm'}>
          <Box mt={5}>
            <Paper>
              <GlobalErrorPage error={this.state.error} />
            </Paper>
          </Box>
        </Container>
      );
    }

    return this.props.children;
  }
}
