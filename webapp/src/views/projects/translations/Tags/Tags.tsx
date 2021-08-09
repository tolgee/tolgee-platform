import { useState } from 'react';
import { Box } from '@material-ui/core';

import { stopBubble } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { Tag } from './Tag';
import { TagInput } from './TagInput';
import { TagAdd } from './TagAdd';

type TagModel = components['schemas']['TagModel'];

type Props = {
  keyId: number;
  tags: TagModel[] | null;
};

export const Tags: React.FC<Props> = ({ tags, keyId }) => {
  const dispatch = useTranslationsDispatch();
  const [edit, setEdit] = useState(false);

  const handleTagAdd = (name: string) => {
    dispatch({
      type: 'ADD_TAG',
      payload: { keyId, name },
      onSuccess: () => setEdit(false),
    });
  };

  const handleTagDelete = (tagId: number) => {
    dispatch({
      type: 'REMOVE_TAG',
      payload: { keyId, tagId },
    });
  };

  return (
    <Box display="flex" flexDirection="column" alignItems="flex-start">
      {tags?.map((t) => (
        <Tag
          key={t.id}
          name={t.name}
          onDelete={stopBubble(() => handleTagDelete(t.id))}
        />
      ))}

      {edit ? (
        <TagInput onClose={() => setEdit(false)} onAdd={handleTagAdd} />
      ) : (
        <TagAdd withFullLabel={!tags?.length} onClick={() => setEdit(true)} />
      )}
    </Box>
  );
};
