import { useState } from "react";
import "./App.css";
import { useTranslate } from '@tolgee/react';
import Header from "./components/Header";
import GameScreen from "./components/GameScreen";
import NavBar from "./components/NavBar";

function App() {
  const { t } = useTranslate();
  const [gameScreen, setGameScreen] = useState(false);
  return (
    <>
      <NavBar setGameScreen={setGameScreen}/>
      <main className="main flex items-center justify-center">
        {gameScreen ? <GameScreen /> : <Header setGameScreen={setGameScreen}/>}
      </main>
    </>
  );
}

export default App;
