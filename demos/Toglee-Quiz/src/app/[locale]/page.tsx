import { getTranslate } from '@/tolgee/server';
import App from './App';
import { Navbar } from '@/components/Navbar';

export default async function IndexPage() {
  const t = await getTranslate();

  return (
    <div className="bg-slate-900 w-full flex justify-center items-center  min-h-screen">
      <div className="container py-4 ">
        <div className="z-100 w-auto">
          {/* Pass the translation function t to Navbar */}
          <Navbar t={t} />
        </div>
        <div className="w-full flex justify-center items-center mt-14 bg-[#0f1525]">
          <App />
        </div>
      </div>
    </div>
  );
}
