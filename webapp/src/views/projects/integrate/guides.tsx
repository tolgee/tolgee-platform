import {
  AngularIcon,
  GatsbyIcon,
  JsIcon,
  NextIcon,
  PhpIcon,
  ReactIcon,
} from 'tg.component/CustomIcons';
import { default as React } from 'react';
import { Guide } from 'tg.views/projects/integrate/types';
import { Code, Settings } from '@material-ui/icons';

export const guides = [
  {
    name: 'React (CRA)',
    icon: ReactIcon,
    guide: React.lazy(
      // @ts-ignore
      () => import('!babel-loader!@mdx-js/loader!./guides/React.mdx')
    ),
  },
  {
    name: 'Angular',
    icon: AngularIcon,
    guide: React.lazy(
      // @ts-ignore
      () => import('!babel-loader!@mdx-js/loader!./guides/Angular.mdx')
    ),
  },
  {
    name: 'Next.js',
    icon: NextIcon,
    guide: React.lazy(
      // @ts-ignore
      () => import('!babel-loader!@mdx-js/loader!./guides/Next.mdx')
    ),
  },
  {
    name: 'Gatsby',
    icon: GatsbyIcon,
    guide: React.lazy(
      // @ts-ignore
      () => import('!babel-loader!@mdx-js/loader!./guides/Gatsby.mdx')
    ),
  },
  {
    name: 'Php',
    icon: PhpIcon,
    guide: React.lazy(
      // @ts-ignore
      () => import('!babel-loader!@mdx-js/loader!./guides/Php.mdx')
    ),
  },
  {
    name: 'Web',
    icon: Code,
    guide: React.lazy(
      // @ts-ignore
      () => import('!babel-loader!@mdx-js/loader!./guides/Web.mdx')
    ),
  },
  {
    name: 'JS (NPM)',
    icon: JsIcon,
    guide: React.lazy(
      // @ts-ignore
      () => import('!babel-loader!@mdx-js/loader!./guides/Js.mdx')
    ),
  },
  {
    name: 'Rest',
    icon: Settings,
    guide: React.lazy(
      // @ts-ignore
      () => import('!babel-loader!@mdx-js/loader!./guides/Rest.mdx')
    ),
  },
] as Guide[];
