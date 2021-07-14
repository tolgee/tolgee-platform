import React from 'react';

import { Editor } from 'tg.component/editor/Editor';
import { useEditableRow } from '../useEditableRow';
import { CellContent, CellPlain, CellControls } from '../CellBase';

type Props = {
  text: string;
  keyId: number;
  keyName: string;
  language: string | undefined;
  editEnabled: boolean;
};

export const CellData: React.FC<Props> = React.memo(function Cell({
  text,
  keyName,
  language,
  keyId,
  editEnabled,
}) {
  const {
    isEditing,
    value,
    setValue,
    handleEdit,
    handleEditCancel,
    handleSave,
  } = useEditableRow({ keyId, keyName, defaultVal: text, language: language });

  return (
    <CellPlain
      background={isEditing ? '#efefef' : undefined}
      onClick={
        !isEditing && editEnabled ? () => handleEdit(language) : undefined
      }
    >
      <CellContent maxHeight={isEditing ? undefined : '5em'}>
        {isEditing ? (
          <Editor
            minHeight={100}
            initialValue={value}
            variables={[]}
            onChange={(v) => setValue(v as string)}
            onSave={handleSave}
            onCancel={handleEditCancel}
            autoFocus
          />
        ) : (
          text
        )}
      </CellContent>
      <CellControls
        mode={isEditing ? 'edit' : 'view'}
        editEnabled={editEnabled}
        onCancel={handleEditCancel}
        onSave={handleSave}
        onEdit={() => handleEdit(language)}
      />
    </CellPlain>
  );
});
