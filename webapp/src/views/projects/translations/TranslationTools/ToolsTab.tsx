import React from 'react';
import { makeStyles, Typography } from '@material-ui/core';
import { useTranslate } from '@tolgee/react';

import { TabMessage } from './TabMessage';
import { UseQueryResult } from 'react-query';

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    flexDirection: 'column',
    minWidth: 0,
  },
  tab: {
    display: 'flex',
    alignItems: 'center',
    gap: theme.spacing(1),
    padding: theme.spacing(0.5, 1),
    background: theme.palette.extraLightBackground.main,
    borderBottom: `1px solid ${theme.palette.extraLightDivider.main}`,
    textTransform: 'uppercase',
    color: '#808080',
    position: 'sticky',
    top: '0px',
    height: 32,
    flexShrink: 1,
    flexBasis: 0,
  },
  badge: {
    background: theme.palette.lightBackground.main,
    padding: '2px 4px',
    borderRadius: '12px',
    fontSize: 12,
    height: 20,
    minWidth: 20,
    boxSizing: 'border-box',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    fontSize: 14,
  },
  content: {},
}));

type Props = {
  icon: React.ReactNode;
  title: string;
  badgeNumber?: number;
  data?: UseQueryResult<unknown, any>;
};

export const ToolsTab: React.FC<Props> = ({
  icon,
  title,
  badgeNumber,
  children,
  data,
}) => {
  const classes = useStyles();
  const t = useTranslate();

  const getErrorMessage = (code: string) => {
    switch (code) {
      case 'out_of_credits':
        return t('translation_tools_no_credits');
      default:
        return code;
    }
  };

  const error = data?.error;
  const errorCode = error?.message || error?.code || error || 'Unknown error';

  const errorMessage = getErrorMessage(errorCode);

  return (
    <div className={classes.container}>
      <div className={classes.tab}>
        {icon}
        <Typography variant="button" className={classes.title}>
          {title}
        </Typography>
        {badgeNumber ? (
          <div className={classes.badge}>{badgeNumber}</div>
        ) : null}
      </div>

      {data?.isError ? (
        <TabMessage type="error" message={errorMessage} />
      ) : (
        children
      )}
    </div>
  );
};
