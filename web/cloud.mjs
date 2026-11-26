import {initializeApp} from './vendor/app.js';
import {getAuth,GoogleAuthProvider,signInWithPopup,onAuthStateChanged,signOut} from './vendor/auth.js';
import {getFirestore,doc,collection,onSnapshot,runTransaction,serverTimestamp,deleteField} from './vendor/firestore.js';
import {firebaseConfig,databaseId} from './firebase-config.mjs';
import {cycleStart,todayIndia,validDate} from './payroll.mjs';
const firebase=initializeApp(firebaseConfig), auth=getAuth(firebase), db=getFirestore(firebase,databaseId);
const provider=new GoogleAuthProvider(); provider.setCustomParameters({prompt:'select_account'});
export const signIn=()=>signInWithPopup(auth,provider);
export const logOut=()=>signOut(auth);
function profileData(uid,start) {return {userId:uid,timezone:'Asia/Kolkata',schemaVersion:1,trackingStartCycle:start,createdAt:serverTimestamp(),updatedAt:serverTimestamp()}}
export function connect(notify) {
 let clean=[],epoch=0;
 const off=onAuthStateChanged(auth,user=>{
  const turn=++epoch; clean.forEach(f=>f());clean=[];
  if(!user){notify({user:null,records:{},ready:false,mode:'preview',tracking:cycleStart(todayIndia()),error:''});return}
  const uid=user.uid,ref=doc(db,'users',uid);
  let tracking=cycleStart(todayIndia()), records={},readyProfile=false,readyEntries=false,profileCache=true,entriesCache=true,err='';
  const send=()=>{if(turn===epoch)notify({user,records,tracking,error:err,ready:readyProfile&&readyEntries,mode:err?'error':!readyProfile||!readyEntries?'loading':profileCache||entriesCache?'cached':'connected'})};
  const failure=e=>{if(turn!==epoch)return;err=e.code||'sync-error';send()};
  send();
  clean.push(onSnapshot(ref,{includeMetadataChanges:true},async snap=>{
   if(turn!==epoch)return;
   if(!snap.exists()&&!snap.metadata.fromCache){
    try {await runTransaction(db,async tx=>{const p=await tx.get(ref);if(!p.exists())tx.set(ref,profileData(uid,tracking))})}catch(e){failure(e)} return;
   }
   if(snap.exists()&&validDate(snap.get('trackingStartCycle')))tracking=cycleStart(snap.get('trackingStartCycle'));
   profileCache=snap.metadata.fromCache;readyProfile=snap.exists();send();
  },failure));
  clean.push(onSnapshot(collection(ref,'dayEntries'),{includeMetadataChanges:true},snap=>{
   if(turn!==epoch)return;
   records=Object.fromEntries(snap.docs.filter(d=>validDate(d.id)).map(d=>[d.id,d.data()]));
   entriesCache=snap.metadata.fromCache;readyEntries=true;send();
  },failure));
 });
 return ()=>{++epoch;off();clean.forEach(f=>f())};
}
function owner(uid){if(!auth.currentUser||auth.currentUser.uid!==uid)throw Error('Account changed. Reopen the editor.');return doc(db,'users',uid)}
function revision(snap){const t=snap?.updatedAt;return t?t.seconds+'.'+t.nanoseconds:''}
export async function saveDay(uid,date,entry,original) {
 const profile=owner(uid),ref=doc(profile,'dayEntries',date);
 await runTransaction(db,async tx=>{
  const p=await tx.get(profile),d=await tx.get(ref);
  if(d.exists()!==Boolean(original)||revision(d.data())!==revision(original))throw Error('This day changed on another device. Close and reopen the editor to see the latest entry.');
  if(!entry){if(d.exists())tx.delete(ref);return}
  const start=cycleStart(date);
  if(!p.exists())tx.set(profile,profileData(uid,[start,cycleStart(todayIndia())].sort()[0]));
  else if(p.get('trackingStartCycle')>start)tx.update(profile,{trackingStartCycle:start,updatedAt:serverTimestamp()});
  const data={dayType:entry.dayType,workStatus:entry.workStatus,overtimeEnabled:entry.overtimeEnabled,updatedAt:serverTimestamp()};
  for(const key of ['exitTime','note']){if(entry[key])data[key]=entry[key];else if(d.exists()&&Object.hasOwn(d.data(),key))data[key]=deleteField()}
  if(d.exists())tx.update(ref,data);else tx.set(ref,{...data,userId:uid,date,createdAt:serverTimestamp()});
 });
}
export async function setTracking(uid,start){
 const ref=owner(uid);await runTransaction(db,async tx=>{const snap=await tx.get(ref);if(snap.exists())tx.update(ref,{trackingStartCycle:start,updatedAt:serverTimestamp()});else tx.set(ref,profileData(uid,start))});
}

