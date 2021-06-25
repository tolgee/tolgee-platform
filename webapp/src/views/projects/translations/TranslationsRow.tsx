import { Box, Checkbox } from '@material-ui/core';
import { grey } from '@material-ui/core/colors';
import { createStyles, makeStyles } from '@material-ui/core/styles';
import React, { FunctionComponent, useContext } from 'react';
import { useProjectPermissions } from 'tg.hooks/useProjectPermissions';
import { components } from 'tg.service/apiSchema.generated';
import { ProjectPermissionType } from 'tg.service/response.types';
import { KeyCell } from './KeyCell';
import { KeyScreenshots } from './Screenshots/KeySreenshots';
import { TableCell } from './TableCell';
import { TranslationCell } from './TranslationCell';
import { TranslationListContext } from './TtranslationsGridContextProvider';

type KeyTranslationsDTO =
  components['schemas']['KeyWithTranslationsResponseDto'];

export interface TranslationProps {
  data: KeyTranslationsDTO;
}

export type RowContextType = {
  data: KeyTranslationsDTO;
  lastRendered: number;
};

export const RowContext = React.createContext<RowContextType>({
  //@ts-ignore
  data: null,
  lastRendered: 0,
});

const useStyles = makeStyles(() =>
  createStyles({
    moreButton: {
      opacity: '0.8',
      padding: 0,
    },
    lineBox: {
      borderBottom: '1px solid ' + grey[100],
      '&:last-child': {
        borderBottom: 'none',
      },
    },
  })
);

export const TranslationsRow: FunctionComponent<TranslationProps> = (props) => {
  const classes = useStyles({});

  const listContext = useContext(TranslationListContext);
  const permissions = useProjectPermissions();

  const contextValue: RowContextType = {
    lastRendered: 0,
    data: props.data,
  };

  return (
    <Box display="flex" className={classes.lineBox}>
      <RowContext.Provider value={contextValue}>
        {permissions.satisfiesPermission(ProjectPermissionType.EDIT) && (
          <Box
            display="flex"
            alignItems="center"
            justifyContent="start"
            style={{ width: 40, flexShrink: 0 }}
          >
            <Checkbox
              data-cy="translations-row-checkbox"
              onChange={() =>
                listContext.toggleKeyChecked(contextValue.data.id as number)
              }
              checked={listContext.isKeyChecked(contextValue.data.id as number)}
              size="small"
              style={{ padding: 0 }}
            />
          </Box>
        )}
        <Box display="flex" flexGrow={1} minWidth={0}>
          {listContext.showKeys && (
            <TableCell>
              <KeyCell />
            </TableCell>
          )}

          {listContext.listLanguages.map((k) => (
            <TableCell key={k}>
              <TranslationCell tag={k} />
            </TableCell>
          ))}
        </Box>
        <KeyScreenshots data={props.data} />
      </RowContext.Provider>
    </Box>
  );
};
