import { useEffect, useState } from "react";

export default function useScroll(selector) {
  const [scroll, setScroll] = useState({ scrollX: 0, scrollY: 0 });
  useEffect(() => {
    function handleScroll(e) {
      setScroll({ scrollX: e.target.scrollLeft, scrollY: e.target.scrollTop });
    }
    document.querySelector(selector).addEventListener("scroll", handleScroll);
    return () => {
      document
        .querySelector(selector)
        .removeEventListener("scroll", handleScroll);
    };
  }, []);
  return scroll;
}
