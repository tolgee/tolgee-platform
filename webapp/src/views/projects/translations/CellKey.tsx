import React, { useRef, useState } from 'react';
import { Checkbox, styled, useTheme, Tooltip, Box } from '@mui/material';
import clsx from 'clsx';

import { Editor } from 'tg.component/editor/Editor';
import { components } from 'tg.service/apiSchema.generated';
import { LimitedHeightText } from './LimitedHeightText';
import { Tags } from './Tags/Tags';
import { useEditableRow } from './useEditableRow';
import { ScreenshotsPopover } from './Screenshots/ScreenshotsPopover';
import {
  CELL_CLICKABLE,
  CELL_PLAIN,
  CELL_RAISED,
  CELL_SELECTED,
  PositionType,
  StyledCell,
} from './cell/styles';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from './context/TranslationsContext';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useDebounce } from 'use-debounce';
import { ControlsEditor } from './cell/ControlsEditor';
import { ControlsKey } from './cell/ControlsKey';
import { TagAdd } from './Tags/TagAdd';
import { TagInput } from './Tags/TagInput';
import { getMeta } from 'tg.fixtures/isMac';
import { KeyEditModal } from './KeyEdit/KeyEditModal';
import { Bolt } from '@mui/icons-material';
import { useTranslate } from '@tolgee/react';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const StyledContainer = styled(StyledCell)`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto 1fr auto;
  grid-template-areas:
    'checkbox key          '
    '.        tags         '
    'editor   editor       '
    'controls controls     ';

  & .controls {
    grid-area: controls;
    display: flex;
    justify-content: space-between;
    overflow: hidden;
    align-items: flex-end;
  }

  & .controlsSmall {
    box-sizing: border-box;
    grid-area: controls;
    display: flex;
    justify-content: flex-end;
    overflow: hidden;
    min-height: 44px;
    padding: 12px 12px 12px 12px;
  }
`;

const StyledCheckbox = styled(Checkbox)`
  grid-area: checkbox;
  width: 38px;
  height: 38px;
  margin: 3px -9px -9px 3px;
`;

const StyledKey = styled('div')`
  grid-area: key;
  margin: 12px 12px 8px 12px;
  overflow: hidden;
  position: relative;
`;

const StyledTags = styled('div')`
  grid-area: tags;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  overflow: hidden;
  & > * {
    margin: 0px 3px 3px 0px;
  }
  margin: 0px 12px 0px 12px;
  position: relative;
  padding-bottom: 24px;
  margin-bottom: -24px;
`;

const StyledTagAdd = styled(TagAdd)`
  position: absolute;
  bottom: 0px;
  margin: 0px;
`;

const StyledTagInput = styled(TagInput)`
  position: absolute;
  bottom: 0px;
  margin: 0px;
`;

const StyledEditor = styled('div')`
  grid-area: editor;
  overflow: hidden;
  padding: 12px 12px 0px 12px;
`;

const StyledBolt = styled(Bolt)`
  position: absolute;
  bottom: 12px;
  left: 12px;
  font-size: 16px;
`;

type Props = {
  data: KeyWithTranslationsModel;
  width?: string | number;
  editEnabled: boolean;
  active: boolean;
  simple?: boolean;
  position?: PositionType;
  className?: string;
  onSaveSuccess?: (value: string) => void;
  editInDialog?: boolean;
};

export const CellKey: React.FC<Props> = ({
  data,
  width,
  editEnabled,
  active,
  simple,
  position,
  onSaveSuccess,
  editInDialog,
  className,
}) => {
  const cellRef = useRef<HTMLDivElement>(null);
  const [screenshotsOpen, setScreenshotsOpen] = useState(false);
  const { toggleSelect, addTag } = useTranslationsActions();
  const { t } = useTranslate();
  const theme = useTheme();

  const screenshotEl = useRef<HTMLButtonElement | null>(null);

  const isSelected = useTranslationsSelector((c) =>
    c.selection.includes(data.keyId)
  );

  // prevent blinking, when closing popup
  const [screenshotsOpenDebounced] = useDebounce(screenshotsOpen, 100);

  const handleToggleSelect = () => {
    toggleSelect(data.keyId);
  };

  const handleAddTag = (name: string) => {
    addTag({ keyId: data.keyId, name, onSuccess: () => setTagEdit(false) });
  };

  const [tagEdit, setTagEdit] = useState(false);

  const {
    isEditing,
    value,
    setValue,
    handleOpen,
    handleClose,
    handleSave,
    autofocus,
    editVal,
  } = useEditableRow({
    keyId: data.keyId,
    keyName: data.keyName,
    defaultVal: data.keyName,
    language: undefined,
    onSaveSuccess,
    cellRef,
  });

  const displayEditor = isEditing && !editInDialog;

  return (
    <>
      <StyledContainer
        position={position}
        className={clsx(
          {
            [CELL_PLAIN]: true,
            [CELL_CLICKABLE]: editEnabled && !displayEditor,
            [CELL_RAISED]: displayEditor,
            [CELL_SELECTED]: isEditing,
          },
          className
        )}
        style={{ width }}
        onClick={
          !displayEditor && editEnabled ? () => handleOpen('editor') : undefined
        }
        data-cy="translations-table-cell"
        tabIndex={0}
        ref={cellRef}
      >
        {!displayEditor ? (
          <>
            {!simple && (
              <StyledCheckbox
                size="small"
                checked={isSelected}
                onChange={handleToggleSelect}
                onClick={stopBubble()}
                data-cy="translations-row-checkbox"
              />
            )}
            <StyledKey>
              <LimitedHeightText width={width} maxLines={3} wrap="break-all">
                {data.keyName}
              </LimitedHeightText>
            </StyledKey>
            {!simple && (
              <StyledTags>
                <Tags
                  keyId={data.keyId}
                  tags={data.keyTags}
                  deleteEnabled={editEnabled}
                />
                {editEnabled &&
                  (tagEdit ? (
                    <StyledTagInput
                      onAdd={handleAddTag}
                      onClose={() => setTagEdit(false)}
                      autoFocus
                    />
                  ) : (
                    active && (
                      <StyledTagAdd
                        onClick={() => setTagEdit(true)}
                        withFullLabel={!data.keyTags?.length}
                      />
                    )
                  ))}
              </StyledTags>
            )}
            {data.contextPresent && (
              <Box role="button" onClick={() => handleOpen('context')}>
                <Tooltip title={t('key-context-present-hint')}>
                  <StyledBolt />
                </Tooltip>
              </Box>
            )}
          </>
        ) : (
          <StyledEditor>
            <Editor
              plaintext
              value={value}
              onChange={(v) => setValue(v as string)}
              autofocus={autofocus}
              onSave={() => handleSave()}
              onCancel={() => handleClose(true)}
              shortcuts={{
                [`${getMeta()}-Enter`]: () => handleSave('EDIT_NEXT'),
              }}
              background={theme.palette.cell.selected}
            />
          </StyledEditor>
        )}

        <div className={displayEditor ? 'controls' : 'controlsSmall'}>
          {displayEditor ? (
            <ControlsEditor
              onCancel={() => handleClose(true)}
              onSave={handleSave}
              onScreenshots={
                simple ? undefined : () => setScreenshotsOpen(true)
              }
              screenshotRef={screenshotEl}
              screenshotsPresent={data.screenshotCount > 0}
            />
          ) : !tagEdit ? (
            active || screenshotsOpen || screenshotsOpenDebounced ? (
              <ControlsKey
                onEdit={() => handleOpen('editor')}
                onScreenshots={
                  simple ? undefined : () => setScreenshotsOpen(true)
                }
                screenshotRef={screenshotEl}
                screenshotsPresent={data.screenshotCount > 0}
                screenshotsOpen={screenshotsOpen || screenshotsOpenDebounced}
                editEnabled={editEnabled}
              />
            ) : (
              // hide as many components as possible in order to be performant
              <ControlsKey
                onScreenshots={
                  data.screenshotCount > 0
                    ? () => setScreenshotsOpen(true)
                    : undefined
                }
                screenshotRef={screenshotEl}
                screenshotsPresent={data.screenshotCount > 0}
                editEnabled={editEnabled}
              />
            )
          ) : null}
        </div>
      </StyledContainer>
      {screenshotsOpen && (
        <ScreenshotsPopover
          anchorEl={screenshotEl.current!}
          keyId={data.keyId}
          onClose={() => {
            setScreenshotsOpen(false);
          }}
        />
      )}
      {isEditing && editInDialog && (
        <KeyEditModal
          keyId={data.keyId}
          name={data.keyName}
          tags={data.keyTags.map((k) => k.name)}
          namespace={data.keyNamespace}
          onClose={() => handleClose(true)}
          initialTab={editVal?.mode === 'context' ? 'context' : 'general'}
          contextPresent={data.contextPresent}
        />
      )}
    </>
  );
};
