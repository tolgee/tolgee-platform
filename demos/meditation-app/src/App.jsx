import { createBrowserRouter, RouterProvider } from "react-router-dom";
import Home from "./pages/Home";
import Meditation from "./pages/Meditation";
import SoundSelect from "./pages/SoundSelect";
import DurationSelect from "./pages/DurationSelect";
import Layout from "./pages/Layout";
import PageNotFound from "./pages/PageNotFound";

function App() {
  const router = createBrowserRouter([
    {
      path: "/",
      element: <Layout />,
      children: [
        { index: true, element: <Home /> },
        {
          path: "/meditation/sound-select",
          element: <SoundSelect />,
        },
        {
          path: "/meditation/duration-select",
          element: <DurationSelect />,
        },
      ],
    },
    {
      path: "/meditation/meditate",
      element: <Meditation />,
    },
    { path: "*", element: <PageNotFound /> },
  ]);

  return <RouterProvider router={router} />;
}

export default App;
