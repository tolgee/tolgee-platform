import { FC } from 'react';
import { KeyHeader, KeyPanel } from './KeyPanelBase';
import { KeyTranslations } from './KeyTranslations';
import { SimpleCellKey } from 'tg.views/projects/translations/SimpleCellKey';

export const SingleKeyPanel: FC<{ keyData: any }> = ({ keyData }) => (
  <KeyPanel>
    <KeyHeader>
      <SimpleCellKey data={keyData} />
    </KeyHeader>
    <KeyTranslations keyData={keyData} />
  </KeyPanel>
);
