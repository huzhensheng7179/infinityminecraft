const fs = require('fs');
const path = require('path');
const dir = 'src/main/resources/assets/infinitycraft/textures/steampunk';
const files = fs.readdirSync(dir);
for (const f of files) {
  const full = path.join(dir, f);
  if (f.endsWith('.mcmeta')) {
    console.log('--- ' + f + ' ---');
    console.log(JSON.stringify(fs.readFileSync(full, 'utf8')));
    continue;
  }
  if (f.endsWith('.png')) {
    const buf = fs.readFileSync(full);
    const sig = buf.slice(0, 8).toString('hex');
    const isPng = sig === '89504e470d0a1a0a';
    // IHDR: width at byte16-19, height 20-23 (big endian)
    let w = -1, h = -1, bitDepth = -1, colorType = -1;
    if (isPng && buf.slice(12, 16).toString('ascii') === 'IHDR') {
      w = buf.readUInt32BE(16);
      h = buf.readUInt32BE(20);
      bitDepth = buf[24];
      colorType = buf[25];
    }
    console.log(f + '  png=' + isPng + '  ' + w + 'x' + h + '  bitDepth=' + bitDepth + ' colorType=' + colorType + '  bytes=' + buf.length);
  }
}
