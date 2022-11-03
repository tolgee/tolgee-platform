import { ReactNode } from 'react';
import { Box, Container, Grid, Typography } from '@mui/material';

import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { Navigation } from 'tg.component/navigation/Navigation';

import { SecondaryBar } from './SecondaryBar';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { BaseViewAddButton } from './BaseViewAddButton';

export interface BaseViewProps {
  windowTitle: string;
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
  customNavigation?: ReactNode;
  customHeader?: ReactNode;
  navigationRight?: ReactNode;
  switcher?: ReactNode;
  hideChildrenOnLoading?: boolean;
  containerMaxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | false;
  allCentered?: boolean;
  'data-cy'?: string;
  initialSearch?: string;
}

export const BaseView = (props: BaseViewProps) => {
  const hideChildrenOnLoading =
    props.hideChildrenOnLoading === undefined || props.hideChildrenOnLoading;

  useGlobalLoading(props.loading);

  useWindowTitle(props.windowTitle);

  const displayNavigation = props.customNavigation || props.navigation;

  const displayHeader =
    props.title ||
    props.customHeader ||
    props.onSearch ||
    props.onAdd ||
    props.addLinkTo;

  return (
    <Container
      maxWidth={props.allCentered ? props.containerMaxWidth : false}
      style={{
        position: 'relative',
        padding: 0,
        flexGrow: 1,
      }}
    >
      <Box minHeight="100%" data-cy={props['data-cy']}>
        {displayNavigation && (
          <SecondaryBar
            height={49}
            display="flex"
            flexDirection="column"
            alignItems="stretch"
            justifyContent="center"
          >
            <Box
              style={{ padding: 0, margin: 0 }}
              display="flex"
              align-items="center"
              justifyContent="space-between"
            >
              <Navigation path={props.navigation!} />
              {props.navigationRight}
            </Box>
          </SecondaryBar>
        )}
        {displayHeader && (
          <SecondaryBar noBorder={Boolean(displayNavigation)}>
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
                      <Box display="flex" alignItems="center" gap="8px">
                        {props.title && (
                          <Typography variant="h4">{props.title}</Typography>
                        )}
                        {typeof props.onSearch === 'function' && (
                          <Box>
                            <SecondaryBarSearchField
                              onSearch={props.onSearch}
                              initial={props.initialSearch}
                            />
                          </Box>
                        )}
                      </Box>
                      <Box display="flex" gap={2}>
                        {props.switcher && (
                          <Box display="flex" alignItems="center">
                            {props.switcher}
                          </Box>
                        )}
                        {(props.onAdd || props.addLinkTo) && (
                          <BaseViewAddButton
                            addLinkTo={props.addLinkTo}
                            onClick={props.onAdd}
                          />
                        )}
                      </Box>
                    </Box>
                  )}
                </Grid>
              </Grid>
            </Container>
          </SecondaryBar>
        )}
        <Box pl={3} pr={3} pt={2} pb={2}>
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
  );
};
