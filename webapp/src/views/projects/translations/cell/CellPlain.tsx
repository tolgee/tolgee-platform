import React from 'react';
import { Box, BoxProps, Tooltip, useTheme } from '@material-ui/core';

import { StateType, translationStates } from 'tg.constants/translationStates';
import { useCellStyles } from './styles';
import { stopBubble } from './stopBubble';
import clsx from 'clsx';
import { T } from '@tolgee/react';

type Props = {
  background?: string;
  hover?: boolean;
  onClick?: () => void;
  state?: StateType | 'NONE';
  onResize?: any;
} & BoxProps;

export const CellPlain: React.FC<Props> = ({
  children,
  background,
  hover,
  onClick,
  state,
  onResize,
  ...props
}) => {
  const classes = useCellStyles();
  const theme = useTheme();

  return (
    <Box
      className={clsx({
        [classes.cellPlain]: true,
        [classes.cellClickable]: onClick,
        [classes.hover]: hover,
      })}
      onClick={onClick}
      display="flex"
      flexDirection="column"
      width="100%"
      bgcolor={background}
      position="relative"
      data-cy="translations-table-cell"
      {...props}
    >
      {children}
      {state &&
        (state === 'NONE' ? (
          <Box
            onMouseDown={stopBubble(onResize)}
            onClick={stopBubble()}
            onMouseUp={stopBubble()}
            position="absolute"
            height="100%"
            borderLeft={`4px solid ${theme.palette.grey[200]}`}
            className={classes.state}
          />
        ) : (
          <Tooltip
            title={<T noWrap>{translationStates[state]?.translationKey}</T>}
          >
            <Box
              position="absolute"
              width={12}
              height="100%"
              data-cy="translations-state-indicator"
            >
              <Box
                onMouseDown={stopBubble(onResize)}
                onClick={stopBubble()}
                onMouseUp={stopBubble()}
                position="absolute"
                height="100%"
                borderLeft={`4px solid ${translationStates[state]?.color}`}
                className={classes.state}
              />
            </Box>
          </Tooltip>
        ))}
    </Box>
  );
};
