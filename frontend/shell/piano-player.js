// The app window: a WebKitGTK view showing the UI, with the backend (`pp api`) as a child process.
// It passes JSON-RPC lines between the two; see src/api/webkitBridge.ts for the page side.
import Gdk from "gi://Gdk?version=4.0";
import Gio from "gi://Gio";
import GLib from "gi://GLib";
import Gtk from "gi://Gtk?version=4.0";
import WebKit from "gi://WebKit?version=6.0";
import System from "system";
import { logFile, shouldRestart } from "./backendCare.js";

const here = GLib.path_get_dirname(GLib.filename_from_uri(import.meta.url)[0]);
const bundled = GLib.file_test(`${here}/renderer`, GLib.FileTest.IS_DIR);
const renderer = bundled ? `${here}/renderer` : `${here}/../dist/renderer`;
const backendCommand = GLib.getenv("PP_BACKEND") ?? (bundled ? `${here}/backend/bin/pp` : `${here}/../../bin/pp`);
const SONG_FILES = ["mid", "midi", "musicxml", "xml", "mxl"];
const LOG_LIMIT_BYTES = 1_000_000;
const MEDIA_TYPES = { html: "text/html", js: "text/javascript", css: "text/css", svg: "image/svg+xml", png: "image/png", json: "application/json" };

function call(view, receiver, ...args) {
  const script = `window.piano?.${receiver}(${args.map((arg) => JSON.stringify(arg)).join(",")})`;
  view.evaluate_javascript(script, -1, null, null, null, null);
}

// The UI is served as piano://app/… rather than file://, where WebKit blocks module scripts.
function serveUi(context) {
  const root = Gio.File.new_for_path(renderer);
  context.register_uri_scheme("piano", (request) => {
    const file = root.resolve_relative_path(request.get_path().replace(/^\/+/, "") || "index.html");
    try {
      if (!file.has_prefix(root)) {
        throw new GLib.Error(Gio.IOErrorEnum, Gio.IOErrorEnum.NOT_FOUND, request.get_path());
      }
      const size = file.query_info("standard::size", Gio.FileQueryInfoFlags.NONE, null).get_size();
      const type = MEDIA_TYPES[file.get_basename().split(".").pop()] ?? "application/octet-stream";
      request.finish(file.read(null), size, type);
    } catch (error) {
      request.finish_error(error);
    }
  });
  context.get_security_manager().register_uri_scheme_as_secure("piano");
}

function openLog() {
  const path = logFile((name) => GLib.getenv(name), GLib.get_home_dir());
  GLib.mkdir_with_parents(GLib.path_get_dirname(path), 0o755);
  const file = Gio.File.new_for_path(path);
  if (file.query_exists(null) && file.query_info("standard::size", Gio.FileQueryInfoFlags.NONE, null).get_size() > LOG_LIMIT_BYTES) {
    file.move(Gio.File.new_for_path(`${path}.1`), Gio.FileCopyFlags.OVERWRITE, null, null);
  }
  const stream = file.append_to(Gio.FileCreateFlags.NONE, null);
  return (text) => {
    printerr(text);
    stream.write_all(new TextEncoder().encode(`${new Date().toISOString()} ${text}\n`), null);
    stream.flush(null);
  };
}

function readLines(pipe, onLine) {
  const lines = new Gio.DataInputStream({ base_stream: pipe });
  const next = () =>
    lines.read_line_async(GLib.PRIORITY_DEFAULT, null, (_stream, result) => {
      const [line] = lines.read_line_finish_utf8(result);
      if (line !== null) {
        onLine(line);
        next();
      }
    });
  next();
}

// The backend (`pp api`) as a child process: its stdout lines go to the page, its stderr to the log.
class Backend {
  constructor(view, log, onUnexpectedStop) {
    this.view = view;
    this.log = log;
    this.onUnexpectedStop = onUnexpectedStop;
    this.closing = false;
  }

  start() {
    const process = Gio.Subprocess.new(
      [backendCommand, "api"],
      Gio.SubprocessFlags.STDIN_PIPE | Gio.SubprocessFlags.STDOUT_PIPE | Gio.SubprocessFlags.STDERR_PIPE,
    );
    this.process = process;
    this.stdin = process.get_stdin_pipe();
    readLines(process.get_stdout_pipe(), (line) => call(this.view, "line", line));
    readLines(process.get_stderr_pipe(), this.log);
    process.wait_async(null, (_process, result) => {
      process.wait_finish(result);
      if (process !== this.process || this.closing) {
        return;
      }
      const code = process.get_if_exited() ? process.get_exit_status() : null;
      this.log(`The backend stopped (exit code ${code ?? "unknown"})`);
      this.onUnexpectedStop(code);
    });
  }

  send(line) {
    try {
      this.stdin.write_all(new TextEncoder().encode(line + "\n"), null);
    } catch (error) {
      this.log(`Could not send to the backend: ${error.message}`);
    }
  }

  // Closing stdin is the backend's signal to stop its session, close the MIDI devices and exit.
  close() {
    this.closing = true;
    this.stdin.close(null);
  }
}

function chooseSong(window, view, id) {
  const songs = new Gtk.FileFilter({ name: "MIDI and MusicXML" });
  SONG_FILES.forEach((suffix) => songs.add_suffix(suffix));
  const all = new Gtk.FileFilter({ name: "All files" });
  all.add_pattern("*");
  const filters = new Gio.ListStore({ item_type: Gtk.FileFilter });
  filters.append(songs);
  filters.append(all);
  const dialog = new Gtk.FileDialog({ title: "Import a song", filters, default_filter: songs });
  dialog.open(window, null, (_dialog, result) => {
    let path = null;
    try {
      path = dialog.open_finish(result)?.get_path() ?? null;
    } catch {
      // Cancelled.
    }
    call(view, "chosen", id, path);
  });
}

// PP_E2E names a test script to run in the page; see e2e/runner.ts.
function runTests(content, view, backend) {
  const script = GLib.getenv("PP_E2E");
  if (script === null) {
    return () => false;
  }
  const [, source] = GLib.file_get_contents(script);
  content.add_script(new WebKit.UserScript(new TextDecoder().decode(source), WebKit.UserContentInjectedFrames.TOP_FRAME, WebKit.UserScriptInjectionTime.END, null, null));
  const screens = GLib.getenv("PP_E2E_SCREENS");
  return (message) => {
    if (message.type === "e2e.log") {
      print(message.text);
    } else if (message.type === "e2e.screenshot" && screens !== null) {
      view.get_snapshot(WebKit.SnapshotRegion.VISIBLE, WebKit.SnapshotOptions.NONE, null, (_view, result) =>
        view.get_snapshot_finish(result).save_to_png(`${screens}/${message.name}.png`),
      );
    } else if (message.type === "e2e.done") {
      backend.close();
      System.exit(message.failed ? 1 : 0);
    } else {
      return false;
    }
    return true;
  };
}

function openWindow(app) {
  const content = new WebKit.UserContentManager();
  const view = new WebKit.WebView({ user_content_manager: content });
  serveUi(view.get_context());
  const settings = view.get_settings();
  settings.enable_developer_extras = GLib.getenv("PP_DEVTOOLS") !== null;
  settings.enable_write_console_messages_to_stdout = settings.enable_developer_extras;
  const background = new Gdk.RGBA();
  background.parse("#14161b");
  view.set_background_color(background);

  const window = new Gtk.ApplicationWindow({ application: app, title: "Piano Player", default_width: 1280, default_height: 800 });
  window.set_child(view);

  const screen = GLib.getenv("PP_SCREEN");
  const page = GLib.getenv("VITE_DEV_SERVER_URL") ?? "piano://app/index.html";
  const log = openLog();
  const stops = [];
  const backend = new Backend(view, log, (code) => {
    const now = Date.now();
    stops.push(now);
    if (!shouldRestart(stops, now)) {
      call(view, "exit", code);
      return;
    }
    log("Restarting the backend");
    backend.start();
    const shown = view.get_uri() ?? "";
    view.load_uri(`${page}?restarted=1${shown.includes("#") ? shown.slice(shown.indexOf("#")) : ""}`);
  });
  backend.start();
  const testMessage = runTests(content, view, backend);
  content.register_script_message_handler("shell", null);
  content.connect("script-message-received::shell", (_manager, value) => {
    const message = JSON.parse(value.to_string());
    if (message.type === "line") {
      backend.send(message.line);
    } else if (message.type === "chooseSong") {
      chooseSong(window, view, message.id);
    } else {
      testMessage(message);
    }
  });
  window.connect("close-request", () => {
    backend.close();
    return false;
  });

  view.load_uri(screen ? `${page}#${screen}` : page);
  window.present();
}

const app = new Gtk.Application({ application_id: "bg.petarh.piano.Player", flags: Gio.ApplicationFlags.NON_UNIQUE });
app.connect("activate", openWindow);
app.run([System.programInvocationName, ...ARGV]);
