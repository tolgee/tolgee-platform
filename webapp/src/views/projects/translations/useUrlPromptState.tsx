import { QUERY } from 'tg.constants/links';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

export type PlaygroundType = 'translate' | 'review';

export const usePromptUrlState = () => {
  const [_openPrompt, _setOpenPrompt] = useUrlSearchState(
    QUERY.TRANSLATIONS_AI_PLAYGROUND_PROMPT,
    {
      defaultVal: undefined,
      history: false,
    }
  );

  const [_aiPlayground, _setAiPlayground] = useUrlSearchState(
    QUERY.TRANSLATIONS_AI_PLAYGROUND,
    {
      defaultVal: undefined,
      history: false,
    }
  );

  const [_playgroundType, _setPlaygroundType] = useUrlSearchState(
    QUERY.TRANSLATIONS_AI_PLAYGROUND_TYPE,
    {
      defaultVal: undefined,
      history: false,
    }
  );

  function setAiPlayground(value: boolean | undefined) {
    _setAiPlayground(value ? '1' : undefined);
    if (!value) {
      _setPlaygroundType(undefined);
    }
  }
  function setOpenPrompt(value: number | undefined) {
    _setOpenPrompt(typeof value === 'number' ? String(value) : undefined);
  }
  function setPlaygroundType(value: PlaygroundType) {
    _setPlaygroundType(value === 'review' ? 'review' : undefined);
  }
  const openPrompt = Number.isNaN(Number(_openPrompt))
    ? undefined
    : Number(_openPrompt);
  const aiPlayground = _aiPlayground ? true : false;
  const playgroundType: PlaygroundType =
    _playgroundType === 'review' ? 'review' : 'translate';

  return {
    setAiPlayground,
    setOpenPrompt,
    openPrompt,
    aiPlayground,
    playgroundType,
    setPlaygroundType,
  };
};
