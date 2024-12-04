// import { useState, useEffect } from 'react';

// interface Recipe {
//   title: string;
//   image: string;
//   summary: string;
//   sourceUrl: string;
// }

// export default function Recipe() {
//   const [recipe, setRecipe] = useState<Recipe | null>(null);
//   const [loading, setLoading] = useState<boolean>(true);
//   const [error, setError] = useState<Error | null>(null);

//   const fetchRecipe = async () => {
//     try {
//       setLoading(true);
//       const response = await fetch('https://api.spoonacular.com/recipes/random?apiKey=5b1bbeb6b0a14126a5924f6309b05369');
//       if (!response.ok) {
//         throw new Error(`HTTP error! status: ${response.status}`);
//       }
//       const data = await response.json();
//       if (!data.recipes || data.recipes.length === 0) {
//         throw new Error('No recipe data received');
//       }
//       setRecipe(data.recipes[0] as Recipe);
//     } catch (err) {
//       console.error('Error fetching recipe:', err);
//       setError(err as Error);
//     } finally {
//       setLoading(false);
//     }
//   };

//   useEffect(() => {
//     fetchRecipe();
//   }, []);

//   if (loading) return <div>Loading...</div>;
//   if (error) return <div>Error fetching recipe: {error.message}</div>;
//   if (!recipe) return <div>No recipe data available</div>;

//   return (
//     <div className="max-w-3xl mx-auto bg-white shadow-lg rounded-lg overflow-hidden">
//       {recipe.image && <img className="w-full h-full object-cover" src={recipe.image} alt={recipe.title} />}
//       <div className="p-4">
//         <h2 className="text-5xl font-bold text-black" >{recipe.title}</h2>
//         <p className="mt-2 text-black">{recipe.summary?.replace(/<\/?[^>]+(>|$)/g, "")}</p>
//         {recipe.sourceUrl && <a href={recipe.sourceUrl} className="mt-4 inline-block text-blue-500">View Recipe</a>}
//         <button 
//           className="mt-8 ml-4 bg-blue-500 text-white px-4 py-2 rounded" 
//           onClick={fetchRecipe}
//         >
//           Get Another Recipe
//         </button>
//       </div>
//     </div>
//   );
// }

import React, { useState, useEffect } from 'react';

interface Recipe {
  title: string;
  image: string;
  summary: string;
  sourceUrl: string;
}

interface RecipeProps {
  onAddToFavorites: (recipeTitle: string) => void;
}

export default function Recipe({ onAddToFavorites }: RecipeProps) {
  const [recipe, setRecipe] = useState<Recipe | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<Error | null>(null);

  const fetchRecipe = async () => {
    try {
      setLoading(true);
      const response = await fetch('https://api.spoonacular.com/recipes/random?apiKey=5b1bbeb6b0a14126a5924f6309b05369');
      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }
      const data = await response.json();
      if (!data.recipes || data.recipes.length === 0) {
        throw new Error('No recipe data received');
      }
      setRecipe(data.recipes[0] as Recipe);
    } catch (err) {
      console.error('Error fetching recipe:', err);
      setError(err as Error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRecipe();
  }, []);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error fetching recipe: {error.message}</div>;
  if (!recipe) return <div>No recipe data available</div>;

  return (
    <div className="max-w-3xl mx-auto bg-white shadow-lg rounded-lg overflow-hidden">
      {recipe.image && <img className="w-full h-full object-cover" src={recipe.image} alt={recipe.title} />}
      <div className="p-4">
        <h2 className="text-5xl font-bold text-black">{recipe.title}</h2>
        <p className="mt-2 text-black">{recipe.summary?.replace(/<\/?[^>]+(>|$)/g, "")}</p>
        {recipe.sourceUrl && <a href={recipe.sourceUrl} className="mt-4 inline-block text-blue-500">View Recipe</a>}
        <div className="mt-8">
          <button 
            className="bg-blue-500 text-white px-4 py-2 rounded mr-4" 
            onClick={fetchRecipe}
          >
            Get Another Recipe
          </button>
          <button 
            className="bg-green-500 text-white px-4 py-2 rounded" 
            onClick={() => onAddToFavorites(recipe.title)}
          >
            Add to Favorites
          </button>
        </div>
      </div>
    </div>
  );
}
