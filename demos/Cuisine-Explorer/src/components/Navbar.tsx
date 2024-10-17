import React, { useState } from "react";
import { motion } from "framer-motion";
import { Menu, X } from "react-feather"; 
import { Link } from "react-router-dom"; 

const Navbar: React.FC = () => {
  const [isOpen, setIsOpen] = useState(false);

  const toggleMenu = () => {
    setIsOpen(!isOpen);
  };

  return (
    <nav className="bg-gradient-to-r from-blue-500 to-purple-600 shadow-lg">
      <div className="container mx-auto p-4 flex justify-between items-center">
        {/* Logo */}
        <div className="flex items-center space-x-2">
          <img
            src="https://img.icons8.com/clouds/100/000000/food.png"
            alt="Dish Logo"
            className="w-12 h-12"
          />
          <a href="/" className="text-white text-2xl font-bold">
            DishHub
          </a>
        </div>

        <div className="hidden md:flex space-x-6">
          <a
            href="/home"
            className="text-white text-lg hover:text-yellow-300 transition duration-300"
          >
            Home
          </a>
          <a
            href="/dishes"
            className="text-white text-lg hover:text-yellow-300 transition duration-300"
          >
            Dishes
          </a>
          <a
            href="/about"
            className="text-white text-lg hover:text-yellow-300 transition duration-300"
          >
            About
          </a>
          <a
            href="/contact"
            className="text-white text-lg hover:text-yellow-300 transition duration-300"
          >
            Contact
          </a>
        </div>

        <motion.button
          whileHover={{ scale: 1.1 }}
          whileTap={{ scale: 0.9 }}
          className="hidden md:inline-block bg-yellow-400 text-gray-800 px-4 py-2 rounded-full shadow-lg transition duration-300"
        >
          Get Started
        </motion.button>
        <div className="md:hidden cursor-pointer" onClick={toggleMenu}>
          {isOpen ? (
            <X className="text-white" />
          ) : (
            <Menu className="text-white" />
          )}
        </div>
      </div>
      {isOpen && (
        <motion.div
          initial={{ opacity: 0, height: 0 }}
          animate={{ opacity: 1, height: "auto" }}
          transition={{ duration: 0.3 }}
          className="md:hidden bg-blue-700"
        >
          <div className="flex flex-col space-y-4 p-4">
            <a
              href="/home"
              className="text-white text-lg hover:text-yellow-300 transition duration-300"
              onClick={toggleMenu}
            >
              Home
            </a>
            <a
              href="/dishes"
              className="text-white text-lg hover:text-yellow-300 transition duration-300"
              onClick={toggleMenu}
            >
              Dishes
            </a>
            <a
              href="/about"
              className="text-white text-lg hover:text-yellow-300 transition duration-300"
              onClick={toggleMenu}
            >
              About
            </a>
            <a
              href="/contact"
              className="text-white text-lg hover:text-yellow-300 transition duration-300"
              onClick={toggleMenu}
            >
              Contact
            </a>
            <motion.button
              whileHover={{ scale: 1.1 }}
              whileTap={{ scale: 0.9 }}
              className="bg-yellow-400 text-gray-800 px-4 py-2 rounded-full shadow-lg transition duration-300"
              onClick={toggleMenu}
            >
              Get Started
            </motion.button>
          </div>
        </motion.div>
      )}
    </nav>
  );
};

export default Navbar;
