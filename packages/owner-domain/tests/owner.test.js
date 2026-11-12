import test from 'node:test';
import assert from 'node:assert/strict';
import { petPayload, profilePayload, availableFutureSlots, upcomingAppointments, safeExternalUrl } from '../src/model.js';
import { createOwnerRepository } from '../src/api.js';

test('pet registration validates species, real dates and required names without inventing identity fields',()=>{
  assert.throws(()=>petPayload({name:' ',species:'DOG'}),/name/);
  assert.throws(()=>petPayload({name:'Coco',species:'UNICORN'}),/species/);
  assert.throws(()=>petPayload({name:'Coco',species:'DOG',dob:'2024-02-30'}),/calendar/);
  const value=petPayload({name:' Coco ',species:'CAT',gender:'FEMALE',dob:'2020-02-29',breed:'Domestic'});
  assert.equal(value.name,'Coco');assert.equal(value.dob,'2020-02-29');assert.equal(value.registrationNo,null);assert.equal(value.photoUrl,null);
});
test('only the selected hospital’s available, future slots can be offered',()=>{
  const now=new Date('2026-11-12T04:30:00Z').getTime();
  const base={slotDate:'2026-11-12',startTime:'11:00:00',hospitalId:'hospital-a',status:'AVAILABLE',capacity:1,bookedCount:0};
  const slots=[{...base,slotId:'future'},{...base,slotId:'past',startTime:'09:00:00'},{...base,slotId:'full',bookedCount:1},{...base,slotId:'blocked',status:'BLOCKED'},{...base,slotId:'other',hospitalId:'hospital-b'}];
  assert.deepEqual(availableFutureSlots(slots,'hospital-a',now).map(s=>s.slotId),['future']);
});
test('upcoming appointments exclude completed and cancelled records and sort by time',()=>{
  const list=[{appointmentId:'late',status:'BOOKED',appointmentAt:'2026-11-14T00:00:00Z'},{appointmentId:'cancelled',status:'CANCELLED',appointmentAt:'2026-11-13T00:00:00Z'},{appointmentId:'early',status:'CONFIRMED',appointmentAt:'2026-11-13T00:00:00Z'}];
  assert.deepEqual(upcomingAppointments(list,new Date('2026-11-12').getTime()).map(a=>a.appointmentId),['early','late']);
});
test('profile validation preserves actual caregiver input and rejects invalid pincodes',()=>{
  assert.throws(()=>profilePayload({firstName:'A',lastName:'B',pincode:'123'}),/six/);
  assert.throws(()=>profilePayload({firstName:'A',lastName:''}),/required/);
  assert.equal(profilePayload({firstName:' A ',lastName:' B ',pincode:'560001'}).firstName,'A');
});
test('document links reject unsafe schemes and credential-bearing URLs',()=>{
  for(const value of ['javascript:alert(1)','file:///private/record','https://user:password@example.test/file','/private/record'])assert.equal(safeExternalUrl(value),null);
  assert.equal(safeExternalUrl('https://storage.example.test/record.pdf'),'https://storage.example.test/record.pdf');
});
test('repository reads every API page and uses object bodies for the shared transport',async()=>{
  const calls=[];const repo=createOwnerRepository(async(path,options)=>{calls.push({path,options});if(path.startsWith('/owner/pets?'))return {content:[{petId:path.includes('page=0')?'one':'two'}],totalPages:2};return {appointmentId:'confirmed'};});
  assert.deepEqual((await repo.pets()).map(p=>p.petId),['one','two']);
  await repo.book({petId:'one',slotId:'slot'});assert.deepEqual(calls.at(-1),{path:'/v1/slots/book',options:{method:'POST',body:{petId:'one',slotId:'slot'}}});
  await repo.share('one',{targetHospitalId:'hospital',scope:'VACCINES_ONLY',expiryHours:24});assert.equal(calls.at(-1).options.body.petId,'one');
  await repo.cancel('appointment/id','Changed plans');assert.equal(calls.at(-1).path,'/owner/appointments/appointment%2Fid/cancel');
});

test('legacy species labels normalize for editing without changing the pet identity',async()=>{
  const repository=createOwnerRepository(async()=>({content:[{petId:'existing',name:'Leo',species:'Feline'}],totalPages:1}));
  const [pet]=await repository.pets();assert.equal(pet.species,'CAT');assert.equal(pet.petId,'existing');assert.equal(petPayload(pet).species,'CAT');
});
