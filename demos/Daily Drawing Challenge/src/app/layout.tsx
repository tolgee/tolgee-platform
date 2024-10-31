import { ReactNode } from 'react';
import './style.css';
import { Metadata } from 'next';

export const metadata: Metadata = {
  title: "Daily Mini Drawing Challenge",
  description: "Created by Sanket shinde",
};

type Props = {
  children: ReactNode;
};

// Since we have a `not-found.tsx` page on the root, a layout file
// is required, even if it's just passing children through.
export default function RootLayout({ children }: Props) {
  return children;
}
