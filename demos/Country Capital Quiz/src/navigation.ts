import { createSharedPathnamesNavigation } from 'next-intl/navigation';
import { ALL_LOCALES } from './tolgee/shared';

// read more about next-intl library
// https://next-intl-docs.vercel.app
export const { Link, redirect, usePathname, useRouter } =
  createSharedPathnamesNavigation({ locales: ALL_LOCALES });
