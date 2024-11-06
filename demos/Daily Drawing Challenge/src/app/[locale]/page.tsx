import { getTranslate } from '@/tolgee/server';
import Draw from './Draw';
import { Navbar } from '@/components/Navbar';

import 'react-toastify/dist/ReactToastify.css';

export default async function IndexPage() {
  const t = await getTranslate();

  return (
    <div className="bg-faint-cross-lines">
      <Navbar />
      <div className='pt-20'>
        <Draw />
      </div>
          
          <footer className=" bg-tranparent text-white py-4 text-center">
            <p>Made by <a href="https://github.com/sanketshinde3001" target='blank' className='text-blue-400'>Sanket Shinde</a></p>
          </footer>
        </div>
  );
}
