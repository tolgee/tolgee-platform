import { getTranslate } from '@/tolgee/server';
import App from './App';
import { Navbar } from '@/components/Navbar';

export default async function IndexPage() {
  const t = await getTranslate();

  return (
    <div className="bg-slate-900  flex justify-center items-center  min-h-screen">
      <div className="container ">
        <div className="z-100 ">
          {/* Pass the translation function t to Navbar */}
          <Navbar t={t} />
        </div>
        <div className="flex justify-center items-center bg-[#0f1525]">
          <App />
        </div>
      </div>
    </div>
  );
}
