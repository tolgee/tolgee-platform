import * as React from 'react';
import {AppState} from '../store';
import {connect} from 'react-redux';

const GlobalError = React.lazy(() => import(/* webpackChunkName: "global-error" */'./common/GlobalError'));


class ErrorBoundary extends React.Component<{ globalError }, { hasError: boolean, error: any }> {
    constructor(props) {
        super(props);
        this.state = {hasError: false, error: null};
    }

    static getDerivedStateFromError(error) {
        // Update state so the next render will show the fallback UI.
        return {hasError: true, error: error};
    }

    render() {
        if (this.state.hasError || this.props.globalError) {
            return <GlobalError error={this.state.error || this.props.globalError}/>;
        }

        return this.props.children;
    }
}

export default connect((state: AppState) => ({globalError: state.error.error}))(ErrorBoundary);
