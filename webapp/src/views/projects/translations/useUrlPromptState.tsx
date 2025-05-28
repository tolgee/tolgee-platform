import { QUERY } from 'tg.constants/links';
import { useUrlSearchState } from 'tg.hooks/useUrlSearchState';

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
  function setAiPlayground(value: boolean | undefined) {
    _setAiPlayground(value ? '1' : undefined);
  }
  function setOpenPrompt(value: number | undefined) {
    _setOpenPrompt(typeof value === 'number' ? String(value) : undefined);
  }
  const openPrompt = Number.isNaN(Number(_openPrompt))
    ? undefined
    : Number(_openPrompt);
  const aiPlayground = _aiPlayground ? true : false;

  return { setAiPlayground, setOpenPrompt, openPrompt, aiPlayground };
};
