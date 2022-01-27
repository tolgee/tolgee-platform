import React, { useEffect, useState } from 'react';
import { makeStyles, Popper } from '@material-ui/core';
import TranslationTools, {
  Props as TranslationToolsProps,
} from './TranslationTools';
import { PopupArrow } from './PopupArrow';

export const TOOLS_HEIGHT = 200;

const useStyles = makeStyles((theme) => ({
  popper: {
    position: 'relative',
    marginTop: 5,
  },
  popperContent: {
    display: 'flex',
    height: TOOLS_HEIGHT,
    background: 'white',
    boxShadow: theme.shadows[3],
    borderRadius: theme.shape.borderRadius,
  },
}));

type Props = {
  anchorEl: HTMLDivElement | undefined;
  cellPosition?: string;
  data: TranslationToolsProps['data'];
};

export const ToolsPopup: React.FC<Props> = ({
  anchorEl,
  cellPosition,
  data,
}) => {
  const classes = useStyles();
  const [width, setWidth] = useState<number | undefined>();

  useEffect(() => {
    setWidth(anchorEl?.offsetWidth);
  });

  return width !== undefined ? (
    <Popper
      open={true}
      anchorEl={anchorEl}
      placement="bottom-end"
      modifiers={{
        flip: {
          enabled: false,
        },
      }}
    >
      <div className={classes.popper}>
        <PopupArrow position={cellPosition || '75%'} />
        <div className={classes.popperContent}>
          <TranslationTools width={width} data={data} />
        </div>
      </div>
    </Popper>
  ) : null;
};
