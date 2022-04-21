import { AutoTranslationIcon } from '../cell/AutoTranslationIcon';

import { DiffInput } from './types';

export const getAutoChange = (input?: DiffInput<boolean>) => {
  if (input?.new) {
    return <AutoTranslationIcon />;
  }
};
