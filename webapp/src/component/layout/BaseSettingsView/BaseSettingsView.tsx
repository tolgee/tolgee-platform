import { Box, Container, styled, Typography } from '@mui/material';

import {
  BaseView,
  BaseViewProps,
  getBaseViewWidth,
} from 'tg.component/layout/BaseView';
import { SettingsMenu, SettingsMenuItem } from './SettingsMenu';
import { BaseViewAddButton } from '../BaseViewAddButton';

const StyledWrapper = styled('div')`
  display: grid;
  grid-template: auto / auto 1fr;
  gap: 32px;
  @container main-container (max-width: 800px) {
    grid-template: auto auto / auto;
  }
  overflow-x: hidden;
`;

const StyledContainer = styled(Container)`
  display: grid;
  padding: 0px !important;
`;

const StyledMenu = styled('div')`
  min-width: 200px;
`;

const StyledContent = styled(Box)`
  display: grid;
`;

type Props = BaseViewProps & {
  menuItems: SettingsMenuItem[];
};

export const BaseSettingsView: React.FC<Props> = ({
  children,
  title,
  menuItems,
  addLinkTo,
  maxWidth = 'normal',
  onAdd,
  ...otherProps
}) => {
  const containerMaxWidth = getBaseViewWidth(maxWidth);
  return (
    <BaseView {...otherProps}>
      <StyledWrapper>
        <StyledMenu>
          <SettingsMenu items={menuItems} />
        </StyledMenu>

        <StyledContainer style={{ maxWidth: containerMaxWidth }}>
          <StyledContent>
            {title && (
              <Box sx={{ mb: 2, display: 'flex' }}>
                <Box sx={{ flexGrow: 1 }}>
                  <Typography variant="h6">{title}</Typography>
                </Box>
                {(addLinkTo || onAdd) && (
                  <Box>
                    <BaseViewAddButton
                      addLinkTo={addLinkTo}
                      onClick={onAdd}
                    ></BaseViewAddButton>
                  </Box>
                )}
              </Box>
            )}
            {children}
          </StyledContent>
        </StyledContainer>
      </StyledWrapper>
    </BaseView>
  );
};
