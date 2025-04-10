import { FC, ReactNode } from 'react';
import { Box, styled, Typography } from '@mui/material';

import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { Navigation } from 'tg.component/navigation/Navigation';

import { SecondaryBar } from './SecondaryBar';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { BaseViewAddButton } from './BaseViewAddButton';
import { useGlobalLoading } from 'tg.component/GlobalLoading';

export const BASE_VIEW_PADDING = 24;

const widthMap = {
  wide: 1200,
  normal: 900,
  narrow: 600,
  max: undefined,
};

const StyledContainer = styled(Box)`
  display: grid;
  width: 100%;
  max-width: 100%;
`;

const StyledPaddingWrapper = styled(Box)`
  display: grid;
  padding: 16px ${BASE_VIEW_PADDING}px;
`;

const StyledContainerInner = styled(Box)`
  display: grid;
  width: 100%;
  margin: 0px auto;
  margin-top: 0px;
  margin-bottom: 0px;
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
  searchPlaceholder?: string;
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
  overflow?: string;
  wrapperProps?: React.ComponentProps<typeof Box>;
  stretch?: boolean;
}

export const BaseView: FC<BaseViewProps> = (props) => {
  const hideChildrenOnLoading =
    props.hideChildrenOnLoading === undefined || props.hideChildrenOnLoading;

  useGlobalLoading(props.loading);

  useWindowTitle(props.windowTitle);

  const displayNavigation = props.customNavigation || props.navigation;

  const displayHeader =
    props.title !== undefined ||
    props.customHeader ||
    props.onSearch ||
    props.onAdd ||
    props.addComponent ||
    props.addLinkTo;

  const maxWidth = getBaseViewWidth(props.maxWidth);

  return (
    <StyledContainer
      justifySelf={props.allCentered ? 'center' : undefined}
      width={props.allCentered ? `min(${maxWidth}px, 100%)` : undefined}
      maxWidth={props.allCentered ? maxWidth : undefined}
    >
      <Box
        display="grid"
        gridTemplateRows="auto auto auto 1fr"
        data-cy={props['data-cy']}
      >
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
            <StyledContainerInner
              data-cy="global-base-view-title"
              style={{ maxWidth }}
            >
              {props.customHeader || (
                <Box display="flex" justifyContent="space-between">
                  <Box display="flex" alignItems="center" gap="8px">
                    {props.title !== undefined && (
                      <Typography variant="h4">{props.title}</Typography>
                    )}
                    {typeof props.onSearch === 'function' && (
                      <Box>
                        <SecondaryBarSearchField
                          onSearch={props.onSearch}
                          initial={props.initialSearch}
                          placeholder={props.searchPlaceholder}
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
            </StyledContainerInner>
          </SecondaryBar>
        )}
        <StyledPaddingWrapper
          {...props.wrapperProps}
          gridRow={props.stretch ? 4 : undefined}
        >
          <StyledContainerInner style={{ maxWidth }}>
            {!props.loading || !hideChildrenOnLoading ? (
              <Box
                data-cy="global-base-view-content"
                display="grid"
                position="relative"
                sx={{ overflow: props.overflow }}
              >
                {typeof props.children === 'function'
                  ? props.children()
                  : props.children}
              </Box>
            ) : null}
          </StyledContainerInner>
        </StyledPaddingWrapper>
      </Box>
    </StyledContainer>
  );
};
