import * as React from 'react';
import {FunctionComponent} from 'react';
import Logo from '../../../svgs/polygloatLogo.svg'
import {SvgIcon, SvgIconProps} from "@material-ui/core";

export const PolygloatLogo: FunctionComponent<SvgIconProps> = (props) =>
    <SvgIcon {...props}><Logo opacity={0.99}/></SvgIcon>;