import React from 'react';

import { components } from 'tg.service/apiSchema.generated';
import { Editor } from 'tg.component/editor/Editor';
import { useEditableRow } from '../useEditableRow';
import { CellContent, CellPlain, CellControls, StateType } from '../CellBase';
import { TranslationVisual } from '../TranslationVisual';
import { useTranslationsDispatch } from '../context/TranslationsContext';

type TranslationModel = components['schemas']['TranslationModel'];

type Props = {
  keyId: number;
  keyName: string;
  language: string;
  editEnabled: boolean;
  width: number;
  locale: string;
  translation?: TranslationModel;
  colIndex: number;
  onResize: (colIndex: number) => void;
};

export const CellData: React.FC<Props> = React.memo(function Cell({
  keyName,
  language,
  keyId,
  editEnabled,
  locale,
  translation,
  onResize,
  colIndex,
}) {
  const {
    isEditing,
    value,
    setValue,
    handleEdit,
    handleEditCancel,
    handleSave,
  } = useEditableRow({
    keyId,
    keyName,
    defaultVal: translation?.text || '',
    language: language,
  });
  const dispatch = useTranslationsDispatch();

  const handleStateChange = (state: StateType) => {
    dispatch({
      type: 'SET_TRANSLATION_STATE',
      payload: {
        keyId,
        translationId: translation?.id as number,
        language: language as string,
        state,
      },
    });
  };

  return (
    <CellPlain
      state={translation?.state || 'UNTRANSLATED'}
      background={isEditing ? '#efefef' : undefined}
      onClick={
        !isEditing && editEnabled ? () => handleEdit(language) : undefined
      }
      onResize={() => onResize(colIndex)}
    >
      <CellContent>
        {isEditing ? (
          <Editor
            initialValue={value}
            onChange={(v) => setValue(v as string)}
            background="#efefef"
            onSave={() => handleSave('DOWN')}
            onCancel={handleEditCancel}
          />
        ) : (
          <TranslationVisual
            locale={locale}
            maxLines={3}
            wrapVariants={true}
            text={translation?.text}
          />
        )}
      </CellContent>
      <CellControls
        absolute
        mode={isEditing ? 'edit' : 'view'}
        state={translation?.state}
        editEnabled={editEnabled}
        onCancel={handleEditCancel}
        onSave={handleSave}
        onEdit={() => handleEdit(language)}
        onStateChange={handleStateChange}
      />
    </CellPlain>
  );
});
