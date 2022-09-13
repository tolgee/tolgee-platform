import React from 'react';
import TranslationTools, {
  Props as TranslationToolsProps,
} from './TranslationTools';
import { BottomPanel } from 'tg.component/bottomPanel/BottomPanel';

const TOOLS_BOTTOM_HEIGHT = 200;

type Props = {
  data: TranslationToolsProps['data'];
  languageTag: TranslationToolsProps['languageTag'];
};

export const ToolsBottomPanel: React.FC<Props> = ({ data, languageTag }) => {
  return (
    <BottomPanel height={TOOLS_BOTTOM_HEIGHT}>
      {(width) => (
        <TranslationTools languageTag={languageTag} width={width} data={data} />
      )}
    </BottomPanel>
  );
};
