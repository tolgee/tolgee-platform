import { Box, styled, Typography, Container } from '@mui/material';

import { BaseView, BaseViewProps } from 'tg.component/layout/BaseView';
import { SettingsMenu, SettingsMenuItem } from './SettingsMenu';

const StyledWrapper = styled('div')`
  display: grid;
  grid-template: auto / auto 1fr;
  gap: 32px;
  @media (max-width: 800px) {
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
  containerMaxWidth,
  ...otherProps
}) => {
  return (
    <BaseView {...otherProps}>
      <StyledWrapper>
        <StyledMenu>
          <SettingsMenu items={menuItems} />
        </StyledMenu>

        <StyledContainer maxWidth={containerMaxWidth}>
          <StyledContent>
            {title && (
              <Box mb={2}>
                <Typography variant="h6">{title}</Typography>
              </Box>
            )}
            {children}
          </StyledContent>
        </StyledContainer>
      </StyledWrapper>
    </BaseView>
  );
};
