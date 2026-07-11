import React, { useMemo } from 'react';
import { styled, Tooltip, useTheme } from '@mui/material';
import { T } from '@tolgee/react';
import { LinkReadMore } from 'tg.component/LinkReadMore';
import { DOCS_LINKS } from 'tg.constants/docLinks';
import { generateKeyNameStyle } from '@tginternal/editor';

import { splitKeyName } from 'tg.fixtures/keyName';

const StyledRoot = styled('span')`
  display: inline;
`;

type Props = {
  name: string;
  className?: string;
  ['data-cy']?: string;
};

export const KeyName: React.FC<React.PropsWithChildren<Props>> = ({
  name,
  className,
  'data-cy': dataCy,
}) => {
  const theme = useTheme();
  const { msgctxt, msgid } = splitKeyName(name);

  const Wrapper = useMemo(
    () =>
      generateKeyNameStyle({
        styled,
        component: StyledRoot,
      }),
    [theme.palette.mode]
  );

  if (!msgctxt) {
    return (
      <span className={className} data-cy={dataCy}>
        {name}
      </span>
    );
  }

  return (
    <Wrapper className={className} data-cy={dataCy}>
      <Tooltip
        title={
          <T
            keyName="translations_key_msgctxt_tooltip"
            params={{
              LearnMore: <LinkReadMore url={DOCS_LINKS.poMsgctxt} />,
            }}
          />
        }
        enterDelay={200}
        leaveDelay={200}
      >
        <span className="keyname-msgctxt-widget" data-cy="key-name-msgctxt">
          {msgctxt}
        </span>
      </Tooltip>
      {msgid}
    </Wrapper>
  );
};
