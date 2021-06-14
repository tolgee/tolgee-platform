import { Box, IconButton } from '@material-ui/core';
import { FlagImage } from './FlagImage';
import { Close, Edit } from '@material-ui/icons';
import React, { FC } from 'react';
import { components } from '../../service/apiSchema.generated';
import { makeStyles } from '@material-ui/core/styles';

const useStyles = makeStyles((theme) => ({
  root: {
    border: `1px solid ${theme.palette.grey['100']}`,
    padding: theme.spacing(1),
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
            className={classes.editButton}
            onClick={props.onEdit}
          >
            <Edit />
          </IconButton>
          <IconButton
            size="small"
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
