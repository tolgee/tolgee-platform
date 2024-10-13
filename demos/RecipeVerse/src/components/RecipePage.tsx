// import { useState} from 'react';
// import { Menu, Instagram, Twitter, Facebook } from 'lucide-react';
// import Recipe from './Recipe';

// export default function RecipePage() {
//   const [isOpen, setIsOpen] = useState(false);

//   const toggleMenu = () => {
//     setIsOpen(!isOpen);
//   };

//   return (
//     <div className="flex flex-col min-h-screen bg-gray-100">
      
//       {/* Navigation */}
//       <nav className=" bg-white shadow-md py-4">
//         <div className="container mx-auto px-4 flex items-center justify-between">
//           {/* Brand Name */}
//           <div className="text-5xl font-bold text-gray-900">
//             RecipeVerse
//           </div>

//           {/* Hamburger Menu */}
//           <div className="lg:hidden">
//             <button onClick={toggleMenu}>
//               <Menu className="h-8 w-8 text-gray-800" />
//             </button>
//           </div>

//           {/* Links */}
//           <div className={`lg:flex space-x-8 font-medium text-lg ${isOpen ? 'block' : 'hidden'} lg:block`}>
//             <a href="#" className= "text-3xl hover:text-blue-500 transition-colors">Home</a>
//             <a href="#" className="text-3xl hover:text-blue-500 transition-colors">Contact</a>
//             <a href="#" className="text-3xl hover:text-blue-500 transition-colors">About Us</a>
//           </div>
//         </div>
//       </nav>

//       {/* Main Content */}
//       <main className="flex-grow container mx-auto px-4 py-8">
//         <Recipe />
//       </main>

//       {/* Footer */}
//       <footer className="bg-gray-900 text-white py-6">
//         <div className=" container mx-auto text-center">
//           <p>Made with love by Sohaib Shaikh© 2024 RecipeVerse. All rights reserved.</p>
//           <p className="mt-2">Follow us on:</p>
//           <div className="flex justify-center space-x-4 mt-4">
//             <a href="#" aria-label="Instagram">
//               <Instagram className="h-10 w-10 text-red-400 hover:text-white transition-colors " />
//             </a>
//             <a href="#" aria-label="Twitter">
//               <Twitter className="h-10 w-10 text-blue-400 hover:text-white transition-colors" />
//             </a>
//             <a href="#" aria-label="Facebook">
//               <Facebook className="h-10 w-10 text-blue-600 hover:text-white transition-colors" />
//             </a>
//           </div>
//         </div>
//       </footer>
//     </div>
//   );
// }


import { useState } from 'react';
import { Menu, Instagram, Facebook, Twitter } from 'lucide-react';
import Recipe from './Recipe';

export default function RecipePage() {
  const [isOpen, setIsOpen] = useState(false);
  const [favorites, setFavorites] = useState<string[]>([]);

  const toggleMenu = () => {
    setIsOpen(!isOpen);
  };

  const addToFavorites = (recipeTitle: string) => {
    setFavorites((prevFavorites) => [...prevFavorites, recipeTitle]);
  };

  return (
    <div className="flex flex-col min-h-screen bg-blue-200">
      {/* Navigation */}
      <nav className="bg-white shadow-md py-4">
        <div className="container mx-auto px-4 flex items-center justify-between">
          {/* Brand Name */}
          <div className="text-5xl font-bold text-gray-900">
            RecipeVerse
          </div>

          {/* Hamburger Menu */}
          <div className="lg:hidden">
            <button onClick={toggleMenu}>
              <Menu className="h-8 w-8 text-gray-800" />
            </button>
          </div>

          {/* Links */}
          <div className={`lg:flex space-x-8 font-medium text-lg ${isOpen ? 'block' : 'hidden'} lg:block`}>
            <a href="#" className="text-3xl hover:text-blue-500 transition-colors">Home</a>
            <a href="#" className="text-3xl hover:text-blue-500 transition-colors">Contact</a>
            <a href="#" className="text-3xl hover:text-blue-500 transition-colors">About Us</a>
          </div>
        </div>
      </nav>

      {/* Main Content */}
      <main className="flex-grow container mx-auto px-4 py-8">
        <Recipe onAddToFavorites={addToFavorites} />

        {/* Favorites Section */}
        <section className="mt-8">
          <h2 className="text-3xl font-bold text-gray-900">Your Favorite Recipes</h2>
          <div className="mt-4 grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
            {favorites.map((recipeTitle, index) => (
              <div key={index} className="bg-white shadow-lg rounded-lg p-4">
                <h3 className="text-xl font-semibold">{recipeTitle}</h3>
              </div>
            ))}
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="bg-gray-900 text-white py-6">
        <div className="container mx-auto text-center">
          <p>Made with love by Sohaib Shaikh © 2024 RecipeVerse. All rights reserved.</p>
          <p className="mt-2">Follow us on:</p>
          <div className="flex justify-center space-x-4 mt-4">
            <a href="#" aria-label="Instagram">
              <Instagram className="h-10 w-10 text-red-400 hover:text-white transition-colors" />
            </a>
            <a href="#" aria-label="Twitter">
              <Twitter className="h-10 w-10 text-blue-400 hover:text-white transition-colors" />
            </a>
            <a href="#" aria-label="Facebook">
              <Facebook className="h-10 w-10 text-blue-600 hover:text-white transition-colors" />
            </a>
          </div>
        </div>
      </footer>
    </div>
  );
}

