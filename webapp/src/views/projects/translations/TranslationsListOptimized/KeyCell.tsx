import React, { useState, useRef } from 'react';
import { Checkbox, makeStyles } from '@material-ui/core';
import { useContextSelector } from 'use-context-selector';
import clsx from 'clsx';

import { Editor } from 'tg.component/editor/Editor';
import { components } from 'tg.service/apiSchema.generated';
import { CellControls } from '../cell';
import { LimitedHeightText } from '../LimitedHeightText';
import { Tags } from '../Tags/Tags';
import { useEditableRow } from '../useEditableRow';
import { ScreenshotsPopover } from '../Screenshots/ScreenshotsPopover';
import { useCellStyles } from '../cell/styles';
import {
  TranslationsContext,
  useTranslationsDispatch,
} from '../context/TranslationsContext';
import { stopBubble } from 'tg.fixtures/eventHandler';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const useStyles = makeStyles((theme) => {
  return {
    container: {
      display: 'grid',
      padding: '12px 12px',
      gridTemplateColumns: '28px auto',
      gridTemplateRows: 'auto auto 1fr auto auto',
      gridTemplateAreas: `
        "checkbox key          "
        "space    tags         "
        "editor   editor       "
        "controls controls     "
        "space2   smallControls"
      `,
    },
    checkbox: {
      gridArea: 'checkbox',
      width: 38,
      height: 38,
      margin: -9,
    },
    key: {
      gridArea: 'key',
      margin: '0px 0px 8px 0px',
      overflow: 'hidden',
    },
    tags: {
      gridArea: 'tags',
      display: 'flex',
      flexWrap: 'wrap',
      alignItems: 'flex-start',
      overflow: 'hidden',
      '& > *': {
        margin: '0px 2px 2px 0px',
      },
    },
    editor: {
      gridArea: 'editor',
      overflow: 'hidden',
    },
    controls: {
      gridArea: 'controls',
      overflow: 'hidden',
    },
    smallControls: {
      gridArea: 'smallControls',
      overflow: 'hidden',
    },
  };
});

type Props = {
  data: KeyWithTranslationsModel;
  width: number;
  editEnabled: boolean;
  active: boolean;
};

export const KeyCell: React.FC<Props> = React.memo(
  ({ data, width, editEnabled, active }) => {
    const classes = useStyles();
    const cellClasses = useCellStyles();
    const [screenshotsOpen, setScreenshotsOpen] = useState(false);
    const dispatch = useTranslationsDispatch();

    const screenshotEl = useRef<HTMLButtonElement | null>(null);

    const isSelected = useContextSelector(TranslationsContext, (c) =>
      c.selection.includes(data.keyId)
    );

    const toggleSelect = () => {
      dispatch({ type: 'TOGGLE_SELECT', payload: data.keyId });
    };

    const handleAddTag = (name: string, onSuccess) => {
      dispatch({
        type: 'ADD_TAG',
        payload: { keyId: data.keyId, name },
        onSuccess,
      });
    };

    const {
      isEditing,
      value,
      setValue,
      handleEdit,
      handleEditCancel,
      handleSave,
      autofocus,
    } = useEditableRow({
      keyId: data.keyId,
      keyName: data.keyName,
      defaultVal: data.keyName,
      language: undefined,
    });

    const isEmpty = data.keyId < 0;

    return (
      <>
        <div
          className={clsx({
            [classes.container]: true,
            [cellClasses.cellPlain]: true,
            [cellClasses.cellClickable]: editEnabled && !isEditing,
            [cellClasses.cellRaised]: isEditing,
          })}
          style={{ width }}
          onClick={
            !isEditing && editEnabled ? () => handleEdit(undefined) : undefined
          }
        >
          {!isEditing ? (
            <>
              <Checkbox
                className={classes.checkbox}
                size="small"
                checked={isSelected}
                onChange={toggleSelect}
                onClick={stopBubble()}
                data-cy="translations-row-checkbox"
              />
              <div className={classes.key}>
                <LimitedHeightText>{data.keyName}</LimitedHeightText>
              </div>
              <div className={classes.tags}>
                <Tags keyId={data.keyId} tags={data.keyTags} />
              </div>
            </>
          ) : (
            <div className={classes.editor}>
              <Editor
                plaintext
                value={value}
                onChange={(v) => setValue(v as string)}
                onSave={() => handleSave()}
                onCmdSave={() =>
                  handleSave(isEmpty ? 'NEW_EMPTY_KEY' : 'EDIT_NEXT')
                }
                onCancel={handleEditCancel}
                autofocus={autofocus}
              />
            </div>
          )}

          <div className={isEditing ? classes.controls : classes.smallControls}>
            {isEditing ? (
              <CellControls
                key="edit"
                mode="edit"
                onCancel={handleEditCancel}
                onSave={handleSave}
                onScreenshots={
                  isEmpty ? undefined : () => setScreenshotsOpen(true)
                }
                screenshotRef={screenshotEl}
                screenshotsPresent={data.screenshotCount > 0}
                screenshotsOpen={screenshotsOpen}
                editEnabled={editEnabled}
              />
            ) : active ? (
              <CellControls
                mode="view"
                onEdit={() => handleEdit(undefined)}
                onCancel={handleEditCancel}
                onSave={handleSave}
                onScreenshots={
                  isEmpty ? undefined : () => setScreenshotsOpen(true)
                }
                screenshotRef={screenshotEl}
                screenshotsPresent={data.screenshotCount > 0}
                screenshotsOpen={screenshotsOpen}
                firstTag={!data.keyTags?.length}
                onAddTag={handleAddTag}
                addTag={true}
                editEnabled={editEnabled}
              />
            ) : (
              <CellControls
                mode="view"
                onScreenshots={
                  isEmpty ? undefined : () => setScreenshotsOpen(true)
                }
                screenshotRef={screenshotEl}
                screenshotsPresent={data.screenshotCount > 0}
                screenshotsOpen={screenshotsOpen}
                firstTag={!data.keyTags?.length}
                onAddTag={handleAddTag}
                editEnabled={editEnabled}
              />
            )}
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
  }
);
