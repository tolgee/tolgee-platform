import React from "react";

function ErrorMessage({error}) {
  return (
    <div className="text-red-400 text-lg text-center font-semibold font-primary">
      {error}
    </div>
  );
}

export default ErrorMessage;
