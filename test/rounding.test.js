import test from 'node:test';
import assert from 'node:assert/strict';
import {allocate} from '../api/_rounding.js';
test('one paisa goes to largest fractional remainder',()=>{const r=allocate('100.00',[{id:'A',share:'33333'},{id:'B',share:'33333'},{id:'C',share:'33334'}]);assert.deepEqual(r.lines.map(x=>x.finalAmount),['33.33','33.33','33.34']);assert.equal(r.residualPaise,1);assert.equal(r.reconciledTotal,'100.00');});
test('ties use stable ascending recipient ID',()=>{const data=[{id:'B',share:'1'},{id:'A',share:'1'},{id:'C',share:'1'}],a=allocate('0.01',data),b=allocate('0.01',data);assert.deepEqual(a,b);assert.equal(a.lines.find(x=>x.id==='A').finalAmount,'0.01');});
test('multiple residual paise are allocated deterministically',()=>{const r=allocate('0.05',[{id:'A',share:'1'},{id:'B',share:'1'},{id:'C',share:'1'}]);assert.equal(r.residualPaise,2);assert.deepEqual(r.lines.map(x=>x.finalAmount),['0.02','0.02','0.01']);});
