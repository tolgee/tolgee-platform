import React from 'react';
import { GlobalErrorView } from './common/GlobalErrorView';

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
    if (this.state.hasError) {
      return <GlobalErrorView error={this.state.error} />;
    }

    return this.props.children;
  }
}
