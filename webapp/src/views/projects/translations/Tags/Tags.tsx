import { stopBubble } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import {
  useTranslationsSelector,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { encodeFilter, toggleFilter } from '../Filters/useAvailableFilters';
import { Tag } from './Tag';

type TagModel = components['schemas']['TagModel'];

type Props = {
  keyId: number;
  tags: TagModel[] | null;
  deleteEnabled: boolean;
};

export const Tags: React.FC<Props> = ({ tags, keyId, deleteEnabled }) => {
  const dispatch = useTranslationsDispatch();
  const filters = useTranslationsSelector((c) => c.filters);

  const handleTagDelete = (tagId: number) => {
    dispatch({
      type: 'REMOVE_TAG',
      payload: { keyId, tagId },
    });
  };

  const handleTagClick = (tagName: string) => {
    const newFilters = toggleFilter(
      filters,
      [],
      encodeFilter({
        filter: 'filterTag',
        value: tagName,
      })
    );
    dispatch({
      type: 'SET_FILTERS',
      payload: newFilters,
    });
  };

  return (
    <>
      {tags?.map((t) => (
        <Tag
          key={t.id}
          name={t.name}
          selected={Boolean(filters['filterTag']?.includes(t.name))}
          onDelete={
            deleteEnabled ? stopBubble(() => handleTagDelete(t.id)) : undefined
          }
          onClick={handleTagClick}
        />
      ))}
    </>
  );
};
