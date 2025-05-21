import { usePanelData } from 'tg.views/projects/translations/ToolsPanel/usePanelData';
import { AiPrompt } from 'tg.ee';

type Props = { width: number };

export const AiPlayground = ({ width }: Props) => {
  const data = usePanelData();
  return (
    <AiPrompt
      width={width}
      project={data.project}
      language={data.language}
      keyData={data.keyData}
    />
  );
};
