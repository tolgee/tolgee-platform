import { FunctionComponent } from 'react';
import { SvgIcon, SvgIconProps } from '@mui/material';

import Logo from 'tg.svgs/tolgeeLogo.svg?react';

export const TolgeeLogo: FunctionComponent<SvgIconProps> = (props) => (
  <SvgIcon {...props}>
    <Logo opacity={0.99} />
  </SvgIcon>
);
