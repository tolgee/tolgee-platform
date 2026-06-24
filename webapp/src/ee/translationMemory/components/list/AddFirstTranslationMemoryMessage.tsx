import React from 'react';
import { EmptyResourceMessage } from 'tg.component/common/EmptyResourceMessage';

export type AddFirstTranslationMemoryMessageProps = {
  onCreateClick?: () => void;
};

export const AddFirstTranslationMemoryMessage: React.VFC<
  AddFirstTranslationMemoryMessageProps
> = ({ onCreateClick }) => {
  return (
    <EmptyResourceMessage
      title={{
        keyName: 'translation_memories_list_empty_title',
        defaultValue: 'No translation memories yet',
      }}
      message={{
        keyName: 'translation_memories_list_empty_message',
        defaultValue:
          'Create a translation memory to reuse existing translations across projects.',
      }}
      button={{
        keyName: 'translation_memories_add_first_button',
        defaultValue: 'New translation memory',
        dataCy: 'translation-memories-empty-add-button',
        onClick: onCreateClick,
      }}
    />
  );
};
