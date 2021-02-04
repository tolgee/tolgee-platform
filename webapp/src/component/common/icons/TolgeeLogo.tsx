import * as React from 'react';
import {FunctionComponent} from 'react';
import Logo from '../../../svgs/tolgeeLogo.svg'
import {SvgIcon, SvgIconProps} from "@material-ui/core";

export const TolgeeLogo: FunctionComponent<SvgIconProps> = (props) =>
    <SvgIcon {...props}><Logo opacity={0.99}/></SvgIcon>;