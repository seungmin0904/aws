import "@toast-ui/editor/dist/toastui-editor.css";
import "@/styles/login.css";
import "./index.css";
import React from "react";
import { createRoot } from "react-dom/client";  // ← 여기!
import App from "./App.jsx";

const container = document.getElementById("root");
const root = createRoot(container);
root.render(
  <>
  {/* <React.StrictMode> */}
    <App />
  {/* </React.StrictMode> */}
</>
);