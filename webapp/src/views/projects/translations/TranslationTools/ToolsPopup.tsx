import React, { useEffect, useState } from 'react';
import { makeStyles, Popper } from '@material-ui/core';
import TranslationTools, {
  Props as TranslationToolsProps,
} from './TranslationTools';
import { PopupArrow } from './PopupArrow';
import { container } from 'tsyringe';
import { ThemeService } from 'tg.service/ThemeService';

export const TOOLS_HEIGHT = 200;

const paletteType = container.resolve(ThemeService).paletteType;

const useStyles = makeStyles((theme) => ({
  popper: {
    position: 'relative',
    marginTop: 5,
  },
  popperContent: {
    display: 'flex',
    height: TOOLS_HEIGHT,
    background: theme.palette.background.default,
    boxShadow:
      paletteType === 'light'
        ? theme.shadows[3]
        : '0px 3px 3px -2px rgb(255, 255, 255, 0.2), 0px 3px 4px 0px rgb(255, 255, 255, 0.14), 0px 1px 8px 0px rgb(255, 255, 255, 0.12)',
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
