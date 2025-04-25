import { Box, Button, Portal, styled, useMediaQuery } from '@mui/material';
import { SecondaryBarSearchField } from 'tg.component/layout/SecondaryBarSearchField';
import { GlossaryViewLanguageSelect } from 'tg.ee.module/glossary/components/GlossaryViewLanguageSelect';
import { BaseViewAddButton } from 'tg.component/layout/BaseViewAddButton';
import clsx from 'clsx';
import { ChevronLeft, ChevronRight } from '@untitled-ui/icons-react';
import { T, useTranslate } from '@tolgee/react';
import { GlossaryViewListHeader } from 'tg.ee.module/glossary/components/GlossaryViewListHeader';
import { ReactList } from 'tg.component/reactList/ReactList';
import { GlossaryViewListRow } from 'tg.ee.module/glossary/components/GlossaryViewListRow';
import React, { useRef, useState } from 'react';
import { components } from 'tg.service/apiSchema.generated';
import { useResizeObserver } from 'usehooks-ts';
import { useScrollStatus } from 'tg.component/common/useScrollStatus';
import { GlossaryBatchToolbar } from 'tg.ee.module/glossary/components/GlossaryBatchToolbar';
import { useSelectionService } from 'tg.service/useSelectionService';
import { EmptyListMessage } from 'tg.component/common/EmptyListMessage';

type GlossaryTermWithTranslationsModel =
  components['schemas']['GlossaryTermWithTranslationsModel'];

const ARROW_SIZE = 50;

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

  &.scrollLeft {
    &::before {
      opacity: 1;
    }
  }

  &.scrollRight {
    &::after {
      opacity: 1;
    }
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

const StyledContainerInner = styled(Box)`
  display: grid;
  width: 100%;
  margin: 0px auto;
  margin-top: 0px;
  margin-bottom: 0px;
`;

const StyledScrollArrow = styled('div')`
  position: fixed;
  top: 50vh;
  width: ${ARROW_SIZE / 2}px;
  height: ${ARROW_SIZE}px;
  z-index: 5;
  cursor: pointer;
  border: 1px solid ${({ theme }) => theme.palette.divider1};
  background: ${({ theme }) => theme.palette.background.default};
  opacity: 0;
  transition: opacity 150ms ease-in-out;
  pointer-events: none;

  display: flex;
  align-items: center;
  justify-content: center;

  &.right {
    border-radius: ${ARROW_SIZE}px 0px 0px ${ARROW_SIZE}px;
    padding-left: 4px;
    border-right: none;
  }

  &.left {
    border-radius: 0px ${ARROW_SIZE}px ${ARROW_SIZE}px 0px;
    padding-right: 4px;
    border-left: none;
  }

  &.scrollLeft {
    opacity: 1;
    pointer-events: all;
  }

  &.scrollRight {
    opacity: 1;
    pointer-events: all;
  }
`;

const StyledBatchToolbarWrapper = styled(Box)`
  position: fixed;
  bottom: 0;
  z-index: ${({ theme }) => theme.zIndex.drawer};
`;

type Props = {
  organizationId: number;
  glossaryId: number;
  loading?: boolean;
  data?: GlossaryTermWithTranslationsModel[];
  fetchDataIds: () => Promise<number[]>;
  totalElements?: number;
  baseLanguage?: string;
  selectedLanguages?: string[];
  selectedLanguagesWithBaseLanguage?: string[];
  updateSelectedLanguages: (languages: string[]) => void;
  onFetchNextPage?: () => void;
  onCreate?: () => void;
  onSearch?: (search: string) => void;
};

export const GlossaryViewBody: React.VFC<Props> = ({
  organizationId,
  glossaryId,
  loading,
  data = [],
  fetchDataIds,
  totalElements,
  baseLanguage,
  selectedLanguages,
  selectedLanguagesWithBaseLanguage,
  updateSelectedLanguages,
  onFetchNextPage,
  onCreate,
  onSearch,
}) => {
  const tableRef = useRef<HTMLDivElement>(null);
  const verticalScrollRef = useRef<HTMLDivElement>(null);
  const reactListRef = useRef<ReactList>(null);
  const clearSearchRef = useRef<(() => void) | null>(null);

  const { t } = useTranslate();

  const [editingTranslation, setEditingTranslation] = useState<
    [number | undefined, string | undefined]
  >([undefined, undefined]);

  const selectionService = useSelectionService<number>({
    totalCount: totalElements,
    itemsAll: fetchDataIds,
  });

  const [tablePosition, setTablePosition] = useState({ left: 0, right: 0 });
  useResizeObserver({
    ref: tableRef,
    onResize: () => {
      const position = tableRef.current?.getBoundingClientRect();
      if (position) {
        const left = position?.left;
        const right = window.innerWidth - position?.right;
        setTablePosition({ left, right });
      }
    },
  });
  const hasMinimalHeight = useMediaQuery('(min-height: 400px)');

  const [scrollLeft, scrollRight] = useScrollStatus(verticalScrollRef, [
    selectedLanguages,
    tablePosition,
  ]);

  const handleScroll = (direction: 'left' | 'right') => {
    const element = verticalScrollRef.current;
    if (element) {
      const position = element.scrollLeft;
      element.scrollTo({
        left: position + (direction === 'left' ? -350 : +350),
      });
    }
  };

  const renderItem = (index: number) => {
    const row = data[index];
    const isLast = index === data.length - 1;
    if (isLast) {
      onFetchNextPage?.();
    }

    return (
      <GlossaryViewListRow
        key={row.id}
        organizationId={organizationId}
        glossaryId={glossaryId}
        item={row}
        baseLanguage={baseLanguage}
        onEditTranslation={(termId, languageTag) => {
          setEditingTranslation([termId, languageTag]);
        }}
        editingTranslation={editingTranslation}
        selectedLanguages={selectedLanguages}
        selectionService={selectionService}
      />
    );
  };

  return (
    <>
      {data && (
        <Box>
          <StyledContainerInner>
            <Box display="flex" justifyContent="space-between">
              <Box display="flex" alignItems="center" gap="8px">
                <Box>
                  <SecondaryBarSearchField
                    onSearch={onSearch}
                    placeholder={t('glossary_search_placeholder')}
                    clearCallbackRef={clearSearchRef}
                  />
                </Box>
              </Box>
              <Box display="flex" gap={2}>
                <GlossaryViewLanguageSelect
                  organizationId={organizationId}
                  glossaryId={glossaryId}
                  value={selectedLanguagesWithBaseLanguage}
                  onValueChange={updateSelectedLanguages}
                  sx={{
                    width: '250px',
                  }}
                />
                <BaseViewAddButton
                  onClick={onCreate}
                  label={t('glossary_add_button')}
                />
              </Box>
            </Box>
          </StyledContainerInner>
        </Box>
      )}

      <StyledContainer
        data-cy="translations-view-table"
        className={clsx({ scrollLeft, scrollRight })}
        ref={tableRef}
      >
        {hasMinimalHeight && (
          <Portal>
            <StyledScrollArrow
              className={clsx('right', { scrollRight })}
              style={{
                right: tablePosition?.right,
              }}
              onClick={() => handleScroll('right')}
            >
              <ChevronRight width={20} height={20} />
            </StyledScrollArrow>
            <StyledScrollArrow
              className={clsx('left', { scrollLeft })}
              style={{
                left: tablePosition?.left,
              }}
              onClick={() => handleScroll('left')}
            >
              <ChevronLeft width={20} height={20} />
            </StyledScrollArrow>
          </Portal>
        )}
        {data.length === 0 ? (
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
        ) : (
          <StyledVerticalScroll ref={verticalScrollRef}>
            <StyledContent>
              <StyleTermsCount>
                <T
                  keyName="glossary_view_terms_count"
                  params={{
                    count: totalElements ?? 0,
                  }}
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
                itemSizeEstimator={(index, cache) => {
                  return cache[index] || 84; // TODO: different size based on if item contains description and flags?
                }}
                // @ts-ignore
                scrollParentGetter={() => window}
                length={data.length}
                useTranslate3d
                itemRenderer={renderItem}
              />
            </StyledContent>
          </StyledVerticalScroll>
        )}
        <Portal>
          <StyledBatchToolbarWrapper
            sx={{
              left: verticalScrollRef.current?.getBoundingClientRect?.()?.left,
            }}
          >
            <GlossaryBatchToolbar
              organizationId={organizationId}
              glossaryId={glossaryId}
              selectionService={selectionService}
            />
          </StyledBatchToolbarWrapper>
        </Portal>
      </StyledContainer>
    </>
  );
};
