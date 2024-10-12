import React, { useState, useEffect } from "react";
import { motion } from "framer-motion";
import dishesData from "../../public/dishes.json";
import { T, useTranslate } from "@tolgee/react"; 

interface Dish {
  name: string;
  description: string;
  country: string;
  region: string;
  image: string;
  tags: string[];
  mainIngredients: string[];
}

const DishCard: React.FC = () => {
  const [dishes, setDishes] = useState<Dish[]>([]);
  const [filteredDishes, setFilteredDishes] = useState<Dish[]>([]);
  const [currentPage, setCurrentPage] = useState(1);
  const [dishesPerPage] = useState(6);
  const [selectedDish, setSelectedDish] = useState<string | null>(null);
  const [filterTag, setFilterTag] = useState("all");
  const [searchTerm, setSearchTerm] = useState("");
  const { t } = useTranslate();

  useEffect(() => {
    setDishes(dishesData.dishes);
    setFilteredDishes(dishesData.dishes);
  }, []);

  const indexOfLastDish = currentPage * dishesPerPage;
  const indexOfFirstDish = indexOfLastDish - dishesPerPage;
  const currentDishes = filteredDishes.slice(indexOfFirstDish, indexOfLastDish);

  const paginate = (pageNumber: number) => setCurrentPage(pageNumber);

  const handleFilterChange = (tag: string) => {
    setFilterTag(tag);
    setCurrentPage(1);
    if (tag === "all") {
      setFilteredDishes(dishes);
    } else {
      setFilteredDishes(dishes.filter((dish) => dish.tags.includes(tag)));
    }
  };

  const handleSearch = (event: React.ChangeEvent<HTMLInputElement>) => {
    const term = event.target.value;
    setSearchTerm(term);
    setCurrentPage(1);
    if (term === "") {
      setFilteredDishes(dishes);
    } else {
      const searchResults = dishes.filter((dish) =>
        dish.name.toLowerCase().includes(term.toLowerCase())
      );
      setFilteredDishes(searchResults);
    }
  };

  return (
    <div className="container mx-auto p-6">
      {/* Search and Filter options */}
      <div className="flex flex-col md:flex-row justify-between mb-6 space-y-4 md:space-y-0">
        <input
          type="text"
          placeholder={t("search_placeholder", "Search for a dish...")}
          className="p-3 border rounded-md w-full md:w-1/3"
          value={searchTerm}
          onChange={handleSearch}
        />
        <div className="flex space-x-2">
          <button
            className={`p-2 rounded-md ${
              filterTag === "all" ? "bg-blue-500 text-white" : "bg-gray-200"
            }`}
            onClick={() => handleFilterChange("all")}
            aria-label={t("filter_all", "Show all dishes")}
          >
            <T keyName="all" defaultValue="All" />
          </button>
          <button
            className={`p-2 rounded-md ${
              filterTag === "veg" ? "bg-green-500 text-white" : "bg-gray-200"
            }`}
            onClick={() => handleFilterChange("veg")}
            aria-label={t("filter_veg", "Show vegetarian dishes")}
          >
            <T keyName="vegetarian" defaultValue="Vegetarian" />
          </button>
          <button
            className={`p-2 rounded-md ${
              filterTag === "nonveg" ? "bg-red-500 text-white" : "bg-gray-200"
            }`}
            onClick={() => handleFilterChange("nonveg")}
            aria-label={t("filter_nonveg", "Show non-vegetarian dishes")}
          >
            <T keyName="non_vegetarian" defaultValue="Non-Vegetarian" />
          </button>
        </div>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-6">
        {currentDishes.map((dish, index) => (
          <motion.div
            key={index}
            className="bg-white rounded-lg shadow-lg overflow-hidden transform hover:scale-105 transition-all duration-300"
            onClick={() =>
              setSelectedDish(selectedDish === dish.name ? null : dish.name)
            }
            layout
            whileHover={{ scale: 1.05 }}
          >
            <img
              src={dish.image}
              alt={dish.name}
              className="w-full h-48 object-cover"
            />
            <div className="p-4">
              <h2 className="text-2xl font-bold text-blue-600">{dish.name}</h2>
              <p className="text-sm text-gray-500">
                {dish.region}, {dish.country}
              </p>
              <p
                className={`text-xs font-semibold mt-2 ${
                  dish.tags.includes("veg") ? "text-green-500" : "text-red-500"
                }`}
              >
                {dish.tags.includes("veg")
                  ? t("vegetarian", "Vegetarian")
                  : t("non_vegetarian", "Non-Vegetarian")}
              </p>
            </div>
            {selectedDish === dish.name && (
              <motion.div
                initial={{ opacity: 0, height: 0 }}
                animate={{ opacity: 1, height: "auto" }}
                className="p-4 bg-gray-100"
              >
                <p>{dish.description}</p>
                <p className="text-sm font-semibold mt-2">
                  <T
                    keyName="main_ingredients"
                    defaultValue="Main Ingredients:"
                  />
                </p>
                <ul className="list-disc ml-6">
                  {dish.mainIngredients.map((ingredient, idx) => (
                    <li key={idx}>{ingredient}</li>
                  ))}
                </ul>
              </motion.div>
            )}
          </motion.div>
        ))}
      </div>

      <div className="flex justify-center mt-6 space-x-2">
        {Array.from(
          { length: Math.ceil(filteredDishes.length / dishesPerPage) },
          (_, index) => (
            <button
              key={index}
              onClick={() => paginate(index + 1)}
              className={`px-4 py-2 rounded-md ${
                currentPage === index + 1
                  ? "bg-blue-500 text-white"
                  : "bg-gray-200"
              }`}
            >
              {index + 1}
            </button>
          )
        )}
      </div>
    </div>
  );
};

export default DishCard;
