const fs = require('fs');
const path = require('path');

function pngSize(full) {
  const buf = fs.readFileSync(full);
  const isPng = buf.slice(0, 8).toString('hex') === '89504e470d0a1a0a';
  let w = -1, h = -1;
  if (isPng && buf.slice(12, 16).toString('ascii') === 'IHDR') { w = buf.readUInt32BE(16); h = buf.readUInt32BE(20); }
  return { isPng, w, h, bytes: buf.length };
}

function dumpTitle(t) { console.log('\n===== ' + t + ' ====='); }

// source package textures
dumpTitle('SOURCE 资源文件/模型资源/武器模型/assets/minecraft/textures/steampunk');
const srcDir = '资源文件/模型资源/武器模型/assets/minecraft/textures/steampunk';
for (const f of fs.readdirSync(srcDir)) {
  const full = path.join(srcDir, f);
  if (f.endsWith('.png')) { const s = pngSize(full); console.log(f + '  ' + s.w + 'x' + s.h + '  png=' + s.isPng + '  ' + s.bytes + 'B'); }
  else if (f.endsWith('.mcmeta')) { console.log(f + '  mcmeta=' + fs.readFileSync(full,'utf8').replace(/\s+/g,' ').trim()); }
}

// deployed textures
dumpTitle('DEPLOYED src/main/resources/assets/infinitycraft/textures/steampunk');
const depDir = 'src/main/resources/assets/infinitycraft/textures/steampunk';
if (fs.existsSync(depDir)) {
  for (const f of fs.readdirSync(depDir)) {
    const full = path.join(depDir, f);
    if (f.endsWith('.png')) { const s = pngSize(full); console.log(f + '  ' + s.w + 'x' + s.h + '  png=' + s.isPng + '  ' + s.bytes + 'B'); }
    else if (f.endsWith('.mcmeta')) { console.log(f + '  mcmeta=' + fs.readFileSync(full,'utf8').replace(/\s+/g,' ').trim()); }
  }
} else console.log('(missing)');

// deployed model json
dumpTitle('DEPLOYED models/item/wildfire_bow.json (head)');
const m = 'src/main/resources/assets/infinitycraft/models/item/wildfire_bow.json';
if (fs.existsSync(m)) {
  const txt = fs.readFileSync(m, 'utf8');
  console.log(txt.substring(0, 800));
  console.log('... total length=' + txt.length);
} else console.log('(missing)');
