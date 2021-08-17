import { Tooltip } from '@material-ui/core';
import { useTranslate } from '@tolgee/react';

import { StateType, translationStates } from 'tg.constants/translationStates';
import { ControlsButton } from './ControlsButton';
import { StateIcon } from './StateIcon';

type Props = {
  state: StateType | undefined;
  onStateChange?: (s: StateType) => void;
  className?: string;
};

export const StateTransitionButtons: React.FC<Props> = ({
  state,
  onStateChange,
  className,
}) => {
  const t = useTranslate();

  return (
    <>
      {state &&
        translationStates[state]?.next.map((s, i) => (
          <Tooltip
            key={i}
            title={t(
              'translation_state_change',
              {
                newState: t(translationStates[s]?.translationKey, {}, true),
              },
              true
            )}
          >
            <ControlsButton
              data-cy="translation-state-button"
              onClick={() => onStateChange?.(s)}
              className={className}
            >
              <StateIcon state={s} fontSize="small" />
            </ControlsButton>
          </Tooltip>
        ))}
    </>
  );
};
