import { getTranslate } from '@/tolgee/server';
import { Navbar } from '@/components/Navbar';
import Book from '@/components/Book';


export default async function IndexPage() {
  const t = await getTranslate();

  return (
    <div className="">
      <div className="container mx-auto py-4">
        <Navbar />
      </div>
      <div className="mt-12">
        <Book />
        </div>
    </div>
  );
}
