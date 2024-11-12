'use client';

import { useState } from 'react';
import { useTranslate } from '@tolgee/react';
import RecipeForm from "@/components/RecipeForm";
import RecipeCard from "@/components/RecipeCard";
import Modal from "@/components/Modal";
import { usePathname } from 'next/navigation'
import { toast } from 'react-toastify';

// Define types for Recipe and the API response
interface Recipe {
  id: number;
  name: string;
  ingredients: string[];
  instructions: string[];
  tips: string[];
  createdAt: Date;
}

interface RecipeAPIResponse {
  recipename: string;
  ingredients: string[];
  instructions: string[];
  tips: string[];
}

export default function Recipes() {
  const pathname = usePathname()
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [selectedRecipe, setSelectedRecipe] = useState<Recipe | null>(null); // To track the currently selected recipe
  const [isModalOpen, setIsModalOpen] = useState(false); // To control the modal
  const [isrunnig, setisrunnig] = useState(false);

  const getLanguageName = (path: string) => {
    const languageMap: { [key: string]: string } = {
      en: "English",
      ar: "Arabic",
      hi: "Hindi",
      es: "Spanish",
    };

    const locale = path.replace('/', '');

    return languageMap[locale] || "English";
  };

  const generateRecipeDetails = async (recipeName: string): Promise<RecipeAPIResponse | null> => {
    try {
      const lang = getLanguageName(pathname);
      const response = await fetch("/api/generate", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ recipeName, language: lang }),
      });
  
      const data = await response.json();
  
      // Check if response status is ok
      if (!response.ok) {
        console.error("Error generating recipe:", data.error);
        toast.error(data.error || "Error generating recipe");
        return null; // Exit early if there's an error
      }
  
      // Validate the structure of the data returned
      const { recipename, ingredients, instructions, tips } = data;
  
      if (
        !recipename ||
        !Array.isArray(ingredients) ||
        !Array.isArray(instructions) ||
        !Array.isArray(tips)
      ) {
        console.error("Invalid recipe data structure.");
        toast.error("Invalid recipe data structure.");
        return null;
      }
  
      // If all checks pass, return the data
      return { recipename, ingredients, instructions, tips };
    } catch (error) {
      console.error("Error fetching recipe details:", error);
      toast.error("Error fetching recipe details");
      return null;
    }
  };
  
  const addRecipe = async (recipeName: string) => {
    // Set the running state to true at the start of the async operation
    setisrunnig(true)
  
    try {
      const generatedDetails = await generateRecipeDetails(recipeName);
  
      // Only proceed if recipe details are valid
      if (generatedDetails) {
        setRecipes((prev) => [
          ...prev,
          {
            id: Date.now(), // Unique ID for each recipe
            name: generatedDetails.recipename,
            ingredients: generatedDetails.ingredients,
            instructions: generatedDetails.instructions,
            tips: generatedDetails.tips,
            createdAt: new Date(),
          },
        ]);
        toast.success("Successfully added recipe details.");
      } else {
        console.error("Failed to generate recipe details.");
        toast.error("Failed to add recipe details.");
      }
    } catch (error) {
      console.error("Error adding recipe:", error);
      toast.error("An error occurred while adding the recipe.");
    } finally {
      // Ensure that the running state is set back to false after the operation finishes
      setisrunnig(false)
    }
  };
  

  const deleteRecipe = (id: number) => {
    setRecipes(recipes.filter((recipe) => recipe.id !== id));
  };

  const openPopup = (recipe: Recipe) => {
    setSelectedRecipe(recipe); // Set the selected recipe
    setIsModalOpen(true); // Open the modal
  };

  const closeModal = () => {
    setIsModalOpen(false);
    setSelectedRecipe(null); // Clear selected recipe after closing
  };

  return (
    <div className="container mx-auto px-4">
      <RecipeForm addRecipe={addRecipe} isrunnig={isrunnig}/>
      <div className="flex flex-wrap justify-center mt-8">
        {recipes
          .filter((recipe) => {
            // Check if recipe has the required properties in the correct format
            const isValidRecipe =
              recipe &&
              typeof recipe.name === 'string' && // Ensure recipe.name is a string
              Array.isArray(recipe.ingredients) && // Ensure recipe.ingredients is an array
              recipe.ingredients.length > 0; // Ensure there's at least one ingredient

            // Log an error message if the recipe is not valid
            if (!isValidRecipe) {
              toast.error("Data is not in proper format.")
            }

            return isValidRecipe; // Return the validity check
          })
          .map((recipe) => (
            <RecipeCard
              key={recipe.id}
              recipe={recipe}
              onClick={() => openPopup(recipe)}
              onDelete={() => deleteRecipe(recipe.id)}
            />
          ))}
      </div>

      {isModalOpen && selectedRecipe && (
        <Modal recipe={selectedRecipe} onClose={closeModal} />
      )}
    </div>

  );
}
