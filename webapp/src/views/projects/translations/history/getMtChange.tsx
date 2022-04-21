import { AutoTranslationIcon } from '../cell/AutoTranslationIcon';

import { DiffInput } from './types';

export const getMtChange = (input?: DiffInput) => {
  if (input?.new) {
    return <AutoTranslationIcon provider={input.new} />;
  }
};
