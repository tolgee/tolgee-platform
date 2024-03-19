import { useEffect, useState } from 'react';
import clsx from 'clsx';
import Draggable from 'react-draggable';
import { styled } from '@mui/material';

const StyledDraggableContent = styled('div')`
  z-index: 1;
  position: absolute;
  top: 0px;
  cursor: col-resize;
  display: flex;
  justify-content: center;

  & .expanded {
    width: 2004px;
    margin-left: -1000px;
    margin-right: -1000px;
  }
`;

const StyledIndicator = styled('div')`
  height: 100%;
  width: 4px;
`;

type Props = {
  onResize: (change: number) => void;
  left: number | string;
  size: number;
  passResizeCallback?: (callback: () => void) => void;
};

export const ColumnResizer: React.FC<Props> = ({
  size,
  left,
  onResize,
  passResizeCallback,
}) => {
  const [offset, setOffset] = useState(left);
  const [originalSize, setOriginalSize] = useState(size);
  const [isDragging, setIsDragging] = useState(false);
  const [position, setPosition] = useState({ x: 0, y: 0 });

  useEffect(() => {
    if (isDragging) {
      onResize(originalSize + position.x);
    }
  }, [position, isDragging]);

  useEffect(() => {
    if (!isDragging) {
      setOffset(left);
      setOriginalSize(size);
    }
  }, [isDragging, left]);

  return (
    <Draggable
      axis="x"
      position={position}
      onStart={() => {
        setIsDragging(true);
      }}
      onDrag={(e, data) => {
        setPosition({ x: data.x, y: 0 });
      }}
      onStop={(e) => {
        e.preventDefault();
        e.stopPropagation();
        setIsDragging(false);
        setPosition({ x: 0, y: 0 });
      }}
      bounds={false}
    >
      <StyledDraggableContent
        ref={(el) =>
          passResizeCallback?.(() =>
            el?.dispatchEvent(new Event('mousedown', { bubbles: true }))
          )
        }
        style={{
          left: offset,
          pointerEvents: isDragging ? 'auto' : 'none',
          height: '100%',
        }}
        className={clsx({
          expanded: isDragging,
        })}
      >
        <StyledIndicator
          style={{
            background: isDragging ? '#00000030' : undefined,
          }}
        />
      </StyledDraggableContent>
    </Draggable>
  );
};
