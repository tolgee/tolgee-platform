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

export const guides = [
  {
    name: 'React (VITE)',
    icon: getTechnologyImgComponent('react'),
    guide: null, // React.lazy(() => import('./guides/React.mdx')),
  },
  {
    name: 'Angular',
    icon: getTechnologyImgComponent('angular'),
    guide: null, // React.lazy(() => import('./guides/Angular.mdx')),
  },
  {
    name: 'Vue',
    icon: getTechnologyImgComponent('vue'),
    guide: null, // React.lazy(() => import('./guides/Vue.mdx')),
  },
  {
    name: 'Next.js',
    icon: getTechnologyImgComponent('next'),
    guide: null, // React.lazy(() => import('./guides/Next.mdx')),
  },
  {
    name: 'Gatsby',
    icon: getTechnologyImgComponent('gatsby'),
    guide: null, // React.lazy(() => import('./guides/Gatsby.mdx')),
  },
  {
    name: 'Web',
    icon: Code,
    guide: null, // React.lazy(() => import('./guides/Web.mdx')),
  },
  {
    name: 'JS (NPM)',
    icon: getTechnologyImgComponent('js'),
    guide: null, // React.lazy(() => import('./guides/Js.mdx')),
  },
  {
    name: 'Tolgee CLI',
    icon: Terminal,
    guide: null, // React.lazy(() => import('./guides/Cli.mdx')),
  },
  {
    name: 'Rest',
    icon: Settings,
    guide: null, // React.lazy(() => import('./guides/Rest.mdx')),
  },
  {
    name: 'Svelte',
    icon: getTechnologyImgComponent('svelte'),
    guide: null, // React.lazy(() => import('./guides/Svelte.mdx')),
  },
  {
    name: 'Figma',
    icon: getTechnologyImgComponent('figma'),
    guide: null, // React.lazy(() => import('./guides/Figma.mdx')),
  },
  {
    name: 'Unreal',
    icon: getTechnologyImgComponent('unreal'),
    guide: null, // React.lazy(() => import('./guides/Unreal.mdx')),
  },
] as Guide[];
