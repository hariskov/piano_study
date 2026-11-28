import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { installWebKitBridge } from "./api/webkitBridge";
import { App } from "./app/App";
import "./styles.css";

installWebKitBridge();

createRoot(document.getElementById("root")!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
