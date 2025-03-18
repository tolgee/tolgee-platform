import { usePanelData } from '../usePanelData';
import { AiPrompt } from './AiPrompt';

export const AiPlayground = () => {
  const data = usePanelData();
  if (!data.keyData || !data.language) {
    return null;
  }
  return <AiPrompt {...data} setItemsCount={() => {}} />;
};
