import path from "node:path";

export interface Launch {
  env: Record<string, string | undefined>;
  isPackaged: boolean;
  resourcesPath: string;
  appPath: string;
}

export function backendCommand({ env, isPackaged, resourcesPath, appPath }: Launch): string {
  if (env.PP_BACKEND) {
    return env.PP_BACKEND;
  }
  return isPackaged ? path.join(resourcesPath, "backend", "bin", "pp") : path.resolve(appPath, "..", "bin", "pp");
}
