import React, { useState } from "react";
import "./App.css";
import {
  Tolgee,
  DevTools,
  TolgeeProvider,
  FormatSimple,
  useTranslate,
  T,
  BackendFetch,
} from "@tolgee/react";
import { LanguageSelect } from "./LanguageSelect";

// Initialize Tolgee
const tolgee = Tolgee()
  .use(DevTools())
  .use(
    BackendFetch({
      prefix: "https://cdn.tolg.ee/d8ad83faefdfe437ea9b8a66d6bab6fc",
    })
  )
  .use(FormatSimple())
  .init({
    language: "en",
    apiUrl: import.meta.env.VITE_APP_TOLGEE_API_URL,
    apiKey: import.meta.env.VITE_APP_TOLGEE_API_KEY,
  });

// Define workout data as an array of objects
const workouts = [
  {
    id: 1,
    name: "pushups",
    description: "Push-ups: 3 sets of 10 reps",
    image: "/pushups.jpg",
  },
  {
    id: 2,
    name: "squats",
    description: "Squats: 3 sets of 10 reps",
    image: "/squats.jpg",
  },
  {
    id: 3,
    name: "lunges",
    description: "Lunges: 3 sets of 10 reps",
    image: "/lunges.jpg",
  },
  {
    id: 4,
    name: "planks",
    description: "Planks: 3 sets of 30 seconds",
    image: "/plank.jpg",
  },
  {
    id: 5,
    name: "burpees",
    description: "Burpees: 3 sets of 10 reps",
    image: "/burpees.jpg",
  },
  {
    id: 6,
    name: "crunches",
    description: "Crunches: 3 sets of 30 seconds",
    image: "/crunches.jpg",
  },
];

function WorkoutComponent() {
  const [workout, setWorkout] = useState({ num: 0, name: "", image: "" });
  const { t } = useTranslate();

  // Function to generate a random workout
  const generateWorkout = () => {
    const randomNum = Math.floor(Math.random() * workouts.length);
    const selectedWorkout = workouts[randomNum];

    setWorkout({
      num: randomNum + 1,
      name: t(selectedWorkout.name, selectedWorkout.description),
      image: selectedWorkout.image,
    });
  };

  return (
    <div className="app-container">
      {/* LanguageSelect is placed here, above the workout content */}
      <LanguageSelect />
      <div className="content">
        <h1 className="title">
          <T>Welcome to Dice Workout</T>
        </h1>
        <button
          onClick={generateWorkout}
          className="dice-button"
          style={{ backgroundColor: "white" }}
        >
          <img src="/dice.gif" alt="dice" className="dice-image" />
        </button>
        <h3 className="subtitle">
          <T>Click Here to know</T>
        </h3>
        {workout.num !== 0 && (
          <div className="result">
            <T>You Got a </T>
            <span className="number">{workout.num}</span>
          </div>
        )}
        {workout.num !== 0 && (
          <>
            <div className="workout">
              <T>The Workout is</T>
              <span className="workout-name">{workout.name}</span>
            </div>
            <div className="workout-image-container">
              <img
                src={workout.image}
                alt="workout"
                className="workout-image"
              />
            </div>
          </>
        )}
      </div>
    </div>
  );
}

function App() {
  return (
    <TolgeeProvider tolgee={tolgee} fallback="Loading...">
      <WorkoutComponent />
    </TolgeeProvider>
  );
}

export default App;
