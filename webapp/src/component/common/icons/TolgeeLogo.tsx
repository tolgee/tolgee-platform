import { FunctionComponent } from 'react';
import { SvgIcon, SvgIconProps } from '@mui/material';

import { ReactComponent as Logo } from 'tg.svgs/tolgeeLogo.svg';

export const TolgeeLogo: FunctionComponent<SvgIconProps> = (props) => (
  <SvgIcon {...props}>
    <Logo opacity={0.99} />
  </SvgIcon>
);
