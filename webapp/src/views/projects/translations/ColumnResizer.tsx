import { useEffect, useState } from 'react';
import Draggable from 'react-draggable';
import { makeStyles } from '@material-ui/core';
import clsx from 'clsx';

const useStyles = makeStyles({
  draggable: {
    zIndex: 1,
    position: 'absolute',
    top: 0,
    cursor: 'col-resize',
    display: 'flex',
    justifyContent: 'center',
  },
  expanded: {
    width: 34,
    marginLeft: -15,
    marginRight: -15,
  },
  indicator: {
    height: '100%',
    width: 4,
  },
});

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
          pointerEvents: isDragging ? 'auto' : 'none',
          height: '100%',
        }}
        className={clsx({
          [classes.draggable]: true,
          [classes.expanded]: isDragging,
        })}
      >
        <div
          className={classes.indicator}
          style={{
            background: isDragging ? '#00000030' : undefined,
            height: '100%',
          }}
        />
      </div>
    </Draggable>
  );
};
