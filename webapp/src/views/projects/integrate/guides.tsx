import { default as React } from 'react';
import { Guide } from 'tg.views/projects/integrate/types';
import { Code, Settings, Terminal } from '@mui/icons-material';

const getTechnologyImgComponent = (imgName: string) => {
  return function TechnologyImage(props) {
    return (
      <img
        src={`/images/technologies/${imgName}.svg`}
        alt={imgName}
        {...props}
      />
    );
  };
};

const Test = () => <div />;

export const guides = [
  {
    name: 'React (VITE)',
    icon: getTechnologyImgComponent('react'),
    guide: Test,
  },
  {
    name: 'Angular',
    icon: getTechnologyImgComponent('angular'),
    guide: Test,
  },
  {
    name: 'Vue',
    icon: getTechnologyImgComponent('vue'),
    guide: Test,
  },
  {
    name: 'Next.js',
    icon: getTechnologyImgComponent('next'),
    guide: Test,
  },
  {
    name: 'Gatsby',
    icon: getTechnologyImgComponent('gatsby'),
    guide: Test,
  },
  {
    name: 'Web',
    icon: Code,
    guide: Test,
  },
  {
    name: 'JS (NPM)',
    icon: getTechnologyImgComponent('js'),
    guide: Test,
  },
  {
    name: 'Tolgee CLI',
    icon: Terminal,
    guide: Test,
  },
  {
    name: 'Rest',
    icon: Settings,
    guide: Test,
  },
  {
    name: 'Svelte',
    icon: getTechnologyImgComponent('svelte'),
    guide: Test,
  },
  {
    name: 'Figma',
    icon: getTechnologyImgComponent('figma'),
    guide: Test,
  },
  {
    name: 'Unreal',
    icon: getTechnologyImgComponent('unreal'),
    guide: Test,
  },
] as Guide[];
