import React from "react";
import { Facebook, Twitter, Instagram, GitHub } from "react-feather"; // Social media icons

const Footer: React.FC = () => {
  return (
    <footer className="bg-gradient-to-r from-blue-500 to-purple-600 text-white py-8">
      <div className="container mx-auto flex flex-col md:flex-row justify-between items-center px-4">
        {/* Logo */}
        <div className="flex items-center space-x-2 mb-4 md:mb-0">
          <img
            src="https://img.icons8.com/clouds/100/000000/food.png"
            alt="Dish Logo"
            className="w-12 h-12"
          />
          <span className="text-2xl font-bold">DishHub</span>
        </div>

        {/* Links */}
        <div className="flex flex-col md:flex-row space-y-2 md:space-y-0 md:space-x-8">
          <a
            href="/home"
            className="hover:text-yellow-300 transition duration-300"
          >
            Home
          </a>
          <a
            href="/dishes"
            className="hover:text-yellow-300 transition duration-300"
          >
            Dishes
          </a>
          <a
            href="/about"
            className="hover:text-yellow-300 transition duration-300"
          >
            About
          </a>
          <a
            href="/contact"
            className="hover:text-yellow-300 transition duration-300"
          >
            Contact
          </a>
        </div>

        {/* Social Media Icons */}
        <div className="flex space-x-4 mt-4 md:mt-0">
          <a
            href="https://facebook.com"
            target="_blank"
            rel="noopener noreferrer"
          >
            <Facebook className="hover:text-yellow-300 transition duration-300" />
          </a>
          <a
            href="https://twitter.com"
            target="_blank"
            rel="noopener noreferrer"
          >
            <Twitter className="hover:text-yellow-300 transition duration-300" />
          </a>
          <a
            href="https://instagram.com"
            target="_blank"
            rel="noopener noreferrer"
          >
            <Instagram className="hover:text-yellow-300 transition duration-300" />
          </a>
          <a
            href="https://github.com"
            target="_blank"
            rel="noopener noreferrer"
          >
            <GitHub className="hover:text-yellow-300 transition duration-300" />
          </a>
        </div>
      </div>

      {/* Copyright */}
      <div className="mt-4 text-center text-sm">
        <p>© {new Date().getFullYear()} DishHub. All rights reserved.</p>
      </div>
    </footer>
  );
};

export default Footer;
