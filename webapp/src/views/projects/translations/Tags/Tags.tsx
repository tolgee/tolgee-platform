import { stopBubble } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import { useContextSelector } from 'use-context-selector';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { Tag } from './Tag';

type TagModel = components['schemas']['TagModel'];

type Props = {
  keyId: number;
  tags: TagModel[] | null;
  deleteEnabled: boolean;
};

export const Tags: React.FC<Props> = ({ tags, keyId, deleteEnabled }) => {
  const dispatch = useTranslationsDispatch();
  const filters = useContextSelector(TranslationsContext, (c) => c.filters);

  const handleTagDelete = (tagId: number) => {
    dispatch({
      type: 'REMOVE_TAG',
      payload: { keyId, tagId },
    });
  };

  const handleTagClick = (tagName: string) => {
    dispatch({
      type: 'SET_FILTERS',
      payload: {
        ...filters,
        filterTag: filters['filterTag'] === tagName ? undefined : tagName,
      },
    });
  };

  return (
    <>
      {tags?.map((t) => (
        <Tag
          key={t.id}
          name={t.name}
          selected={filters['filterTag'] === t.name}
          onDelete={
            deleteEnabled ? stopBubble(() => handleTagDelete(t.id)) : undefined
          }
          onClick={handleTagClick}
        />
      ))}
    </>
  );
};
