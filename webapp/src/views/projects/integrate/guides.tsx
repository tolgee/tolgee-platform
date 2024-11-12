import { default as React } from 'react';
import { Guide } from 'tg.views/projects/integrate/types';
import { Code01, Settings01, Terminal } from '@untitled-ui/icons-react';

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
    guide: React.lazy(() => import('./guides/React.mdx')),
  },
  {
    name: 'Angular',
    icon: getTechnologyImgComponent('angular'),
    guide: React.lazy(() => import('./guides/Angular.mdx')),
  },
  {
    name: 'Vue',
    icon: getTechnologyImgComponent('vue'),
    guide: React.lazy(() => import('./guides/Vue.mdx')),
  },
  {
    name: 'Next.js',
    icon: getTechnologyImgComponent('next'),
    guide: React.lazy(() => import('./guides/Next.mdx')),
  },
  {
    name: 'Web',
    icon: Code01,
    guide: React.lazy(() => import('./guides/Web.mdx')),
  },
  {
    name: 'JS (NPM)',
    icon: getTechnologyImgComponent('js'),
    guide: React.lazy(() => import('./guides/Js.mdx')),
  },
  {
    name: 'Tolgee CLI',
    icon: Terminal,
    guide: React.lazy(() => import('./guides/Cli.mdx')),
  },
  {
    name: 'Rest',
    icon: Settings01,
    guide: React.lazy(() => import('./guides/Rest.mdx')),
  },
  {
    name: 'Svelte',
    icon: getTechnologyImgComponent('svelte'),
    guide: React.lazy(() => import('./guides/Svelte.mdx')),
  },
  {
    name: 'Figma',
    icon: getTechnologyImgComponent('figma'),
    guide: React.lazy(() => import('./guides/Figma.mdx')),
  },
  {
    name: 'Unreal',
    icon: getTechnologyImgComponent('unreal'),
    guide: React.lazy(() => import('./guides/Unreal.mdx')),
  },
] as Guide[];
