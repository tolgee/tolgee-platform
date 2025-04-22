import { Box, styled } from '@mui/material';

import { BaseView, BaseViewProps } from 'tg.component/layout/BaseView';
import { SettingsMenu, SettingsMenuItem } from './SettingsMenu';
import { getBaseViewWidth } from 'tg.component/layout/BaseViewWidth';
import { HeaderBar } from 'tg.component/layout/HeaderBar';

const StyledWrapper = styled('div')`
  display: grid;
  gap: 32px;
  grid-template-columns: auto 1fr;
  @container main-container (max-width: 800px) {
    grid-template-columns: none;
  }
`;

const StyledContainer = styled(Box)`
  display: grid;
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
  menuItems,
  maxWidth = 'normal',
  allCentered = true,
  ...otherProps
}) => {
  const containerMaxWidth = getBaseViewWidth(maxWidth);
  return (
    <BaseView {...otherProps} allCentered={false} headerBarDisable>
      <StyledWrapper>
        <StyledMenu>
          <SettingsMenu items={menuItems} />
        </StyledMenu>

        <StyledContainer>
          <StyledContent
            justifySelf={allCentered ? 'center' : undefined}
            width={
              allCentered ? `min(${containerMaxWidth}px, 100%)` : undefined
            }
            maxWidth={containerMaxWidth}
          >
            <HeaderBar
              noBorder
              reducedSpacing
              titleVariant="h6"
              {...otherProps}
            />
            {children}
          </StyledContent>
        </StyledContainer>
      </StyledWrapper>
    </BaseView>
  );
};
