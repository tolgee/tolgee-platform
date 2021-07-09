import { useEffect, useState } from 'react';
import Draggable from 'react-draggable';
import { makeStyles } from '@material-ui/core';

const useStyles = makeStyles({
  draggable: {
    zIndex: 1,
    position: 'absolute',
    top: 0,
    width: 5,
    marginLeft: -2,
    background: 'transparent',
    cursor: 'col-resize',
    '&:hover': {
      background: 'lightgray',
      transition: 'background 0.5s step-end',
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
        style={{
          left: offset,
          height: '100%',
          background: isDragging ? 'darkgrey' : undefined,
          ...(isDragging && { transition: 'background 0s' }),
        }}
        className={classes.draggable}
      />
    </Draggable>
  );
};
