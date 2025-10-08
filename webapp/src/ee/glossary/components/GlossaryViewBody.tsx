import React, { useRef } from 'react';
import { useSelectionService } from 'tg.service/useSelectionService';
import { GlossaryViewTopbar } from 'tg.ee.module/glossary/components/GlossaryViewTopbar';
import { GlossaryViewToolbar } from 'tg.ee.module/glossary/components/GlossaryViewToolbar';
import { GlossaryTermsList } from 'tg.ee.module/glossary/components/GlossaryTermsList';
import { useSelectedGlossaryLanguages } from 'tg.ee.module/glossary/hooks/useSelectedGlossaryLanguages';
import { useGlossaryTerms } from 'tg.ee.module/glossary/hooks/useGlossaryTerms';
import { useGlossaryTermCreateDialog } from 'tg.ee.module/glossary/hooks/useGlossaryTermCreateDialog';
import { useGlossaryImportDialog } from 'tg.ee.module/glossary/hooks/useGlossaryImportDialog';

type Props = {
  onSearch?: (search: string) => void;
  search?: string;
};

export const GlossaryViewBody: React.VFC<Props> = ({ onSearch, search }) => {
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

  const { onCreateTerm, createTermDialog } = useGlossaryTermCreateDialog();

  const hasExistingTerms = total !== undefined && total > 0;
  const { onImport, importDialog } = useGlossaryImportDialog(hasExistingTerms);

  return (
    <>
      {createTermDialog}
      {importDialog}
      {terms && (
        <GlossaryViewTopbar
          onCreateTerm={onCreateTerm}
          onImport={onImport}
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
        onCreateTerm={onCreateTerm}
        onImport={onImport}
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
