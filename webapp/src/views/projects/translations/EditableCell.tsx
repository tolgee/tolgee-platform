import { Box, Theme, Tooltip, Typography } from '@material-ui/core';
import IconButton from '@material-ui/core/IconButton';
import InputAdornment from '@material-ui/core/InputAdornment';
import { createStyles, makeStyles } from '@material-ui/core/styles';
import CheckIcon from '@material-ui/icons/Check';
import CloseIcon from '@material-ui/icons/Close';
import EditIcon from '@material-ui/icons/Edit';
import React, { FunctionComponent, useEffect, useState } from 'react';
import { EasyInput } from 'tg.component/common/form/fields/EasyInput';
import { MicroForm } from 'tg.component/common/form/MicroForm';
import * as Yup from 'yup';

export interface EditableCellProps {
  initialValue: any;
  validationSchema: Yup.AnySchema;
  onSubmit: (value: string) => void;
  onChange: (value: string) => void;
  editEnabled: boolean;
  isEditing: boolean;
  onCancel?: (value: string) => void;
  onEditClick: () => void;
  lang?: string;
}

const useStyles = makeStyles((theme: Theme) =>
  createStyles({
    textBox: {
      maxWidth: '100%',
      '& .hiding-click-icon': {
        opacity: '0.1',
        padding: 0,
        transition: 'opacity 0.2s',
        '&.Mui-focusVisible': {
          opacity: 1,
        },
      },
      '&:hover .hiding-click-icon': {
        opacity: 0.5,
      },
    },
  })
);

export const EditableCell: FunctionComponent<EditableCellProps> = (props) => {
  const [overflow, setOverflow] = useState(false);
  const [value, setValue] = useState(props.initialValue);

  const onInputChange = (e) => {
    setValue(e.target.value);
    props.onChange(e.target.value);
  };

  const ref = React.createRef<HTMLDivElement>();

  useEffect(() => {
    const onresize = () => {
      if (ref.current) {
        setOverflow(ref.current.scrollWidth > ref.current.clientWidth);
      }
    };

    onresize();

    window.addEventListener('resize', onresize);

    return () => window.removeEventListener('resize', onresize);
  }, [ref]);

  const classes = useStyles({});

  const textHolder = (
    <Typography noWrap ref={ref} style={{ fontSize: 'inherit' }}>
      {props.initialValue}
    </Typography>
  );

  const EditIconButton = () => (
    <>
      {props.editEnabled && (
        <IconButton
          aria-label="edit"
          color="default"
          className={'hiding-click-icon'}
          size="small"
        >
          <EditIcon fontSize="small" />
        </IconButton>
      )}
    </>
  );

  if (!props.isEditing) {
    return (
      <Box
        data-cy="translations-editable-cell"
        onClick={() => {
          props.editEnabled && props.onEditClick();
        }}
        style={{ cursor: props.editEnabled ? 'pointer' : 'initial' }}
        display="flex"
        alignItems="center"
        maxWidth="100%"
        className={classes.textBox}
      >
        {overflow ? (
          <>
            <Tooltip title={props.initialValue}>{textHolder}</Tooltip>
            <EditIconButton />
          </>
        ) : (
          <>
            {textHolder}
            <EditIconButton />
          </>
        )}
      </Box>
    );
  }

  return (
    <Box flexGrow={1} data-cy="translations-editable-cell-editing">
      <MicroForm
        // @ts-ignore
        onSubmit={(v: { value: any }) => props.onSubmit(v.value)}
        initialValues={{ value: props.initialValue || '' }}
        validationSchema={Yup.object().shape({ value: props.validationSchema })}
      >
        <EasyInput
          lang={props.lang}
          onChange={onInputChange}
          multiline
          name="value"
          fullWidth
          endAdornment={
            <InputAdornment position="end">
              <IconButton edge="end" color="primary" type="submit">
                <CheckIcon />
              </IconButton>
              <IconButton
                onClick={() => props.onCancel && props.onCancel(value)}
                edge="end"
                color="secondary"
              >
                <CloseIcon />
              </IconButton>
            </InputAdornment>
          }
        />
      </MicroForm>
    </Box>
  );
};
