import { getTranslate } from '@/tolgee/server';
import Jokes from './Joke';
import { Navbar } from '@/components/Navbar';

import 'react-toastify/dist/ReactToastify.css';

export default async function IndexPage() {
  const t = await getTranslate();

  return (
    <div className="bg-faint-cross-lines">
      <div className="container mx-auto py-4">
        <Navbar />
        <div className="mt-20">
          <Jokes />
        </div>
      </div>
    </div>
  );
}
