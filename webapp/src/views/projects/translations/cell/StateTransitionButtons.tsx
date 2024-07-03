import { useTranslate } from '@tolgee/react';

import {
  StateInType,
  TRANSLATION_STATES,
} from 'tg.constants/translationStates';
import { components } from 'tg.service/apiSchema.generated';
import { useStateTranslation } from 'tg.translationTools/useStateTranslation';
import { ControlsButton } from './ControlsButton';
import { StateIcon } from './StateIcon';

type State = components['schemas']['TranslationViewModel']['state'];

type Props = {
  state: State | undefined;
  onStateChange?: (s: StateInType) => void;
  className?: string;
};

export const StateTransitionButtons: React.FC<Props> = ({
  state,
  onStateChange,
  className,
}) => {
  const translateState = useStateTranslation();
  const { t } = useTranslate();

  const nextState = state && TRANSLATION_STATES[state]?.next;

  return (
    <>
      {nextState && (
        <ControlsButton
          data-cy="translation-state-button"
          onClick={() => onStateChange?.(nextState)}
          className={className}
          tooltip={t('translation_state_change', {
            newState: translateState(nextState),
          })}
        >
          <StateIcon state={state} fontSize="small" />
        </ControlsButton>
      )}
    </>
  );
};
