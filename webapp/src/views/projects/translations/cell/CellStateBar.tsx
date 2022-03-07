import { makeStyles, Tooltip } from '@material-ui/core';
import clsx from 'clsx';
import { T } from '@tolgee/react';

import { translationStates } from 'tg.constants/translationStates';
import { stopAndPrevent } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';

type State = components['schemas']['TranslationViewModel']['state'];

const useStyles = makeStyles({
  state: {
    cursor: 'col-resize',
  },
  stateHover: {
    position: 'absolute',
    width: 12,
    height: '100%',
  },
  stateBorder: {
    height: '100%',
    width: '4px',
  },
});

type Props = {
  state?: State;
  onResize: React.MouseEventHandler<HTMLDivElement>;
};

export const CellStateBar: React.FC<Props> = ({ state, onResize }) => {
  const classes = useStyles();

  const getContent = () => (
    <div className={classes.stateHover} data-cy="translations-state-indicator">
      <div
        className={clsx(classes.stateBorder, classes.state)}
        onMouseDown={stopAndPrevent(onResize)}
        onClick={stopAndPrevent()}
        onMouseUp={stopAndPrevent()}
        style={{
          borderLeft: `4px solid ${
            translationStates[state || 'UNTRANSLATED']?.color ||
            translationStates['UNTRANSLATED'].color
          }`,
        }}
      />
    </div>
  );

  return state && translationStates[state] ? (
    <Tooltip title={<T noWrap>{translationStates[state].translationKey}</T>}>
      {getContent()}
    </Tooltip>
  ) : (
    getContent()
  );
};
