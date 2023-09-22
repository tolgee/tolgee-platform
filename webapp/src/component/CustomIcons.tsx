import React, { ComponentProps } from 'react';
import { SvgIcon } from '@mui/material';

import { ReactComponent as ExportSvg } from '../svgs/icons/export.svg';
import { ReactComponent as ImportSvg } from '../svgs/icons/import.svg';
import { ReactComponent as ProjectsSvg } from '../svgs/icons/projects.svg';
import { ReactComponent as SettingsSvg } from '../svgs/icons/settings.svg';
import { ReactComponent as TranslationSvg } from '../svgs/icons/translation.svg';
import { ReactComponent as UserAddSvg } from '../svgs/icons/user-add.svg';
import { ReactComponent as UserSettingSvg } from '../svgs/icons/user-setting.svg';
import { ReactComponent as TranslationMemorySvg } from '../svgs/icons/translationMemory.svg';
import { ReactComponent as MachineTranslationSvg } from '../svgs/icons/machineTranslation.svg';
import { ReactComponent as TadaSvg } from '../svgs/icons/tada.svg';
import { ReactComponent as RocketSvg } from '../svgs/icons/rocket.svg';

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
export const TranslationMemoryIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={TranslationMemorySvg} {...props} />
);
export const MachineTranslationIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={MachineTranslationSvg} {...props} />
);
export const TadaIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={TadaSvg} {...props} />
);
export const RocketIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={RocketSvg} {...props} />
);
