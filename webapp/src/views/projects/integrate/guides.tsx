import { default as React } from 'react';
import { Guide } from 'tg.views/projects/integrate/types';
import { Code, Settings } from '@mui/icons-material';

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
    name: 'React (CRA)',
    icon: getTechnologyImgComponent('react'),
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/React.mdx')
    ),
  },
  {
    name: 'Angular',
    icon: getTechnologyImgComponent('angular'),
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/Angular.mdx')
    ),
  },
  {
    name: 'Vue',
    icon: getTechnologyImgComponent('vue'),
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/Vue.mdx')
    ),
  },
  {
    name: 'Next.js',
    icon: getTechnologyImgComponent('next'),
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/Next.mdx')
    ),
  },
  {
    name: 'Gatsby',
    icon: getTechnologyImgComponent('gatsby'),
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/Gatsby.mdx')
    ),
  },
  {
    name: 'Php',
    icon: getTechnologyImgComponent('php'),
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/Php.mdx')
    ),
  },
  {
    name: 'Web',
    icon: Code,
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/Web.mdx')
    ),
  },
  {
    name: 'JS (NPM)',
    icon: getTechnologyImgComponent('js'),
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/Js.mdx')
    ),
  },
  {
    name: 'Rest',
    icon: Settings,
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/Rest.mdx')
    ),
  },
  {
    name: 'Svelte',
    icon: getTechnologyImgComponent('svelte'),
    guide: React.lazy(
      // @ts-ignore
      () => import('./guides/Svelte.mdx')
    ),
  },
] as Guide[];
