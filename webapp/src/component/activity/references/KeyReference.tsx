import React from 'react';
import clsx from 'clsx';
import { Link } from 'react-router-dom';
import { Link as MuiLink } from '@mui/material';

import { LINKS, PARAMS } from 'tg.constants/links';
import { useProject } from 'tg.hooks/useProject';
import { queryEncode } from 'tg.hooks/useUrlSearchState';
import { KeyReferenceData } from '../types';
import { CircledLanguageIcon } from 'tg.component/languages/CircledLanguageIcon';

type Props = {
  data: KeyReferenceData;
};

export const KeyReference: React.FC<Props> = ({ data }) => {
  const project = useProject();

  const href = data.exists
    ? LINKS.PROJECT_TRANSLATIONS_SINGLE.build({
        [PARAMS.PROJECT_ID]: project.id,
      }) +
      queryEncode({
        id: data.id,
      })
    : undefined;

  const content = (
    <>
      <span className="referenceText">
        {data.namespace && (
          <span className="referencePrefix">{data.namespace}</span>
        )}
        {data.keyName}
        {data.languages && ' '}
      </span>
      {data.languages?.map((l, i) => (
        <React.Fragment key={i}>
          <CircledLanguageIcon size={14} flag={l.flagEmoji} />
          {i + 1 < data.languages!.length && ' '}
        </React.Fragment>
      )) || []}
    </>
  );

  const classes = ['reference', 'referenceComposed'];
  return href ? (
    <MuiLink
      component={Link}
      to={href}
      className={clsx(classes, 'referenceLink')}
    >
      {content}
    </MuiLink>
  ) : (
    <span className={clsx(classes)}>{content}</span>
  );
};
