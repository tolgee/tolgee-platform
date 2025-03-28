import { Box, Container, styled, Typography } from '@mui/material';

import {
  BaseView,
  BaseViewProps,
  getBaseViewWidth,
} from 'tg.component/layout/BaseView';
import { SettingsMenu, SettingsMenuItem } from './SettingsMenu';
import { BaseViewAddButton } from '../BaseViewAddButton';

const StyledWrapper = styled('div')`
  display: flex;
  gap: 32px;
  @container main-container (max-width: 800px) {
    flex-direction: column;
  }
`;

const StyledContainer = styled(Container)`
  display: flex;
  padding: 0px !important;
  container: main-container / inline-size;
`;

const StyledMenu = styled('div')`
  min-width: 200px;
`;

const StyledContent = styled(Box)`
  flex-grow: 1;
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
  addLabel,
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
            {(title !== undefined || addLinkTo || onAdd) && (
              <Box sx={{ mb: 2, display: 'flex' }}>
                <Box sx={{ flexGrow: 1 }}>
                  <Typography variant="h6">{title}</Typography>
                </Box>
                {(addLinkTo || onAdd) && (
                  <Box>
                    <BaseViewAddButton
                      label={addLabel}
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
