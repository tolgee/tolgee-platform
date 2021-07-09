import { makeStyles } from '@material-ui/core';
import { useEffect } from 'react';
import { useState } from 'react';
import Draggable from 'react-draggable';
import { useDebounce } from 'use-debounce/lib';

const useStyles = makeStyles({
  draggable: {
    zIndex: 1,
    position: 'absolute',
    top: 0,
    width: 5,
    marginLeft: -4,
    cursor: 'col-resize',
    '&:hover': {
      background: 'lightgray',
    },
  },
});

type Props = {
  onResize: (change: number) => void;
  left: number;
  size: number;
};

export const ColumnResizer: React.FC<Props> = ({ size, left, onResize }) => {
  const classes = useStyles();
  const [offset, setOffset] = useState(left);
  const [originalSize, setOriginalSize] = useState(size);
  const [isDragging, setIsDragging] = useState(false);
  const [position, setPosition] = useState({ x: 0, y: 0 });

  const [debouncedPos] = useDebounce(position.x, 300);
  useEffect(() => {
    onResize(originalSize + position.x);
  }, [debouncedPos, isDragging]);

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
        style={{
          left: offset,
          height: isDragging ? '100%' : 20,
          backgroundColor: isDragging ? 'darkgrey' : undefined,
        }}
        className={classes.draggable}
      />
    </Draggable>
  );
};
