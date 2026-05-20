import { Link } from '@mui/material';
import React from 'react';
import EmptyImage from 'tg.svgs/icons/glossary-empty.svg?react';
import { EmptyResourceMessage } from 'tg.component/common/EmptyResourceMessage';

export type AddFirstGlossaryMessageProps = {
  height?: string;
  onCreateClick?: () => void;
};

export const AddFirstGlossaryMessage: React.VFC<
  AddFirstGlossaryMessageProps
> = ({ height, onCreateClick }) => {
  return (
    <EmptyResourceMessage
      title={{
        keyName: 'glossaries_list_empty_title',
      }}
      message={{
        keyName: 'glossaries_list_empty_message',
        params: {
          bestPractice: (
            <Link href="https://docs.tolgee.io/platform/glossaries/managing_glossaries" />
          ),
        },
      }}
      image={<EmptyImage />}
      imageHeight={height}
      button={{
        keyName: 'glossaries_add_first_button',
        dataCy: 'glossaries-empty-add-button',
        onClick: onCreateClick,
      }}
    />
  );
};
