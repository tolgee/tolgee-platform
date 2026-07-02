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
  titleAdornment?: ReactNode;
  titleVariant?: React.ComponentProps<typeof Typography>['variant'];
  onAdd?: () => void;
  addLinkTo?: string;
  addLabel?: string;
  addComponent?: React.ReactNode;
  onSearch?: (value: string) => void;
  searchPlaceholder?: string;
  customHeader?: ReactNode;
  standaloneTitle?: boolean;
  switcher?: ReactNode;
  maxWidth?: BaseViewWidth;
  initialSearch?: string;
  customButtons?: ReactNode[];
};

export const HeaderBar: React.VFC<HeaderBarProps> = (props) => {
  const maxWidth = getBaseViewWidth(props.maxWidth);

  const displayHeader =
    props.title !== undefined ||
    props.titleAdornment !== undefined ||
    props.customHeader ||
    props.onSearch ||
    props.onAdd ||
    props.addComponent ||
    props.addLinkTo;

  if (props.headerBarDisable || !displayHeader) {
    return null;
  }

  const hasSearch = typeof props.onSearch === 'function';
  const stackedSearch = Boolean(props.standaloneTitle) && hasSearch;

  const titleContent = (
    <Box display="flex" alignItems="center" gap={props.standaloneTitle ? 2 : 1}>
      {props.title !== undefined && (
        <Typography variant={props.titleVariant || 'h4'}>
          {props.title}
        </Typography>
      )}
      {props.titleAdornment}
    </Box>
  );

  const searchField = hasSearch && (
    <Box>
      <SecondaryBarSearchField
        onSearch={props.onSearch!}
        initial={props.initialSearch}
        placeholder={props.searchPlaceholder}
      />
    </Box>
  );

  const switcherAndButtons = (
    <Box display="flex" gap={2}>
      {props.switcher && (
        <Box display="flex" alignItems="center">
          {props.switcher}
        </Box>
      )}
      {props.customButtons &&
        props.customButtons.map((button, index) => (
          <Box key={index} display="flex" alignItems="center">
            {button}
          </Box>
        ))}
    </Box>
  );

  const addButton = props.addComponent
    ? props.addComponent
    : (props.onAdd || props.addLinkTo) && (
        <BaseViewAddButton
          addLinkTo={props.addLinkTo}
          onClick={props.onAdd}
          label={props.addLabel}
        />
      );

  return (
    <SecondaryBar
      noBorder={props.noBorder || props.standaloneTitle}
      reducedSpacing={props.reducedSpacing}
      {...(props.standaloneTitle ? { pt: 5 } : {})}
    >
      <StyledContainerInner
        data-cy="global-base-view-title"
        style={{ maxWidth }}
      >
        {props.customHeader ||
          (stackedSearch ? (
            <Box display="grid" gap={2}>
              <Box
                display="flex"
                justifyContent="space-between"
                alignItems="center"
              >
                {titleContent}
                {switcherAndButtons}
              </Box>
              <Box
                display="flex"
                justifyContent="space-between"
                alignItems="center"
              >
                {searchField}
                {addButton}
              </Box>
            </Box>
          ) : (
            <Box display="flex" justifyContent="space-between">
              <Box display="flex" alignItems="center" gap={1}>
                {titleContent}
                {searchField}
              </Box>
              <Box display="flex" gap={2}>
                {switcherAndButtons}
                {addButton}
              </Box>
            </Box>
          ))}
      </StyledContainerInner>
    </SecondaryBar>
  );
};
