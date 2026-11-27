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
  const message = onCreateClick
    ? {
        keyName: 'glossaries_list_empty_message',
        params: {
          bestPractice: (
            <Link href="https://docs.tolgee.io/platform/glossaries/managing_glossaries" />
          ),
        },
      }
    : {
        // @tolgee-key glossaries_list_empty_no_create_permission
        keyName: 'glossaries_list_empty_no_create_permission',
        defaultValue:
          'You need permission to create glossaries. Ask an organization Owner or Maintainer to create one or update your role.',
      };

  return (
    <EmptyResourceMessage
      title={{
        keyName: 'glossaries_list_empty_title',
      }}
      message={message}
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
