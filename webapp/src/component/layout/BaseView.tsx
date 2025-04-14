import { FC, ReactNode } from 'react';
import { Box, styled } from '@mui/material';

import { Navigation } from 'tg.component/navigation/Navigation';

import { SecondaryBar } from './SecondaryBar';
import { useWindowTitle } from 'tg.hooks/useWindowTitle';
import { useGlobalLoading } from 'tg.component/GlobalLoading';
import {
  BaseViewWidth,
  getBaseViewWidth,
} from 'tg.component/layout/BaseViewWidth';
import { HeaderBar, HeaderBarProps } from 'tg.component/layout/HeaderBar';

export const BASE_VIEW_PADDING = 24;

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

export type BaseViewProps = {
  windowTitle: string;
  loading?: boolean;
  children: (() => ReactNode) | ReactNode;
  navigation?: React.ComponentProps<typeof Navigation>['path'];
  customNavigation?: ReactNode;
  navigationRight?: ReactNode;
  hideChildrenOnLoading?: boolean;
  maxWidth?: BaseViewWidth;
  allCentered?: boolean;
  'data-cy'?: string;
  overflow?: string;
  wrapperProps?: React.ComponentProps<typeof Box>;
  stretch?: boolean;
} & Omit<HeaderBarProps, 'noBorder'>;

export const BaseView: FC<BaseViewProps> = (props) => {
  const hideChildrenOnLoading =
    props.hideChildrenOnLoading === undefined || props.hideChildrenOnLoading;

  useGlobalLoading(props.loading);

  useWindowTitle(props.windowTitle);

  const displayNavigation = props.customNavigation || props.navigation;

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
        <HeaderBar noBorder={Boolean(displayNavigation)} {...props} />
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
