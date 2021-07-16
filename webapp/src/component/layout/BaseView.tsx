import { default as React, ReactNode } from 'react';
import { Box, Container, LinearProgress } from '@material-ui/core';
import Grid from '@material-ui/core/Grid';
import Typography from '@material-ui/core/Typography';
import grey from '@material-ui/core/colors/grey';

import { useLoading } from 'tg.hooks/loading';

import { SecondaryBar } from './SecondaryBar';

export interface BaseViewProps {
  loading?: boolean;
  title?: ReactNode;
  children: (() => ReactNode) | ReactNode;
  xs?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;
  sm?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;
  md?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;
  lg?: boolean | 'auto' | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 | 11 | 12;
  navigation?: ReactNode;
  customHeader?: ReactNode;
  hideChildrenOnLoading?: boolean;
  containerMaxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | false;
}

export const BaseView = (props: BaseViewProps) => {
  const hideChildrenOnLoading =
    props.hideChildrenOnLoading === undefined || props.hideChildrenOnLoading;

  const globalLoading = useLoading();

  return (
    <Container
      data-cy="global-base-view-content-scrollable"
      maxWidth={false}
      style={{
        backgroundColor: 'white',
        borderBottom: `1px solid ${grey[100]}`,
        padding: 0,
        minHeight: '100%',
      }}
    >
      <Box minHeight="100%">
        {props.navigation}
        {(props.title || props.customHeader) && (
          <SecondaryBar>
            <Container maxWidth={props.containerMaxWidth || false}>
              <Grid container justify="center" alignItems="center">
                <Grid
                  data-cy="global-base-view-title"
                  item
                  xs={props.xs || 12}
                  md={props.md || 12}
                  lg={props.lg || 12}
                  sm={props.sm || 12}
                >
                  {props.customHeader || (
                    <Typography variant="h5">{props.title}</Typography>
                  )}
                </Grid>
              </Grid>
            </Container>
          </SecondaryBar>
        )}
        <Box position="relative" overflow="visible">
          <Box position="absolute" width="100%">
            {(globalLoading || props.loading) && (
              <LinearProgress
                data-cy="global-base-view-loading"
                style={{ height: '2px' }}
              />
            )}
          </Box>
        </Box>
        <Box pl={4} pr={4} pt={2} pb={2}>
          <Container
            maxWidth={props.containerMaxWidth || false}
            style={{ padding: 0 }}
          >
            <Grid container justify="center" alignItems="center">
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
  );
};
