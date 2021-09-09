import clsx from 'clsx';
import { makeStyles, Tabs, Tab, IconButton } from '@material-ui/core';
import { Close } from '@material-ui/icons';
import { T } from '@tolgee/react';

import { ControlsEditor } from './cell/ControlsEditor';
import { Editor } from 'tg.component/editor/Editor';
import { components } from 'tg.service/apiSchema.generated';
import { StateType } from 'tg.constants/translationStates';
import { Comments } from './comments/Comments';
import { EditModeType } from './context/useEdit';

type LanguageModel = components['schemas']['LanguageModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];

const useStyles = makeStyles((theme) => {
  const borderColor = theme.palette.grey[200];

  return {
    editor: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'stretch',
      minHeight: 350,
    },
    editorContainer: {
      padding: '12px 12px 0px 12px',
      flexGrow: 1,
      display: 'flex',
      alignItems: 'stretch',
    },
    editorControls: {
      display: 'flex',
    },
    controls: {
      boxSizing: 'border-box',
      gridArea: 'controls',
      display: 'flex',
      justifyContent: 'flex-end',
      overflow: 'hidden',
      minHeight: 44,
      padding: '12px 12px 12px 12px',
      marginTop: -16,
    },

    tabsWrapper: {
      display: 'flex',
      borderBottom: `1px solid ${borderColor}`,
      justifyContent: 'space-between',
      alignItems: 'center',
    },
    tabs: {
      maxWidth: '100%',
      overflow: 'hidden',
      minHeight: 0,
      marginBottom: -1,
    },
    tab: {
      minHeight: 0,
      minWidth: 100,
      margin: '0px 5px',
    },
    closeButton: {
      width: 30,
      height: 30,
      marginRight: 12,
    },
  };
});

type Props = {
  value: string;
  keyId: number;
  language: LanguageModel;
  translation: TranslationViewModel | undefined;
  onChange: (val: string) => void;
  onSave: () => void;
  onCmdSave: () => void;
  onCancel: () => void;
  onStateChange: (state: StateType) => void;
  state: StateType;
  autofocus: boolean;
  className?: string;
  mode: EditModeType;
  onModeChange: (mode: EditModeType) => void;
  editEnabled: boolean;
};

export const TranslationOpened: React.FC<Props> = ({
  value,
  keyId,
  language,
  translation,
  onChange,
  onSave,
  onCmdSave,
  onCancel,
  onStateChange,
  state,
  autofocus,
  className,
  mode,
  onModeChange,
  editEnabled,
}) => {
  const classes = useStyles();

  return (
    <div className={clsx(classes.editor, className)}>
      <div className={classes.tabsWrapper}>
        <Tabs
          indicatorColor="primary"
          value={mode}
          onChange={(_, value) => onModeChange(value)}
          className={classes.tabs}
          variant="scrollable"
          scrollButtons="off"
        >
          {editEnabled && (
            <Tab
              label={<T noWrap>translations_cell_tab_edit</T>}
              value="editor"
              className={classes.tab}
              data-cy="translations-cell-tab-edit"
            />
          )}
          <Tab
            label={
              <T
                noWrap
                parameters={{ count: String(translation?.commentCount || 0) }}
              >
                translations_cell_tab_comments
              </T>
            }
            value="comments"
            className={classes.tab}
            data-cy="translations-cell-tab-comments"
          />
        </Tabs>
        <IconButton
          size="small"
          onClick={onCancel}
          className={classes.closeButton}
          data-cy="translations-cell-close"
        >
          <Close />
        </IconButton>
      </div>
      {mode === 'editor' ? (
        <>
          <div className={classes.editorContainer}>
            <Editor
              value={value}
              onChange={onChange}
              onSave={onSave}
              onCmdSave={onCmdSave}
              onCancel={onCancel}
              autofocus={autofocus}
            />
          </div>
          <div className={classes.editorControls}>
            <ControlsEditor
              state={state}
              onSave={onSave}
              onCancel={onCancel}
              onStateChange={onStateChange}
            />
          </div>
        </>
      ) : mode === 'comments' ? (
        <Comments
          keyId={keyId}
          language={language}
          translation={translation}
          onCancel={onCancel}
          editEnabled={editEnabled}
        />
      ) : null}
    </div>
  );
};
