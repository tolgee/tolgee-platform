import { ScrollArrows } from 'tg.component/entriesList/ScrollArrows';
import {
  Container as ListContainer,
  Content as ListContent,
  VerticalScroll as ListVerticalScroll,
} from 'tg.component/entriesList/entriesListChrome';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { Button, styled, useTheme } from '@mui/material';
import { T } from '@tolgee/react';
import { GlossaryEmptyListMessage } from 'tg.ee.module/glossary/components/GlossaryEmptyListMessage';
import { GlossaryViewListHeader } from 'tg.ee.module/glossary/components/GlossaryViewListHeader';
import { ReactList } from 'tg.component/reactList/ReactList';
import {
  estimateGlossaryViewListRowHeight,
  GlossaryViewListRow,
} from 'tg.ee.module/glossary/components/GlossaryViewListRow';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { SelectionService } from 'tg.service/useSelectionService';
import { components } from 'tg.service/apiSchema.generated';
import { useResizeObserver } from 'usehooks-ts';

type SimpleGlossaryTermWithTranslationsModel =
  components['schemas']['SimpleGlossaryTermWithTranslationsModel'];

const StyleTermsCount = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-top: ${({ theme }) => theme.spacing(0.5)};
  margin-bottom: ${({ theme }) => theme.spacing(1)};
`;

type Props = {
  terms: SimpleGlossaryTermWithTranslationsModel[];
  loading: boolean;
  total?: number;
  selectedLanguages?: string[];
  selectionService: SelectionService<number>;
  onCreateTerm?: () => void;
  onImport?: () => void;
  onFetchNextPageHint?: () => void;
  clearSearchRef: React.RefObject<(() => void) | undefined>;
  verticalScrollRef: React.MutableRefObject<HTMLDivElement | null>;
  search?: string;
};

export const GlossaryTermsList = ({
  terms,
  loading,
  total,
  selectedLanguages,
  selectionService,
  onCreateTerm,
  onImport,
  onFetchNextPageHint,
  clearSearchRef,
  verticalScrollRef,
  search,
}: Props) => {
  const containerRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);

  const [editingTranslation, setEditingTranslation] = useState<
    [number | undefined, string | undefined]
  >([undefined, undefined]);

  const [tableHeight, setTableHeight] = useState(600);
  const theme = useTheme();

  const onResize = useCallback(() => {
    const position = verticalScrollRef.current?.getBoundingClientRect();
    if (position) {
      const bottomSpacing = parseInt(theme.spacing(2), 10);
      // This is very fragile. We need to find a better way of stretching
      // the table to fill the view vertically.
      setTableHeight(window.innerHeight - position.top - bottomSpacing);
    }
  }, [theme]);

  const verticalScrollRefCallback = useCallback(
    (node) => {
      verticalScrollRef.current = node;
      onResize();
    },
    [onResize]
  );

  useEffect(() => {
    onResize();
    window.addEventListener('resize', onResize);
    return () => window.removeEventListener('resize', onResize);
  }, [onResize]);

  useResizeObserver({ ref: verticalScrollRef, onResize });

  const renderItem = (index: number) => {
    const row = terms[index];
    const isLast = index === terms.length - 1;
    if (isLast) {
      onFetchNextPageHint?.();
    }

    return (
      <GlossaryViewListRow
        key={row.id}
        item={row}
        onEditTranslation={(termId, languageTag) => {
          setEditingTranslation([termId, languageTag]);
        }}
        editingTranslation={editingTranslation}
        selectedLanguages={selectedLanguages}
        selectionService={selectionService}
      />
    );
  };

  if (terms.length === 0) {
    if (search !== undefined && search.length > 0) {
      return (
        <ListContainer data-cy="translations-view-table">
          <EmptyListMessage
            loading={loading}
            hint={
              <Button
                onClick={clearSearchRef.current ?? undefined}
                color="primary"
              >
                <T keyName="glossary_terms_nothing_found_action" />
              </Button>
            }
          >
            <T keyName="glossary_terms_nothing_found" />
          </EmptyListMessage>
        </ListContainer>
      );
    }

    return (
      <ListContainer data-cy="translations-view-table">
        <GlossaryEmptyListMessage
          loading={loading}
          onCreateTerm={onCreateTerm}
          onImport={onImport}
        />
      </ListContainer>
    );
  }

  return (
    <ListContainer data-cy="translations-view-table" ref={containerRef}>
      <ScrollArrows
        containerRef={containerRef}
        verticalScrollRef={verticalScrollRef}
        deps={[selectedLanguages]}
      />
      <ListVerticalScroll
        ref={verticalScrollRefCallback}
        style={{ height: tableHeight }}
      >
        <ListContent>
          <StyleTermsCount>
            <T
              keyName="glossary_view_terms_count"
              params={{ count: total ?? 0 }}
            />
          </StyleTermsCount>

          <GlossaryViewListHeader
            selectedLanguages={selectedLanguages}
            selectionService={selectionService}
          />

          <ReactList
            ref={reactListRef}
            threshold={800}
            type="variable"
            itemSizeEstimator={(index, cache) =>
              cache[index] || estimateGlossaryViewListRowHeight(terms[index])
            }
            // @ts-ignore
            scrollParentGetter={() => verticalScrollRef.current ?? window}
            length={terms.length}
            useTranslate3d
            itemRenderer={renderItem}
          />
        </ListContent>
      </ListVerticalScroll>
    </ListContainer>
  );
};
