import React, { useRef } from 'react';
import { useSelectionService } from 'tg.service/useSelectionService';
import { GlossaryViewTopbar } from 'tg.ee.module/glossary/components/GlossaryViewTopbar';
import { GlossaryViewToolbar } from 'tg.ee.module/glossary/components/GlossaryViewToolbar';
import { GlossaryTermsList } from 'tg.ee.module/glossary/components/GlossaryTermsList';
import { useSelectedGlossaryLanguages } from 'tg.ee.module/glossary/hooks/useSelectedGlossaryLanguages';
import { useGlossaryTerms } from 'tg.ee.module/glossary/hooks/useGlossaryTerms';

type Props = {
  onCreate?: () => void;
  onSearch?: (search: string) => void;
  search?: string;
};

export const GlossaryViewBody: React.VFC<Props> = ({
  onCreate,
  onSearch,
  search,
}) => {
  const verticalScrollRef = useRef<HTMLDivElement>(null);
  const clearSearchRef = useRef<(() => void) | null>(null);

  const [selectedLanguages, setSelectedLanguages] =
    useSelectedGlossaryLanguages();

  const { terms, total, loading, onFetchNextPageHint, getAllTermsIds } =
    useGlossaryTerms({
      search,
      languageTags: selectedLanguages,
    });

  const selectionService = useSelectionService<number>({
    totalCount: total,
    itemsAll: getAllTermsIds,
  });

  return (
    <>
      {terms && (
        <GlossaryViewTopbar
          onCreate={onCreate}
          onSearch={onSearch}
          search={search}
          selectedLanguages={selectedLanguages}
          setSelectedLanguages={setSelectedLanguages}
          clearSearchCallbackRef={clearSearchRef}
        />
      )}
      <GlossaryTermsList
        terms={terms}
        loading={loading}
        total={total}
        selectedLanguages={selectedLanguages}
        selectionService={selectionService}
        onCreate={onCreate}
        onFetchNextPageHint={onFetchNextPageHint}
        clearSearchRef={clearSearchRef}
        verticalScrollRef={verticalScrollRef}
        search={search}
      />
      <GlossaryViewToolbar
        leftOffset={verticalScrollRef.current?.getBoundingClientRect?.()?.left}
        selectionService={selectionService}
      />
    </>
  );
};
