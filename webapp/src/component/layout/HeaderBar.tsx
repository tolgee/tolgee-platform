import { Box, styled, Typography } from '@mui/material';
import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { BaseViewAddButton } from 'tg.component/layout/BaseViewAddButton';
import { ReactNode } from 'react';
import {
  BaseViewWidth,
  getBaseViewWidth,
} from 'tg.component/layout/BaseViewWidth';
import { SecondaryBar } from 'tg.component/layout/SecondaryBar';

const StyledContainerInner = styled(Box)`
  display: grid;
  width: 100%;
  margin: 0px auto;
  margin-top: 0px;
  margin-bottom: 0px;
`;

export type HeaderBarProps = {
  headerBarDisable?: boolean;
  noBorder?: boolean;
  reducedSpacing?: boolean;
  title?: ReactNode;
  titleVariant?: React.ComponentProps<typeof Typography>['variant'];
  onAdd?: () => void;
  addLinkTo?: string;
  addLabel?: string;
  addComponent?: React.ReactNode;
  onSearch?: (string) => void;
  searchPlaceholder?: string;
  customHeader?: ReactNode;
  switcher?: ReactNode;
  maxWidth?: BaseViewWidth;
  initialSearch?: string;
};

export const HeaderBar: React.VFC<HeaderBarProps> = (props) => {
  const maxWidth = getBaseViewWidth(props.maxWidth);

  const displayHeader =
    props.title !== undefined ||
    props.customHeader ||
    props.onSearch ||
    props.onAdd ||
    props.addComponent ||
    props.addLinkTo;

  if (props.headerBarDisable || !displayHeader) {
    return null;
  }
  // return (
  //   (props.title !== undefined || props.addLinkTo || props.onAdd) && (
  //     <Box sx={{ mb: 2, display: 'flex' }}>
  //       <Box sx={{ flexGrow: 1 }}>
  //         <Typography variant="h6">{props.title}</Typography>
  //       </Box>
  //       {(props.addLinkTo || props.onAdd) && (
  //         <Box>
  //           <BaseViewAddButton
  //             label={props.addLabel}
  //             addLinkTo={props.addLinkTo}
  //             onClick={props.onAdd}
  //           ></BaseViewAddButton>
  //         </Box>
  //       )}
  //     </Box>
  //   )
  // );
  return (
    <SecondaryBar
      noBorder={props.noBorder}
      reducedSpacing={props.reducedSpacing}
    >
      <StyledContainerInner
        data-cy="global-base-view-title"
        style={{ maxWidth }}
      >
        {props.customHeader || (
          <Box display="flex" justifyContent="space-between">
            <Box display="flex" alignItems="center" gap="8px">
              {props.title !== undefined && (
                <Typography variant={props.titleVariant || 'h4'}>
                  {props.title}
                </Typography>
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
  );
};
