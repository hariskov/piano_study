import { describe, expect, it } from "vitest";
import { backendCommand } from "./backendCommand.cts";

describe("backendCommand", () => {
  it("uses the runtime bundled in the package", () => {
    expect(
      backendCommand({ env: {}, isPackaged: true, resourcesPath: "/tmp/.mount_pp/resources", appPath: "/tmp/.mount_pp/resources/app.asar" }),
    ).toBe("/tmp/.mount_pp/resources/backend/bin/pp");
  });

  it("uses bin/pp of the checkout when run from source", () => {
    expect(backendCommand({ env: {}, isPackaged: false, resourcesPath: "/x", appPath: "/home/me/piano/frontend" })).toBe(
      "/home/me/piano/bin/pp",
    );
  });

  it("lets PP_BACKEND override both", () => {
    expect(backendCommand({ env: { PP_BACKEND: "/opt/pp" }, isPackaged: true, resourcesPath: "/r", appPath: "/a" })).toBe(
      "/opt/pp",
    );
  });
});
