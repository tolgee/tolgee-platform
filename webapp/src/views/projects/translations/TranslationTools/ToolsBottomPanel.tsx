import React from 'react';
import TranslationTools, {
  Props as TranslationToolsProps,
} from './TranslationTools';
import { BottomPanel } from 'tg.component/bottomPanel/BottomPanel';

const TOOLS_BOTTOM_HEIGHT = 200;

type Props = {
  data: TranslationToolsProps['data'];
};

export const ToolsPottomPanel: React.FC<Props> = ({ data }) => {
  return (
    <BottomPanel height={TOOLS_BOTTOM_HEIGHT}>
      {(width) => <TranslationTools width={width} data={data} />}
    </BottomPanel>
  );
};
