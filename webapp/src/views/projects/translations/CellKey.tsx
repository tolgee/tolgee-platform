import clsx from 'clsx';
import React, { useRef, useState } from 'react';
import { useTranslate } from '@tolgee/react';
import { Checkbox, styled, Tooltip, Box } from '@mui/material';
import { Zap } from '@untitled-ui/icons-react';

import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { components } from 'tg.service/apiSchema.generated';
import { stopBubble } from 'tg.fixtures/eventHandler';
import { wrapIf } from 'tg.fixtures/wrapIf';

import { Tags } from './Tags/Tags';
import {
  CELL_CLICKABLE,
  CELL_PLAIN,
  CELL_SELECTED,
  StyledCell,
} from './cell/styles';
import {
  useTranslationsActions,
  useTranslationsSelector,
} from './context/TranslationsContext';
import { ControlsKey } from './cell/ControlsKey';
import { TagAdd } from './Tags/TagAdd';
import { TagInput } from './Tags/TagInput';
import { KeyEditModal } from './KeyEdit/KeyEditModal';
import { useKeyCell } from './useKeyCell';
import { ALLOWED_UPLOAD_TYPES, Screenshots } from './Screenshots/Screenshots';
import { useGlobalContext } from 'tg.globalContext/GlobalContext';
import { ScreenshotDropzone } from './Screenshots/ScreenshotDropzone';
import { useScreenshotUpload } from './Screenshots/useScreenshotUpload';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import ReactMarkdown from 'react-markdown';
import { MarkdownLink } from 'tg.component/common/MarkdownLink';

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const StyledContainer = styled(StyledCell)`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto auto auto 1fr auto;
  grid-template-areas:
    'checkbox  key          '
    '.         description  '
    '.         screenshots  '
    '.         tags         '
    'editor    editor       '
    'controls  controls     ';

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

const StyledDescription = styled('div')`
  grid-area: description;
  padding: 0px 12px 8px 12px;
  font-size: 13px;
  color: ${({ theme }) =>
    theme.palette.mode === 'light'
      ? theme.palette.emphasis[300]
      : theme.palette.emphasis[500]};
`;

const StyledTags = styled('div')`
  grid-area: tags;
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  min-width: 0;
  & > * {
    margin: 0 6px 3px 0;
  }
  margin: 0px 12px 0px 12px;
  position: relative;
  min-height: 28px;
`;

const StyledDropzone = styled('div')`
  display: grid;
  grid-row: 2 / -1;
  grid-column: 1 / -1;
  position: relative;
  padding: 0px 12px 12px 0px;
  z-index: ${({ theme }) => theme.zIndex.tooltip};
`;

const StyledScreenshots = styled('div')`
  grid-area: screenshots;
  position: relative;
  overflow: hidden;
  margin-top: -12px;
  padding-bottom: 8px;
`;

const StyledContextButton = styled(Box)`
  position: absolute;
  bottom: 12px;
  left: 12px;
`;

const StyledBolt = styled(Zap)`
  width: 14px;
  height: 14px;
`;

type Props = {
  data: KeyWithTranslationsModel;
  widthPercent?: string | number;
  width?: number;
  editEnabled: boolean;
  active: boolean;
  simple?: boolean;
  className?: string;
  onSaveSuccess?: (value: string) => void;
  oneScreenshotBig?: boolean;
  editInDialog?: boolean;
};

export const CellKey: React.FC<Props> = ({
  data,
  widthPercent,
  width,
  editEnabled,
  active,
  simple,
  oneScreenshotBig,
  className,
}) => {
  const cellRef = useRef<HTMLDivElement>(null);
  const fileRef = useRef<HTMLInputElement>(null);
  const { toggleSelect, groupToggleSelect, addTag } = useTranslationsActions();
  const { t } = useTranslate();
  const { onFileSelected, validateAndUpload, openFiles } = useScreenshotUpload({
    fileRef,
    keyId: data.keyId,
  });
  const { satisfiesPermission } = useProjectPermissions();
  const canAddScreenshots = satisfiesPermission('screenshots.upload');

  const userIsDragging = useGlobalContext((c) => c.userIsDragging);

  const isSelected = useTranslationsSelector((c) =>
    c.selection.includes(data.keyId)
  );
  const somethingSelected = useTranslationsSelector((c) =>
    Boolean(c.selection.length)
  );

  const handleToggleSelect = (e: React.PointerEvent) => {
    const shiftPressed = e.nativeEvent.shiftKey;
    if (shiftPressed) {
      groupToggleSelect(data.keyId);
    } else {
      toggleSelect(data.keyId);
    }
  };

  const handleAddTag = (name: string) => {
    addTag({ keyId: data.keyId, name, onSuccess: () => setTagEdit(false) });
  };

  const [tagEdit, setTagEdit] = useState(false);

  const { isEditing, handleOpen, handleClose, editVal, isEditingTranslation } =
    useKeyCell({
      keyData: data,
      cellRef,
    });

  return (
    <>
      <StyledContainer
        className={clsx(
          {
            [CELL_PLAIN]: true,
            [CELL_CLICKABLE]: editEnabled,
            [CELL_SELECTED]: isEditing,
          },
          className
        )}
        style={{ width: widthPercent }}
        onClick={editEnabled ? () => handleOpen() : undefined}
        data-cy="translations-table-cell"
        tabIndex={0}
        ref={cellRef}
      >
        <>
          {!simple &&
            wrapIf(
              somethingSelected && !isSelected,
              <StyledCheckbox
                size="small"
                checked={isSelected}
                onChange={handleToggleSelect as any}
                onClick={stopBubble()}
                data-cy="translations-row-checkbox"
              />,
              <Tooltip
                title={t('translations_checkbox_select_multiple_hint')}
                enterDelay={1000}
                enterNextDelay={1000}
                disableInteractive
              >
                <div />
              </Tooltip>
            )}
          <StyledKey data-cy="translations-key-name">
            <LimitedHeightText
              width={widthPercent}
              maxLines={3}
              wrap="break-all"
            >
              {data.keyName}
            </LimitedHeightText>
          </StyledKey>
          {data.keyDescription && (
            <StyledDescription data-cy="translations-key-cell-description">
              <LimitedHeightText maxLines={5}>
                <ReactMarkdown
                  components={{
                    a: MarkdownLink,
                  }}
                >
                  {data.keyDescription}
                </ReactMarkdown>
              </LimitedHeightText>
            </StyledDescription>
          )}
          {data.screenshots && (
            <StyledScreenshots>
              <Screenshots
                screenshots={data.screenshots}
                keyId={data.keyId}
                oneBig={oneScreenshotBig && isEditingTranslation}
                width={width ? width - 38 : undefined}
              />
            </StyledScreenshots>
          )}
          {!simple && (
            <>
              <StyledTags
                sx={{ marginBottom: data.keyTags.length ? '-12px' : '-20px' }}
              >
                <Tags
                  keyId={data.keyId}
                  tags={data.keyTags}
                  deleteEnabled={editEnabled}
                />
                {editEnabled &&
                  (tagEdit ? (
                    <TagInput
                      onAdd={handleAddTag}
                      onClose={() => setTagEdit(false)}
                      autoFocus
                    />
                  ) : (
                    <TagAdd
                      onClick={() => setTagEdit(true)}
                      withFullLabel={!data.keyTags?.length}
                    />
                  ))}
              </StyledTags>
            </>
          )}

          {data.contextPresent && (
            <Tooltip title={t('key-context-present-hint')}>
              <StyledContextButton
                role="button"
                onClick={() => handleOpen('context')}
              >
                <StyledBolt />
              </StyledContextButton>
            </Tooltip>
          )}
        </>

        <div className="controlsSmall">
          {!tagEdit ? (
            active ? (
              <ControlsKey
                onEdit={() => handleOpen()}
                onAddScreenshot={canAddScreenshots ? openFiles : undefined}
                editEnabled={editEnabled}
              />
            ) : (
              // hide as many components as possible in order to be performant
              <ControlsKey editEnabled={editEnabled} />
            )
          ) : null}
        </div>
        {canAddScreenshots && (
          <>
            {userIsDragging && (
              <StyledDropzone data-cy="cell-key-screenshot-dropzone">
                <ScreenshotDropzone validateAndUpload={validateAndUpload} />
              </StyledDropzone>
            )}
            <input
              data-cy="cell-key-screenshot-file-input"
              type="file"
              style={{ display: 'none' }}
              ref={fileRef}
              onChange={onFileSelected}
              multiple
              accept={ALLOWED_UPLOAD_TYPES.join(',')}
            />
          </>
        )}
      </StyledContainer>
      {isEditing && (
        <KeyEditModal
          data={data}
          onClose={() => handleClose(true)}
          initialTab={editVal?.mode === 'context' ? 'context' : 'general'}
        />
      )}
    </>
  );
};
