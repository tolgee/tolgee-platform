import { Box, styled } from '@mui/material';
import { PanelConfig, PanelContentProps } from './types';
import { useState } from 'react';
import { PanelHeader } from './PanelHeader';

const StyledContainer = styled(Box)`
  display: grid;
`;

const StyledContent = styled(Box)`
  min-height: 60px;
  padding-top: 0px;
  padding-bottom: 16px;
  padding-left: 8px;
`;

type Props = PanelConfig & {
  data: Omit<PanelContentProps, 'setItemsCount'>;
  onToggle: () => void;
  open: boolean;
};

export const Panel = ({
  id,
  icon,
  name,
  component,
  data,
  itemsCountFunction,
  hideWhenCountZero,
  hideCount,
  onToggle,
  open,
}: Props) => {
  const [itemsCount, setItemsCount] = useState<number | undefined>(undefined);
  const Component = component;
  const countContent =
    (itemsCount !== undefined && itemsCount !== null
      ? itemsCount
      : itemsCountFunction?.(data)) ?? undefined;

  const hidden = countContent === 0 && hideWhenCountZero;

  if (hidden) {
    return null;
  }

  return (
    <StyledContainer data-cy="translation-panel" data-cy-id={id}>
      <PanelHeader
        icon={icon}
        name={name}
        countContent={countContent}
        onToggle={onToggle}
        panelId={id}
        hideCount={hideCount}
        open={open}
      />
      {open && (
        <StyledContent data-cy="translation-panel-content" data-cy-id={id}>
          <Component {...data} setItemsCount={setItemsCount} />
        </StyledContent>
      )}
    </StyledContainer>
  );
};
