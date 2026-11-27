import { createContext, useContext } from "react";
import type { RpcClient } from "../api/rpcClient";

export const BackendContext = createContext<RpcClient | undefined>(undefined);

export function useBackend(): RpcClient {
  const client = useContext(BackendContext);
  if (client === undefined) {
    throw new Error("useBackend needs a BackendContext provider");
  }
  return client;
}
