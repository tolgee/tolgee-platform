import { stopBubble } from 'tg.fixtures/eventHandler';
import { components } from 'tg.service/apiSchema.generated';
import {
  useTranslationsSelector,
  useTranslationsActions,
} from '../context/TranslationsContext';
import { Tag } from './Tag';

type TagModel = components['schemas']['TagModel'];

type Props = {
  keyId: number;
  tags: TagModel[] | null;
  deleteEnabled: boolean;
};

export const Tags: React.FC<Props> = ({ tags, keyId, deleteEnabled }) => {
  const { removeTag, removeFilter, addFilter } = useTranslationsActions();
  const filters = useTranslationsSelector((c) => c.filters);

  const handleTagDelete = (tagId: number) => {
    removeTag({ keyId, tagId });
  };

  const handleTagClick = (tagName: string) => {
    if (filters.filterTag?.includes(tagName)) {
      removeFilter('filterTag', tagName);
    } else {
      addFilter('filterTag', tagName);
    }
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
