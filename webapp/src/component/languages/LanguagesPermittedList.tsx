import { makeStyles } from '@material-ui/core';
import clsx from 'clsx';
import { T } from '@tolgee/react';

import { components } from 'tg.service/apiSchema.generated';
import { CircledLanguageIcon } from './CircledLanguageIcon';

type LanguageModel = components['schemas']['LanguageModel'];

const useStyles = makeStyles((theme) => ({
  container: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  extraCircle: {
    boxSizing: 'border-box',
    width: 20,
    height: 20,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    margin: 2,
    background: theme.palette.grey[400],
    borderRadius: '50%',
    fontSize: 10,
  },
}));

type Props = React.HTMLAttributes<HTMLDivElement> & {
  languages?: LanguageModel[];
};

export const LanguagesPermittedList: React.FC<Props> = ({
  languages,
  className,
  ...props
}) => {
  const classes = useStyles();
  const selectedLanguages = languages?.slice(0, 3) || [];

  const numOfExtra = (languages?.length || 0) - selectedLanguages.length;

  return (
    <div className={clsx(classes.container, className)} {...props}>
      {!selectedLanguages.length ? (
        <T keyName="languages_permitted_list_all" />
      ) : (
        selectedLanguages.map((l) => (
          <CircledLanguageIcon key={l.id} size={20} flag={l.flagEmoji} />
        ))
      )}
      {numOfExtra > 0 && (
        <div className={classes.extraCircle}>+{numOfExtra}</div>
      )}
    </div>
  );
};
