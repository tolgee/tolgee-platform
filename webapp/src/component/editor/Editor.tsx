import React, { useRef, useState, useEffect } from 'react';
import AceEditor from 'react-ace';
import { Ace } from 'ace-builds';
import { makeStyles } from '@material-ui/core';
import clsx from 'clsx';
import 'ace-builds/src-noconflict/ext-language_tools';

import IcuMode from './icuMode';
import icuValidator from './icuValidator';
import { icuAutocompete } from './icuAutocomplete';

const useStyles = makeStyles((theme) => ({
  editor: {
    overflow: 'visible',
    marginBottom: '16px',
    // @ts-ignore
    background: (props) => props.background || 'white',
    '& .error, & .error-highlight': {
      position: 'absolute',
      pointerEvents: 'auto',
      'z-index': 'unset !important',
    },
    '& .error-highlight': {
      backgroundColor: '#ff00004d',
    },
    '& .error:before, & .error-highlight:before': {
      content: "''",
      position: 'absolute',
      top: 0,
      bottom: 0,
      left: 0,
      right: 0,
      zIndex: 2,
      borderBottom: `2px dotted ${theme.palette.error.main}`,
      borderRadius: 0,
    },

    '& .error:hover:after, & .error-highlight:hover:after': {
      pointerEvents: 'none',
      position: 'absolute',
      left: 0,
      padding: '3px 5px',
      top: 'calc(100% + 3px)',
      zIndex: 1000,
      // @ts-ignore
      content: (props) => `'${props.error || ''}'`,
      backgroundColor: '#000',
      color: '#fff',
      whiteSpace: 'nowrap',
      overflow: 'hidden',
      textOverflow: 'ellipsis',
    },
    '& .ace_marker-layer': {
      zIndex: 'unset',
      overflow: 'visible',
    },
    '& .ace_marker-layer > *': {
      zIndex: 1,
      overflow: 'visible',
    },
    '& .ace_scroller': {
      overflow: 'visible',
    },
  },
  icuSyntax: {
    '& .ace_text-layer .ace_function': {
      color: '#007300',
    },
    '& .ace_text-layer .ace_parameter': {
      color: '#002bff',
    },
    '& .ace_text-layer .ace_option': {
      color: '#002bff',
    },
    '& .ace_text-layer .ace_string': {
      color: '#000000',
    },
    '& .ace_text-layer .ace_bracket': {
      color: '#002bff',
    },
  },
}));

type Props = {
  initialValue: string;
  onChange?: (val: string) => void;
  onSave?: (val: string) => void;
  onCancel?: (val: string) => void;
  background?: string;
  plaintext?: boolean;
};

export const Editor: React.FC<Props> = ({
  initialValue,
  onChange,
  onSave,
  onCancel,
  background,
  plaintext,
}) => {
  const editorRef = useRef<Ace.Editor>();
  const [error, setError] = useState<string>();
  const classes = useStyles({ error, background });
  // editor is not updating references
  // so we need to go through refs with callbacks
  const onSaveRef = useRef<typeof onSave>();
  onSaveRef.current = onSave;
  const onCancelRef = useRef<typeof onCancel>();
  onCancelRef.current = onCancel;

  const handleChange = (text: string) => {
    onChange?.(text);
  };

  const onLoad = (editor: Ace.Editor) => {
    const myCustomMode = new IcuMode();
    editorRef.current = editor;
    editor.focus();
    editor.setOption('indentedSoftWrap', false);
    editor.commands.addCommand({
      name: 'save',
      bindKey: { win: 'Enter', mac: 'Enter' },
      exec: (editor) => onSaveRef.current?.(editor.getSession().getValue()),
    });
    editor.commands.addCommand({
      name: 'cancel',
      bindKey: { win: 'Esc', mac: 'Esc' },
      exec: (editor) => onCancelRef.current?.(editor.getSession().getValue()),
    });

    if (!plaintext) {
      icuValidator(editor, setError);
      editor.completers = [icuAutocompete];
      // @ts-ignore
      editor.setOption('enableBasicAutocompletion', true);
      // @ts-ignore
      editor.setOption('enableLiveAutocompletion', true);
      // @ts-ignore
      editor.setOption('enableSnippets', true);
      // @ts-ignore
      editor.getSession().setMode(myCustomMode);
    }
  };

  useEffect(() => {
    editorRef.current?.resize();
  });

  return (
    <div data-cy="global-editor">
      <AceEditor
        className={clsx(classes.editor, !plaintext && classes.icuSyntax)}
        value={initialValue}
        onChange={handleChange}
        // @ts-ignore
        theme={null}
        width="100%"
        height="100%"
        // @ts-ignore
        mode={null}
        minLines={5}
        setOptions={{
          showLineNumbers: false,
          showGutter: false,
          wrap: true,
          maxLines: Infinity,
          highlightActiveLine: false,
        }}
        onLoad={onLoad}
      />
    </div>
  );
};
