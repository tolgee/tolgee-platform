import { getTranslate } from '@/tolgee/server';
import App from './App';
import { Navbar } from '@/components/Navbar';

export default async function IndexPage() {
  const t = await getTranslate();

  return (
<div className="relative w-full  overflow-hidden min-h-screen sm:h-auto sm:overflow-y-auto overflow-y-scroll bg-slate-950 animate-gradient-x border-b border-gray-800 shadow-2xl flex-col justify-center items-center">     
        <div className="z-100 h-20 ">
          {/* Pass the translation function t to Navbar */}
          <Navbar t={t} />
        </div>
        <div className="flex-1 flex justify-center items-center ">
          <App />
        </div>
    </div>
  );
}
