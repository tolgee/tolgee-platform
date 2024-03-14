import React, { ComponentProps } from 'react';
import { SvgIcon } from '@mui/material';

import ExportSvg from '../svgs/icons/export.svg?react';
import ImportSvg from '../svgs/icons/import.svg?react';
import ProjectsSvg from '../svgs/icons/projects.svg?react';
import SettingsSvg from '../svgs/icons/settings.svg?react';
import TranslationSvg from '../svgs/icons/translation.svg?react';
import UserAddSvg from '../svgs/icons/user-add.svg?react';
import UserSettingSvg from '../svgs/icons/user-setting.svg?react';
import TranslationMemorySvg from '../svgs/icons/translationMemory.svg?react';
import MachineTranslationSvg from '../svgs/icons/machineTranslation.svg?react';
import TadaSvg from '../svgs/icons/tada.svg?react';
import RocketSvg from '../svgs/icons/rocket.svg?react';
import DropZoneSvg from '../svgs/icons/dropzone.svg?react';
import QSFinishedSvg from '../svgs/icons/qs-finished.svg?react';
import StarsSvg from '../svgs/icons/stars.svg?react';
import SlackSvg from '../svgs/icons/slack.svg?react';

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

export const DropzoneIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={DropZoneSvg} {...props} />
);

export const QSFinishedIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={QSFinishedSvg} {...props} />
);

export const StarsIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={StarsSvg} {...props} />
);

export const SlackIcon: React.FC<IconProps> = (props) => (
  <CustomIcon icon={SlackSvg} {...props} />
);
