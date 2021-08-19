import { stopBubble } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import { useTranslationsDispatch } from '../context/TranslationsContext';
import { Tag } from './Tag';

type TagModel = components['schemas']['TagModel'];

type Props = {
  keyId: number;
  tags: TagModel[] | null;
  deleteEnabled: boolean;
};

export const Tags: React.FC<Props> = ({ tags, keyId, deleteEnabled }) => {
  const dispatch = useTranslationsDispatch();

  const handleTagDelete = (tagId: number) => {
    dispatch({
      type: 'REMOVE_TAG',
      payload: { keyId, tagId },
    });
  };

  return (
    <>
      {tags?.map((t) => (
        <Tag
          key={t.id}
          name={t.name}
          onDelete={
            deleteEnabled ? stopBubble(() => handleTagDelete(t.id)) : undefined
          }
        />
      ))}
    </>
  );
};
