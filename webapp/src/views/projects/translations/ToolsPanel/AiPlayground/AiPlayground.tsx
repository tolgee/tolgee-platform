import { usePanelData } from '../usePanelData';
import { AiPrompt } from './AiPrompt';

export const AiPlayground = () => {
  const data = usePanelData();
  return <AiPrompt {...data} setItemsCount={() => {}} />;
};
