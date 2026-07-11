import { useApiQuery } from 'tg.service/http/useQueryApi';
import { messageService } from 'tg.service/MessageService';
import { TranslatedError } from 'tg.translationTools/TranslatedError';
import { PromptItem } from './PromptLoadMenu';
import { usePromptUrlState } from 'tg.views/projects/translations/useUrlPromptState';

type Props = {
  projectId: number;
};

export const useOpenedPrompt = ({ projectId }: Props) => {
  const { openPrompt, setOpenPrompt } = usePromptUrlState();

  const defaultPromptLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/prompts/default',
    method: 'get',
    path: {
      projectId,
    },
  });

  const openPromptLoadable = useApiQuery({
    url: '/v2/projects/{projectId}/prompts/{promptId}',
    method: 'get',
    path: {
      projectId,
      promptId: openPrompt!,
    },
    fetchOptions: {
      disable404Redirect: true,
    },
    options: {
      enabled: openPrompt !== undefined,
      onError(err) {
        if (err.code === 'prompt_not_found') {
          setOpenPrompt(undefined);
          messageService.error(<TranslatedError code={err.code} />);
        }
      },
    },
  });

  const isLoading =
    defaultPromptLoadable.isLoading || openPromptLoadable.isLoading;

  if (isLoading) {
    return {
      isLoading: true,
      data: undefined,
      defaultTemplate: undefined,
    };
  }

  return {
    isLoading,
    data: (typeof openPrompt === 'number'
      ? openPromptLoadable.data
      : defaultPromptLoadable.data) as PromptItem | undefined,
    defaultTemplate: defaultPromptLoadable.data?.template,
  };
};
