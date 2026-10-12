const fs = require('fs');
const path = require('path');
const root = 'C:\\Users\\wdsjx\\Desktop\\infinityminecraft-template-1.21.1\\' +
  String.fromCharCode(0x8D44,0x6E90,0x6587,0x4EF6) + '\\' +
  String.fromCharCode(0x6A21,0x578B,0x8D44,0x6E90);
// find wildfire folder
const folders = fs.readdirSync(root, {withFileTypes:true}).filter(d=>d.isDirectory());
const wf = path.join(root, folders[0].name);
const modelPath = path.join(wf, 'steampunk_bow_0.json');
const m = JSON.parse(fs.readFileSync(modelPath,'utf8'));
console.log('TOP KEYS:', Object.keys(m).join(', '));
console.log('format_version:', m['format_version']);
console.log('texture_size:', JSON.stringify(m.texture_size));
console.log('has display:', !!m.display, m.display?JSON.stringify(Object.keys(m.display)):'');
console.log('elements count:', m.elements?m.elements.length:('none, groups='+ (m.groups?m.groups.length:0)));
console.log('textures map:', JSON.stringify(m.textures, null, 1));
// collect all face texture references
const faceRefs = new Set();
function scanEls(els){ (els||[]).forEach(e=>{ if(e.faces) Object.values(e.faces).forEach(f=>faceRefs.add(f.texture)); }); }
scanEls(m.elements);
if(m.groups) m.groups.forEach(g=>scanEls(g.elements));
console.log('distinct face texture refs:', Array.from(faceRefs).sort().join(', '));
