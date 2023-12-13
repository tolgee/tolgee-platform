import { ReactNode } from 'react';
import { Box, styled, Typography } from '@mui/material';

import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { Navigation } from 'tg.component/navigation/Navigation';

import { SecondaryBar } from './SecondaryBar';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { BaseViewAddButton } from './BaseViewAddButton';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

const widthMap = {
  wide: 1200,
  normal: 900,
  narrow: 600,
};

const StyledContainer = styled(Box)`
  margin: 0px auto;
  width: 100%;
  max-width: 100%;
`;

type BaseViewWidth = keyof typeof widthMap | number | undefined;

export function getBaseViewWidth(width: BaseViewWidth) {
  return typeof width === 'string' ? widthMap[width] : width;
}

export interface BaseViewProps {
  windowTitle: string;
  loading?: boolean;
  title?: ReactNode;
  onAdd?: () => void;
  addLinkTo?: string;
  addLabel?: string;
  addComponent?: React.ReactNode;
  children: (() => ReactNode) | ReactNode;
  onSearch?: (string) => void;
  navigation?: React.ComponentProps<typeof Navigation>['path'];
  customNavigation?: ReactNode;
  customHeader?: ReactNode;
  navigationRight?: ReactNode;
  switcher?: ReactNode;
  hideChildrenOnLoading?: boolean;
  maxWidth?: BaseViewWidth;
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

  const maxWidth = getBaseViewWidth(props.maxWidth);

  return (
    <StyledContainer
      style={{
        maxWidth: props.allCentered ? maxWidth : undefined,
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
            <StyledContainer
              data-cy="global-base-view-title"
              style={{ maxWidth }}
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
                    {props.addComponent
                      ? props.addComponent
                      : (props.onAdd || props.addLinkTo) && (
                          <BaseViewAddButton
                            addLinkTo={props.addLinkTo}
                            onClick={props.onAdd}
                            label={props.addLabel}
                          />
                        )}
                  </Box>
                </Box>
              )}
            </StyledContainer>
          </SecondaryBar>
        )}
        <Box pl={3} pr={3} pt={2} pb={2}>
          <StyledContainer style={{ maxWidth }}>
            {!props.loading || !hideChildrenOnLoading ? (
              <Box
                data-cy="global-base-view-content"
                display="grid"
                position="relative"
                maxWidth="100%"
              >
                {typeof props.children === 'function'
                  ? props.children()
                  : props.children}
              </Box>
            ) : (
              <></>
            )}
          </StyledContainer>
        </Box>
      </Box>
    </StyledContainer>
  );
};
