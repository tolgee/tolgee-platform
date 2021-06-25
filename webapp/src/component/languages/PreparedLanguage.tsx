import React, { FC } from 'react';
import { Box, IconButton } from '@material-ui/core';
import { makeStyles } from '@material-ui/core/styles';
import { Close, Edit } from '@material-ui/icons';
import clsx from 'clsx';

import { components } from 'tg.service/apiSchema.generated';

import { FlagImage } from './FlagImage';

const useStyles = makeStyles((theme) => ({
  root: {
    border: `1px solid ${theme.palette.grey['100']}`,
    padding: `${theme.spacing(0.5)}px ${theme.spacing(1)}px`,
    borderRadius: theme.shape.borderRadius,
    display: `inline-flex`,
    alignItems: `center`,
  },
  flagImage: {
    width: `20px`,
    height: `20px`,
    marginRight: theme.spacing(1),
  },
  editButton: {
    marginLeft: theme.spacing(1),
  },
  icon: {
    '& svg': {
      width: 20,
      height: 20,
    },
  },
}));
export const PreparedLanguage: FC<
  components['schemas']['LanguageDto'] & {
    onReset: () => void;
    onEdit: () => void;
  }
> = (props) => {
  const classes = useStyles();

  return (
    <>
      <Box className={classes.root} data-cy="languages-prepared-language-box">
        {props.flagEmoji && (
          <FlagImage
            flagEmoji={props.flagEmoji}
            className={classes.flagImage}
          />
        )}{' '}
        <Box>
          {props.name} | {props.originalName} ({props.tag})
        </Box>
        <Box>
          <IconButton
            data-cy="languages-create-customize-button"
            size="small"
            className={clsx(classes.icon, classes.editButton)}
            onClick={props.onEdit}
          >
            <Edit />
          </IconButton>
          <IconButton
            size="small"
            className={clsx(classes.icon)}
            onClick={props.onReset}
            data-cy="languages-create-cancel-prepared-button"
          >
            <Close />
          </IconButton>
        </Box>
      </Box>
    </>
  );
};
