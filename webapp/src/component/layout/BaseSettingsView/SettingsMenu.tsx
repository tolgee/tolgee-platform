import { styled } from '@mui/material';
import { SettingsMenuItem } from './SettingsMenuItem';

const MenuList = styled('nav')`
  display: grid;
`;

export type SettingsMenuItem = {
  link: string;
  label: string;
  dataCy?: string;
};

type Props = {
  items: SettingsMenuItem[];
};

export const SettingsMenu: React.FC<Props> = ({ items }) => {
  return (
    <div data-cy="organization-side-menu">
      <MenuList>
        {items?.map((item, idx) => (
          <SettingsMenuItem
            data-cy={item.dataCy}
            key={idx}
            matchAsPrefix={true}
            linkTo={item.link}
            text={item.label}
          />
        ))}
      </MenuList>
    </div>
  );
};
