import { ReactNode, useEffect } from 'react';
import { Add } from '@material-ui/icons';
import { T } from '@tolgee/react';
import { Link } from 'react-router-dom';
import {
  Box,
  Button,
  Container,
  LinearProgress,
  useTheme,
  Typography,
  Grid,
} from '@material-ui/core';

import { useLoading } from 'tg.hooks/loading';
import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { useConfig } from 'tg.hooks/useConfig';
import { Navigation } from 'tg.component/navigation/Navigation';

import { SecondaryBar } from './SecondaryBar';

export interface BaseViewProps {
  windowTitle?: string;
  loading?: boolean;
  title?: ReactNode;
  onAdd?: () => void;
  addLinkTo?: string;
  children: (() => ReactNode) | ReactNode;
  xs?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;
  sm?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;
  md?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;
  lg?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;
  onSearch?: (string) => void;
  navigation?: React.ComponentProps<typeof Navigation>['path'];
  customHeader?: ReactNode;
  hideChildrenOnLoading?: boolean;
  containerMaxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | false;
}

export const BaseView = (props: BaseViewProps) => {
  const theme = useTheme();
  const hideChildrenOnLoading =
    props.hideChildrenOnLoading === undefined || props.hideChildrenOnLoading;

  const globalLoading = useLoading();
  const config = useConfig();

  useEffect(() => {
    if (props.windowTitle) {
      const oldTitle = window.document.title;
      window.document.title = `${config.appName} - ${props.windowTitle}`;
      return () => {
        window.document.title = oldTitle;
      };
    }
  }, []);

  return (
    <>
      <Box position="absolute" width="100%" top={0} zIndex={theme.zIndex.modal}>
        {(globalLoading || props.loading) && (
          <LinearProgress
            data-cy="global-base-view-loading"
            style={{ height: '3px' }}
          />
        )}
      </Box>

      <Container
        data-cy="global-base-view-content-scrollable"
        maxWidth={false}
        style={{
          padding: 0,
          minHeight: '100%',
        }}
      >
        <Box minHeight="100%">
          {props.navigation && (
            <SecondaryBar
              height={49}
              display="flex"
              alignItems="center"
              justifyContent="center"
            >
              <Container
                maxWidth={props.containerMaxWidth || false}
                style={{ padding: 0, margin: 0 }}
              >
                <Navigation path={props.navigation} />
              </Container>
            </SecondaryBar>
          )}
          {(props.title || props.customHeader) && (
            <SecondaryBar>
              <Container
                maxWidth={props.containerMaxWidth || false}
                style={{ padding: 0 }}
              >
                <Grid container justifyContent="center" alignItems="center">
                  <Grid
                    data-cy="global-base-view-title"
                    item
                    xs={props.xs || 12}
                    md={props.md || 12}
                    lg={props.lg || 12}
                    sm={props.sm || 12}
                  >
                    {props.customHeader || (
                      <Box display="flex" justifyContent="space-between">
                        <Box display="flex" alignItems={'center'}>
                          <Typography variant="h5">{props.title}</Typography>
                          {typeof props.onSearch === 'function' && (
                            <Box ml={2}>
                              <SecondaryBarSearchField
                                onSearch={props.onSearch}
                              />
                            </Box>
                          )}
                        </Box>
                        <Box display="flex">
                          {(props.onAdd || props.addLinkTo) && (
                            <Button
                              data-cy="global-plus-button"
                              component={props.addLinkTo ? Link : Button}
                              to={props.addLinkTo}
                              startIcon={<Add />}
                              color="primary"
                              size="small"
                              variant="contained"
                              aria-label="add"
                              onClick={props.onAdd}
                            >
                              <T>global_add_button</T>
                            </Button>
                          )}
                        </Box>
                      </Box>
                    )}
                  </Grid>
                </Grid>
              </Container>
            </SecondaryBar>
          )}
          <Box pl={4} pr={4} pt={2} pb={2}>
            <Container
              maxWidth={props.containerMaxWidth || false}
              style={{ padding: 0 }}
            >
              <Grid container justifyContent="center" alignItems="center">
                <Grid
                  item
                  xs={props.xs || 12}
                  md={props.md || 12}
                  lg={props.lg || 12}
                  sm={props.sm || 12}
                >
                  {!props.loading || !hideChildrenOnLoading ? (
                    <Box data-cy="global-base-view-content">
                      {typeof props.children === 'function'
                        ? props.children()
                        : props.children}
                    </Box>
                  ) : (
                    <></>
                  )}
                </Grid>
              </Grid>
            </Container>
          </Box>
        </Box>
      </Container>
    </>
  );
};
