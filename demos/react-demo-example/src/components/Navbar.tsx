import { LangSelector } from './LangSelector';

export const Navbar = ({ children }: React.PropsWithChildren) => {
  return (
    <div className="navbar">
      {children}
      <LangSelector />
    </div>
  );
};
