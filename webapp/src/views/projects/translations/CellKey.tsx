import clsx from 'clsx';
import React, { useRef, useState } from 'react';
import { useDebounce } from 'use-debounce';
import { useTranslate } from '@tolgee/react';
import { Checkbox, styled, Tooltip, Box } from '@mui/material';
import { Zap } from '@untitled-ui/icons-react';

import { LimitedHeightText } from 'tg.component/LimitedHeightText';
import { components } from 'tg.service/apiSchema.generated';
import { stopBubble } from 'tg.fixtures/eventHandler';

import { Tags } from './Tags/Tags';
import { ScreenshotsPopover } from './Screenshots/ScreenshotsPopover';
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

type KeyWithTranslationsModel =
  components['schemas']['KeyWithTranslationsModel'];

const StyledContainer = styled(StyledCell)`
  display: grid;
  grid-template-columns: auto 1fr;
  grid-template-rows: auto auto auto 1fr auto;
  grid-template-areas:
    'checkbox key          '
    '.        description  '
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
  overflow: hidden;
  & > * {
    margin: 0px 3px 3px 0px;
  }
  margin: 0px 12px 0px 12px;
  position: relative;
  min-height: 28px;
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
  width?: string | number;
  editEnabled: boolean;
  active: boolean;
  simple?: boolean;
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
  onSaveSuccess,
  className,
}) => {
  const cellRef = useRef<HTMLDivElement>(null);
  const [screenshotsOpen, setScreenshotsOpen] = useState(false);
  const { toggleSelect, addTag } = useTranslationsActions();
  const { t } = useTranslate();

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

  const { isEditing, handleOpen, handleClose, editVal } = useKeyCell({
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
        style={{ width }}
        onClick={editEnabled ? () => handleOpen() : undefined}
        data-cy="translations-table-cell"
        tabIndex={0}
        ref={cellRef}
      >
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
          {data.keyDescription && (
            <StyledDescription data-cy="translations-key-cell-description">
              <LimitedHeightText maxLines={5}>
                {data.keyDescription}
              </LimitedHeightText>
            </StyledDescription>
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
            active || screenshotsOpen || screenshotsOpenDebounced ? (
              <ControlsKey
                onEdit={() => handleOpen()}
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
