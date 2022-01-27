import React, { useState, useRef } from 'react';
import { Checkbox, makeStyles } from '@material-ui/core';
import clsx from 'clsx';

import { Editor } from 'tg.component/editor/Editor';
import { components } from 'tg.service/apiSchema.generated';
import { LimitedHeightText } from './LimitedHeightText';
import { Tags } from './Tags/Tags';
import { useEditableRow } from './useEditableRow';
import { ScreenshotsPopover } from './Screenshots/ScreenshotsPopover';
import { PositionType, useCellStyles } from './cell/styles';
import {
  useTranslationsSelector,
  useTranslationsDispatch,
} from './context/TranslationsContext';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { useDebounce } from 'use-debounce/lib';
import { ControlsEditor } from './cell/ControlsEditor';
import { ControlsKey } from './cell/ControlsKey';
import { TagAdd } from './Tags/TagAdd';
import { TagInput } from './Tags/TagInput';
import { getMeta } from 'tg.fixtures/isMac';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const useStyles = makeStyles((theme) => {
  return {
    container: {
      display: 'grid',
      gridTemplateColumns: 'auto 1fr',
      gridTemplateRows: 'auto auto 1fr auto',
      gridTemplateAreas: `
        "checkbox key          "
        ".        tags         "
        "editor   editor       "
        "controls controls     "
      `,
    },
    checkbox: {
      gridArea: 'checkbox',
      width: 38,
      height: 38,
      margin: '3px -9px -9px 3px',
    },
    key: {
      gridArea: 'key',
      margin: '12px 12px 8px 12px',
      overflow: 'hidden',
      position: 'relative',
    },
    tags: {
      gridArea: 'tags',
      display: 'flex',
      flexWrap: 'wrap',
      alignItems: 'flex-start',
      overflow: 'hidden',
      '& > *': {
        margin: '0px 3px 3px 0px',
      },
      margin: '0px 12px 0px 12px',
      position: 'relative',
      paddingBottom: 24,
      marginBottom: -24,
    },
    tagAdd: {
      position: 'absolute',
      bottom: 0,
      margin: 0,
    },
    editor: {
      gridArea: 'editor',
      overflow: 'hidden',
      padding: '12px 12px 0px 12px',
    },
    controls: {
      gridArea: 'controls',
      display: 'flex',
      justifyContent: 'space-between',
      overflow: 'hidden',
      alignItems: 'flex-end',
    },
    controlsSmall: {
      boxSizing: 'border-box',
      gridArea: 'controls',
      display: 'flex',
      justifyContent: 'flex-end',
      overflow: 'hidden',
      minHeight: 44,
      padding: '12px 12px 12px 12px',
    },
  };
});

type Props = {
  data: KeyWithTranslationsModel;
  width?: string | number;
  editEnabled: boolean;
  active: boolean;
  simple?: boolean;
  position?: PositionType;
  onSaveSuccess?: (value: string) => void;
};

export const CellKey: React.FC<Props> = ({
  data,
  width,
  editEnabled,
  active,
  simple,
  position,
  onSaveSuccess,
}) => {
  const classes = useStyles();
  const cellClasses = useCellStyles({ position });
  const cellRef = useRef<HTMLDivElement>(null);
  const [screenshotsOpen, setScreenshotsOpen] = useState(false);
  const dispatch = useTranslationsDispatch();

  const screenshotEl = useRef<HTMLButtonElement | null>(null);

  const isSelected = useTranslationsSelector((c) =>
    c.selection.includes(data.keyId)
  );

  // prevent blinking, when closing popup
  const [screenshotsOpenDebounced] = useDebounce(screenshotsOpen, 100);

  const toggleSelect = () => {
    dispatch({ type: 'TOGGLE_SELECT', payload: data.keyId });
  };

  const handleAddTag = (name: string) => {
    dispatch({
      type: 'ADD_TAG',
      payload: { keyId: data.keyId, name, onSuccess: () => setTagEdit(false) },
    });
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
  } = useEditableRow({
    keyId: data.keyId,
    keyName: data.keyName,
    defaultVal: data.keyName,
    language: undefined,
    onSaveSuccess,
    cellRef,
  });

  return (
    <>
      <div
        className={clsx({
          [classes.container]: true,
          [cellClasses.cellPlain]: true,
          [cellClasses.hover]: !isEditing,
          [cellClasses.cellClickable]: editEnabled && !isEditing,
          [cellClasses.cellRaised]: isEditing,
          [cellClasses.scrollMargins]: true,
        })}
        style={{ width }}
        onClick={
          !isEditing && editEnabled ? () => handleOpen('editor') : undefined
        }
        data-cy="translations-table-cell"
        tabIndex={0}
        ref={cellRef}
      >
        {!isEditing ? (
          <>
            {editEnabled && !simple && (
              <Checkbox
                className={classes.checkbox}
                size="small"
                checked={isSelected}
                onChange={toggleSelect}
                onClick={stopBubble()}
                data-cy="translations-row-checkbox"
              />
            )}
            <div className={classes.key}>
              <LimitedHeightText width={width} maxLines={3} wrap="break-all">
                {data.keyName}
              </LimitedHeightText>
            </div>
            {!simple && (
              <div className={classes.tags}>
                <Tags
                  keyId={data.keyId}
                  tags={data.keyTags}
                  deleteEnabled={editEnabled}
                />
                {editEnabled &&
                  (tagEdit ? (
                    <TagInput
                      className={classes.tagAdd}
                      onAdd={handleAddTag}
                      onClose={() => setTagEdit(false)}
                      autoFocus
                    />
                  ) : (
                    active && (
                      <TagAdd
                        className={classes.tagAdd}
                        onClick={() => setTagEdit(true)}
                        withFullLabel={!data.keyTags?.length}
                      />
                    )
                  ))}
              </div>
            )}
          </>
        ) : (
          <div className={classes.editor}>
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
            />
          </div>
        )}

        <div className={isEditing ? classes.controls : classes.controlsSmall}>
          {isEditing ? (
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
      </div>
      {screenshotsOpen && (
        <ScreenshotsPopover
          anchorEl={screenshotEl.current!}
          keyId={data.keyId}
          onClose={() => {
            setScreenshotsOpen(false);
          }}
        />
      )}
    </>
  );
};
