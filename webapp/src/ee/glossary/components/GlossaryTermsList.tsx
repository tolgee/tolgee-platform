import { ScrollArrows } from 'tg.ee.module/glossary/components/ScrollArrows';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';
import { Button, styled } from '@mui/material';
import { T } from '@tolgee/react';
import { GlossaryEmptyListMessage } from 'tg.ee.module/glossary/components/GlossaryEmptyListMessage';
import { GlossaryViewListHeader } from 'tg.ee.module/glossary/components/GlossaryViewListHeader';
import { ReactList } from 'tg.component/reactList/ReactList';
import {
  estimateGlossaryViewListRowHeight,
  GlossaryViewListRow,
} from 'tg.ee.module/glossary/components/GlossaryViewListRow';
import React, { useRef, useState } from 'react';
import { SelectionService } from 'tg.service/useSelectionService';
import { components } from 'tg.service/apiSchema.generated';

type SimpleGlossaryTermWithTranslationsModel =
  components['schemas']['SimpleGlossaryTermWithTranslationsModel'];

const StyledContainer = styled('div')`
  position: relative;
  display: grid;
  margin: 0px;
  border-left: 0px;
  border-right: 0px;
  background: ${({ theme }) => theme.palette.background.default};
  flex-grow: 1;

  &::before {
    content: '';
    height: 100%;
    position: absolute;
    width: 6px;
    background-image: linear-gradient(90deg, #0000002c, transparent);
    top: 0px;
    left: 0px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 100ms ease-in-out;
  }

  &::after {
    content: '';
    height: 100%;
    position: absolute;
    width: 6px;
    background-image: linear-gradient(-90deg, #0000002c, transparent);
    top: 0px;
    right: 0px;
    z-index: 10;
    pointer-events: none;
    opacity: 0;
    transition: opacity 100ms ease-in-out;
  }
`;

const StyleTermsCount = styled('div')`
  color: ${({ theme }) => theme.palette.text.secondary};
  margin-top: ${({ theme }) => theme.spacing(1)};
  margin-bottom: ${({ theme }) => theme.spacing(1)};
`;

const StyledVerticalScroll = styled('div')`
  overflow-x: auto;
  overflow-y: hidden;
  scrollbar-width: none;
  scroll-behavior: smooth;
`;

const StyledContent = styled('div')`
  position: relative;
`;

type Props = {
  terms: SimpleGlossaryTermWithTranslationsModel[];
  loading: boolean;
  total?: number;
  selectedLanguages?: string[];
  selectionService: SelectionService<number>;
  onCreate?: () => void;
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
  onCreate,
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
        <StyledContainer data-cy="translations-view-table">
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
        </StyledContainer>
      );
    }

    return (
      <StyledContainer data-cy="translations-view-table">
        <GlossaryEmptyListMessage
          loading={loading}
          onCreate={onCreate}
          onImport={undefined}
        />
      </StyledContainer>
    );
  }

  return (
    <StyledContainer data-cy="translations-view-table" ref={containerRef}>
      <ScrollArrows
        containerRef={containerRef}
        verticalScrollRef={verticalScrollRef}
        deps={[selectedLanguages]}
      />
      <StyledVerticalScroll ref={verticalScrollRef}>
        <StyledContent>
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
            scrollParentGetter={() => window}
            length={terms.length}
            useTranslate3d
            itemRenderer={renderItem}
          />
        </StyledContent>
      </StyledVerticalScroll>
    </StyledContainer>
  );
};
