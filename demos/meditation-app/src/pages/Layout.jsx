import React from "react";
import { Outlet } from "react-router-dom";
import Header from "../components/Header";

function Layout() {
  return (
    <div className="bg-primaryBackground relative overflow-auto">
      <Header />
      <main className="pt-[72px] h-screen overflow-auto">
        <Outlet />
      </main>
    </div>
  );
}

export default Layout;
