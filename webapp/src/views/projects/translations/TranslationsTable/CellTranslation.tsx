import { useRef } from 'react';
import clsx from 'clsx';
import { components } from 'tg.service/apiSchema.generated';

import {
  CELL_CLICKABLE,
  CELL_PLAIN,
  CELL_RAISED,
  StyledCell,
} from '../cell/styles';
import { useTranslationCell } from '../useTranslationCell';
import { CellStateBar } from '../cell/CellStateBar';
import { TranslationRead } from './TranslationRead';
import { TranslationWrite } from './TranslationWrite';

type LanguageModel = components['schemas']['LanguageModel'];
type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];

type Props = {
  data: KeyWithTranslationsModel;
  language: LanguageModel;
  colIndex?: number;
  onResize?: (colIndex: number) => void;
  width?: number | string;
  active: boolean;
  lastFocusable: boolean;
  className?: string;
};

export const CellTranslation: React.FC<Props> = ({
  data,
  language,
  colIndex,
  onResize,
  width,
  active,
  lastFocusable,
  className,
}) => {
  const cellRef = useRef<HTMLDivElement>(null);

  const translation = data.translations[language.tag] as
    | TranslationViewModel
    | undefined;

  const tools = useTranslationCell({
    keyData: data,
    language: language,
    cellRef: cellRef,
  });

  const {
    isEditing,
    editEnabled: canEditTranslation,
    aiPlaygroundEnabled,
  } = tools;

  const handleResize = () => {
    onResize?.(colIndex || 0);
  };

  const state = translation?.state || 'UNTRANSLATED';

  const disabled = state === 'DISABLED';
  const editable = canEditTranslation && !disabled;

  return (
    <StyledCell
      className={clsx({
        [CELL_PLAIN]: true,
        [CELL_RAISED]: isEditing,
        [CELL_CLICKABLE]: editable && !isEditing,
        className,
      })}
      tabIndex={0}
      ref={cellRef}
      data-cy="translations-table-cell-translation"
      data-cy-lang={language.tag}
    >
      <CellStateBar state={state} onResize={handleResize} />
      {isEditing && !aiPlaygroundEnabled ? (
        <TranslationWrite tools={tools} />
      ) : (
        <TranslationRead
          active={active}
          lastFocusable={lastFocusable}
          tools={tools}
          width={width}
          colIndex={colIndex}
        />
      )}
    </StyledCell>
  );
};
