import React, { useRef, useState } from 'react';
import { useContextSelector } from 'use-context-selector';
import { Checkbox, Box } from '@material-ui/core';

import { Editor } from 'tg.component/editor/Editor';
import { useEditableRow } from './useEditableRow';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from './TranslationsContext';
import { ScreenshotsPopover } from './Screenshots/ScreenshotsPopover';
import { CellContent, CellPlain, CellControls, stopBubble } from './CellBase';

type Props = {
  text: string;
  keyId: number;
  keyName: string;
  screenshotCount: number;
  editEnabled: boolean;
};

export const CellKey: React.FC<Props> = React.memo(function Cell({
  text,
  keyName,
  keyId,
  screenshotCount,
  editEnabled,
}) {
  const [screenshotsOpen, setScreenshotsOpen] = useState(false);

  const screenshotEl = useRef<HTMLButtonElement | null>(null);

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
    defaultVal: keyName,
    language: undefined,
  });

  const isSelected = useContextSelector(TranslationsContext, (c) =>
    c.selection.includes(keyId)
  );

  const dispatch = useTranslationsDispatch();

  const toggleSelect = () => {
    dispatch({ type: 'TOGGLE_SELECT', payload: keyId });
  };

  return (
    <>
      <CellPlain
        background={isEditing ? '#efefef' : undefined}
        onClick={
          !isEditing && editEnabled ? () => handleEdit(undefined) : undefined
        }
      >
        <CellContent>
          {isEditing ? (
            <>
              <Editor
                minHeight={100}
                initialValue={value}
                variables={[]}
                onChange={(v) => setValue(v as string)}
                onSave={handleSave}
                onCancel={handleEditCancel}
                language="plaintext"
                autoFocus
              />
            </>
          ) : (
            <Box display="flex" alignItems="baseline">
              {editEnabled && (
                <Box margin={-1} onClick={stopBubble()}>
                  <Checkbox
                    size="small"
                    checked={isSelected}
                    onChange={toggleSelect}
                    data-cy="translations-row-checkbox"
                  />
                </Box>
              )}
              <Box overflow="hidden" textOverflow="ellipsis">
                {text}
              </Box>
            </Box>
          )}
        </CellContent>
        <CellControls
          mode={isEditing ? 'edit' : 'view'}
          onEdit={() => handleEdit(undefined)}
          onCancel={handleEditCancel}
          onSave={handleSave}
          onScreenshots={() => setScreenshotsOpen(true)}
          screenshotRef={screenshotEl}
          screenshotsPresent={screenshotCount > 0}
          editEnabled={editEnabled}
        />
      </CellPlain>
      {screenshotsOpen && (
        <ScreenshotsPopover
          anchorEl={screenshotEl.current!}
          keyId={keyId}
          onClose={() => {
            setScreenshotsOpen(false);
          }}
        />
      )}
    </>
  );
});
