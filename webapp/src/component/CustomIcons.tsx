import React, { ComponentProps } from 'react';
import { SvgIcon } from '@material-ui/core';

import { ReactComponent as ExportSvg } from '../svgs/icons/export.svg';
import { ReactComponent as ImportSvg } from '../svgs/icons/import.svg';
import { ReactComponent as ProjectsSvg } from '../svgs/icons/projects.svg';
import { ReactComponent as SettingsSvg } from '../svgs/icons/settings.svg';
import { ReactComponent as TranslationSvg } from '../svgs/icons/translation.svg';
import { ReactComponent as UserAddSvg } from '../svgs/icons/user-add.svg';
import { ReactComponent as UserSettingSvg } from '../svgs/icons/user-setting.svg';
import { ReactComponent as ReactSvg } from '../svgs/icons/react.svg';
import { ReactComponent as AngularSvg } from '../svgs/icons/angular.svg';
import { ReactComponent as NextSvg } from '../svgs/icons/next.svg';
import { ReactComponent as GatsbySvg } from '../svgs/icons/gatsby.svg';
import { ReactComponent as PhpSvg } from '../svgs/icons/php.svg';
import { ReactComponent as JsSvg } from '../svgs/icons/js.svg';

type IconProps = ComponentProps<typeof SvgIcon>;

const CustomIcon: React.FC<IconProps & { icon: typeof ExportSvg }> = ({
  icon,
  ...props
}) => {
  const Icon = icon;
  return (
    <SvgIcon {...props}>
      <Icon fill="currentColor" />
    </SvgIcon>
  );
};

export const ExportIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={ExportSvg} {...props} />
);
export const ImportIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={ImportSvg} {...props} />
);
export const ProjectsIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={ProjectsSvg} {...props} />
);
export const SettingsIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={SettingsSvg} {...props} />
);
export const TranslationIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={TranslationSvg} {...props} />
);
export const UserAddIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={UserAddSvg} {...props} />
);
export const UserSettingIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={UserSettingSvg} {...props} />
);

export const ReactIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={ReactSvg} {...props} />
);

export const AngularIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={AngularSvg} {...props} />
);

export const NextIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={NextSvg} {...props} />
);

export const GatsbyIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={GatsbySvg} {...props} />
);

export const PhpIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={PhpSvg} {...props} />
);

export const JsIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={JsSvg} {...props} />
);
