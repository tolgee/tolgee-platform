import React, { useEffect, useState } from 'react';
import { Popper, styled } from '@mui/material';

import TranslationTools, {
  Props as TranslationToolsProps,
} from './TranslationTools';
import { PopupArrow } from './PopupArrow';

export const TOOLS_HEIGHT = 200;

const StyledPopper = styled('div')`
  position: relative;
  margin-top: 5px;
`;

const StyledPopperContent = styled('div')`
  display: flex;
  height: ${TOOLS_HEIGHT}px;
  background: ${({ theme }) => theme.palette.cellSelected2.main};
  box-shadow: ${({ theme }) =>
    theme.palette.mode === 'dark'
      ? `${theme.shadows[5]}, ${theme.shadows[3]}`
      : theme.shadows[3]};
  border-radius: ${({ theme }) => theme.shape.borderRadius};
`;

type Props = {
  anchorEl: HTMLDivElement | undefined;
  cellPosition?: string;
  data: TranslationToolsProps['data'];
  languageTag: TranslationToolsProps['languageTag'];
};

export const ToolsPopup: React.FC<Props> = ({
  anchorEl,
  cellPosition,
  data,
  languageTag,
}) => {
  const [width, setWidth] = useState<number | undefined>();

  useEffect(() => {
    setWidth(anchorEl?.offsetWidth);
  });

  return width !== undefined ? (
    <Popper
      open={true}
      anchorEl={anchorEl}
      placement="bottom-end"
      modifiers={[
        {
          name: 'flip',
          enabled: false,
        },
      ]}
    >
      <StyledPopper>
        <PopupArrow position={cellPosition || '75%'} />
        <StyledPopperContent>
          <TranslationTools
            languageTag={languageTag}
            width={width}
            data={data}
          />
        </StyledPopperContent>
      </StyledPopper>
    </Popper>
  ) : null;
};
