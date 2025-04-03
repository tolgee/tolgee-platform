import { usePanelData } from 'tg.views/projects/translations/ToolsPanel/usePanelData';
import { AiPrompt } from 'tg.ee';

export const AiPlayground = () => {
  const data = usePanelData();
  return <AiPrompt {...data} setItemsCount={() => {}} />;
};
