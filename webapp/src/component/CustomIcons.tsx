import { SvgIcon } from '@material-ui/core';
import React, { ComponentProps } from 'react';
import { ReactComponent as ExportSvg } from '../svgs/icons/export.svg';
import { ReactComponent as ImportSvg } from '../svgs/icons/import.svg';
import { ReactComponent as ProjectsSvg } from '../svgs/icons/projects.svg';
import { ReactComponent as SettingsSvg } from '../svgs/icons/settings.svg';
import { ReactComponent as TranslationSvg } from '../svgs/icons/translation.svg';
import { ReactComponent as UserAddSvg } from '../svgs/icons/user-add.svg';
import { ReactComponent as UserSettingSvg } from '../svgs/icons/user-setting.svg';

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
