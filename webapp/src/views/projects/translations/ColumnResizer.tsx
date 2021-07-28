import { useEffect, useState } from 'react';
import Draggable from 'react-draggable';
import { makeStyles } from '@material-ui/core';

const useStyles = makeStyles({
  draggable: {
    zIndex: 1,
    position: 'absolute',
    top: 0,
    width: 4,
    background: 'transparent',
    cursor: 'col-resize',
  },
});

type Props = {
  onResize: (change: number) => void;
  left: number;
  size: number;
  passResizeCallback?: (callback: () => void) => void;
};

export const ColumnResizer: React.FC<Props> = ({
  size,
  left,
  onResize,
  passResizeCallback,
}) => {
  const classes = useStyles();
  const [offset, setOffset] = useState(left);
  const [originalSize, setOriginalSize] = useState(size);
  const [isDragging, setIsDragging] = useState(false);
  const [position, setPosition] = useState({ x: 0, y: 0 });

  useEffect(() => {
    onResize(originalSize + position.x);
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
      onStop={() => {
        setIsDragging(false);
        setPosition({ x: 0, y: 0 });
      }}
      bounds="parent"
    >
      <div
        ref={(el) =>
          passResizeCallback?.(() =>
            el?.dispatchEvent(new Event('mousedown', { bubbles: true }))
          )
        }
        style={{
          left: offset,
          height: '100%',
          background: isDragging ? '#00000030' : undefined,
          pointerEvents: isDragging ? 'auto' : 'none',
          ...(isDragging && { transition: 'background 0s' }),
        }}
        className={classes.draggable}
      />
    </Draggable>
  );
};
