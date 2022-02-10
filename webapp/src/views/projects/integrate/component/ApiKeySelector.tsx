import { default as React, FC, useState } from 'react';
import {
  Box,
  FormControl,
  makeStyles,
  MenuItem,
  Select,
} from '@material-ui/core';
import { components } from 'tg.service/apiSchema.generated';
import { BoxLoading } from 'tg.component/common/BoxLoading';
import { T } from '@tolgee/react';
import { Add } from '@material-ui/icons';
import clsx from 'clsx';
import { AddApiKeyFormDialog } from 'tg.views/userSettings/apiKeys/AddApiKeyFormDialog';
import { useProject } from 'tg.hooks/useProject';

const useStyles = makeStyles((t) => ({
  itemWrapper: {
    maxWidth: 400,
  },
  scopes: {
    fontSize: 11,
    whiteSpace: 'normal',
  },
  addIcon: {
    marginRight: t.spacing(1),
  },
  addItem: {
    display: 'flex',
    alignItems: 'center',
    color: t.palette.primary.main,
    paddingTop: t.spacing(1),
    paddingBottom: t.spacing(1),
  },
}));

export const ApiKeySelector: FC<{
  selected: components['schemas']['ApiKeyModel'] | undefined;
  onSelect: (key: components['schemas']['ApiKeyModel']) => void;
  keys?: components['schemas']['ApiKeyModel'][];
  keysLoading: boolean;
  onNewCreated: (key: components['schemas']['ApiKeyModel']) => void;
}> = (props) => {
  const classes = useStyles();

  const [addDialogOpen, setAddDialogOpen] = useState(false);

  const findKey = (id: number) => props.keys?.find((k) => k.id === id);

  const onSelect = (id: number) => {
    if (id === 0) {
      setAddDialogOpen(true);
      return;
    }
    props.onSelect(findKey(id)!);
  };

  const project = useProject();

  return (
    <>
      {!props.keysLoading ? (
        <FormControl variant="outlined" style={{ minWidth: 400 }}>
          <Select
            data-cy="integrate-api-key-selector-select"
            id="api-key-select"
            value={props.selected?.id || ''}
            inputProps={{
              'data-cy': 'integrate-api-key-selector-select-input',
            }}
            renderValue={(id) => {
              return (
                <span data-openreplay-masked="">
                  {findKey(id as number)?.key}
                </span>
              );
            }}
            onChange={(e) => onSelect(e.target.value as number)}
          >
            {props.keys?.map((k) => (
              <MenuItem
                key={k.id}
                value={k.id}
                data-cy="integrate-api-key-selector-item"
              >
                <Box className={classes.itemWrapper}>
                  <Box data-openreplay-masked="">{k.key}</Box>
                  <Box className={classes.scopes}>{k.scopes.join(', ')}</Box>
                </Box>
              </MenuItem>
            ))}
            <MenuItem
              value={0}
              data-cy="integrate-api-key-selector-create-new-item"
            >
              <Box className={clsx(classes.itemWrapper, classes.addItem)}>
                <T>api_key_selector_create_new</T>
                <Add fontSize="small" className={classes.addIcon} />
              </Box>
            </MenuItem>
          </Select>
        </FormControl>
      ) : (
        <BoxLoading p={1} />
      )}
      {addDialogOpen && (
        <AddApiKeyFormDialog
          project={project}
          onClose={() => {
            setAddDialogOpen(false);
          }}
          onSaved={(key) => {
            setAddDialogOpen(false);
            props.onNewCreated(key);
          }}
        />
      )}
    </>
  );
};
