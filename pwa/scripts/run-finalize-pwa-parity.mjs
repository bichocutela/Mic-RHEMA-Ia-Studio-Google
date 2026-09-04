import fs from "node:fs";
import path from "node:path";
import { pathToFileURL } from "node:url";

const sourcePath=path.resolve(process.cwd(),"scripts/finalize-pwa-parity.mjs");
const runtimePath=path.resolve(process.cwd(),"scripts/.finalize-pwa-parity-runtime.mjs");
let source=fs.readFileSync(sourcePath,"utf8");
source=source.replace(
  String.raw`/function BannerForm\([\s\S]*?\n\nfunction DonationsAdmin/`,
  String.raw`/function BannerForm\([\s\S]*?function DonationsAdmin/`,
);
fs.writeFileSync(runtimePath,source);
try {
  await import(`${pathToFileURL(runtimePath).href}?v=${Date.now()}`);
} finally {
  fs.rmSync(runtimePath,{force:true});
}
