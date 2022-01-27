import clsx from 'clsx';
import { makeStyles, Tabs, Tab, IconButton } from '@material-ui/core';
import { Close } from '@material-ui/icons';
import { T } from '@tolgee/react';

import { ControlsEditor } from './cell/ControlsEditor';
import { Editor } from 'tg.component/editor/Editor';
import { components } from 'tg.service/apiSchema.generated';
import { StateType, translationStates } from 'tg.constants/translationStates';
import { Comments } from './comments/Comments';
import { getMeta } from 'tg.fixtures/isMac';
import { useTranslationsDispatch } from './context/TranslationsContext';
import { ToolsPopup } from './TranslationTools/ToolsPopup';
import { useTranslationTools } from './TranslationTools/useTranslationTools';
import { useProject } from 'tg.hooks/useProject';
import { EditMode } from './context/types';

type LanguageModel = components['schemas']['LanguageModel'];
type TranslationViewModel = components['schemas']['TranslationViewModel'];
type State = components['schemas']['TranslationViewModel']['state'];

const useStyles = makeStyles((theme) => {
  const borderColor = theme.palette.grey[200];

  return {
    container: {
      display: 'flex',
      flexDirection: 'column',
      alignItems: 'stretch',
      minHeight: 300,
    },
    editorContainer: {
      padding: '12px 12px 0px 12px',
      flexGrow: 1,
      display: 'flex',
      alignItems: 'stretch',
      flexDirection: 'column',
    },
    editorControls: {
      display: 'flex',
      position: 'relative',
      marginTop: theme.spacing(3),
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
  onCancel: (force: boolean) => void;
  onStateChange: (state: StateType) => void;
  state: State;
  autofocus: boolean;
  className?: string;
  mode: EditMode;
  onModeChange: (mode: EditMode) => void;
  editEnabled: boolean;
  cellRef: React.RefObject<HTMLDivElement>;
  cellPosition?: string;
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
  cellRef,
  cellPosition,
}) => {
  const project = useProject();
  const classes = useStyles();
  const dispatch = useTranslationsDispatch();

  const nextState = translationStates[state]?.next;

  const handleStateChange = () => {
    if (nextState) {
      dispatch({
        type: 'SET_TRANSLATION_STATE',
        payload: {
          state: nextState,
          keyId,
          translationId: translation!.id,
          language: language.tag,
        },
      });
    }
  };

  const data = useTranslationTools({
    projectId: project.id,
    keyId,
    targetLanguageId: language.id,
    enabled: !language.base,
    onValueUpdate: (value) => {
      dispatch({
        type: 'UPDATE_EDIT',
        payload: {
          value,
        },
      });
    },
  });

  return (
    <div className={clsx(classes.container, className)}>
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
              label={<T>translations_cell_tab_edit</T>}
              value="editor"
              className={classes.tab}
              data-cy="translations-cell-tab-edit"
            />
          )}
          <Tab
            label={
              <T parameters={{ count: String(translation?.commentCount || 0) }}>
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
          onClick={() => onCancel(true)}
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
              onCancel={() => onCancel(true)}
              onSave={onSave}
              autofocus={autofocus}
              shortcuts={{
                [`${getMeta()}-E`]: handleStateChange,
                [`${getMeta()}-Enter`]: onCmdSave,
              }}
            />
          </div>
          <div className={classes.editorControls}>
            <ControlsEditor
              state={state}
              onSave={onSave}
              onCancel={() => onCancel(true)}
              onStateChange={onStateChange}
            />
          </div>
          {!language.base && (
            <ToolsPopup
              anchorEl={cellRef.current || undefined}
              cellPosition={cellPosition}
              data={data}
            />
          )}
        </>
      ) : mode === 'comments' ? (
        <Comments
          keyId={keyId}
          language={language}
          translation={translation}
          onCancel={() => onCancel(true)}
          editEnabled={editEnabled}
        />
      ) : null}
    </div>
  );
};
