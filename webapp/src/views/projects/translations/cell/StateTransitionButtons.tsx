import { useTranslate } from '@tolgee/react';

import { StateType, translationStates } from 'tg.constants/translationStates';
import { components } from 'tg.service/apiSchema.generated';
import { ControlsButton } from './ControlsButton';
import { StateIcon } from './StateIcon';

type State = components['schemas']['TranslationViewModel']['state'];

type Props = {
  state: State | undefined;
  onStateChange?: (s: StateType) => void;
  className?: string;
};

export const StateTransitionButtons: React.FC<Props> = ({
  state,
  onStateChange,
  className,
}) => {
  const { t } = useTranslate();

  const nextState: StateType =
    (state && translationStates[state]?.next) || 'TRANSLATED';

  return (
    <>
      {state !== 'UNTRANSLATED' && (
        <ControlsButton
          data-cy="translation-state-button"
          onClick={() => onStateChange?.(nextState)}
          className={className}
          tooltip={t('translation_state_change', {
            newState: t(translationStates[nextState]?.translationKey || ''),
          })}
        >
          <StateIcon state={state} fontSize="small" />
        </ControlsButton>
      )}
    </>
  );
};
