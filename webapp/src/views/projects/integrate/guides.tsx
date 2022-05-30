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
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/React.mdx')
    // ),
  },
  {
    name: 'Angular',
    icon: getTechnologyImgComponent('angular'),
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/Angular.mdx')
    // ),
  },
  {
    name: 'Vue',
    icon: getTechnologyImgComponent('vue'),
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/Vue.mdx')
    // ),
  },
  {
    name: 'Next.js',
    icon: getTechnologyImgComponent('next'),
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/Next.mdx')
    // ),
  },
  {
    name: 'Gatsby',
    icon: getTechnologyImgComponent('gatsby'),
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/Gatsby.mdx')
    // ),
  },
  {
    name: 'Php',
    icon: getTechnologyImgComponent('php'),
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/Php.mdx')
    // ),
  },
  {
    name: 'Web',
    icon: Code,
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/Web.mdx')
    // ),
  },
  {
    name: 'JS (NPM)',
    icon: getTechnologyImgComponent('js'),
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/Js.mdx')
    // ),
  },
  {
    name: 'Rest',
    icon: Settings,
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/Rest.mdx')
    // ),
  },
  {
    name: 'Svelte',
    icon: getTechnologyImgComponent('svelte'),
    guide: null as any,
    // React.lazy(
    //   // @ts-ignore
    //   () => import('!babel-loader!@mdx-js/loader!./guides/Svelte.mdx')
    // ),
  },
] as Guide[];
