const oneDay = 86400000;
export function iso(value) { return value.toISOString().slice(0,10) }
export function utc(key) { return new Date(key+'T00:00:00Z') }
export function shift(key, days) { return iso(new Date(utc(key).getTime()+oneDay*days)) }
export function monthDate(key,offset,day) { const d=utc(key); return iso(new Date(Date.UTC(d.getUTCFullYear(),d.getUTCMonth()+offset,day))) }
export function todayIndia(now=new Date()) { return iso(new Date(now.getTime()+19800000)) }
export function cycleStart(key) { return monthDate(key,Number(key.slice(-2))<24?-1:0,24) }
export function validDate(key) { return typeof key==='string' && /^\d{4}-\d\d-\d\d$/.test(key) && !Number.isNaN(utc(key).getTime()) && iso(utc(key))===key }
export const statusNames={FULL:'Full day',HALF:'Half day',LEAVE:'Leave',PAID_OFF:'Paid off'};
export const typeNames={OFFICE_DAY:'Office day',SUNDAY:'Sunday',HOLIDAY:'Company holiday'};
export function statuses(type) { return type==='OFFICE_DAY'?['FULL','HALF','LEAVE']:['PAID_OFF','HALF','FULL'] }
export function dayPay(date,record={}) {
 const defaultType=utc(date).getUTCDay()===0?'SUNDAY':'OFFICE_DAY';
 const dayType=Object.hasOwn(typeNames,record.dayType)?record.dayType:defaultType;
 const options=statuses(dayType);
 const workStatus=options.includes(record.workStatus)?record.workStatus:options[0];
 const exitTime=/^([01]\d|2[0-3]):[0-5]\d$/.test(record.exitTime||'')?record.exitTime:'';
 const worked=workStatus==='FULL'||workStatus==='HALF'; const overtimeEnabled=worked&&record.overtimeEnabled===true;
 const [h,m]=exitTime.split(':').map(Number);
 const blocks=overtimeEnabled&&exitTime?Math.max(0,Math.ceil((60*h+m-1110)/30)):0;
 // All pay components are stored in eighteenths of a rupee until display.
 const regular=18*(dayType==='OFFICE_DAY'?(workStatus==='FULL'?500:workStatus==='HALF'?250:0):500);
 const holiday=18*(dayType!=='OFFICE_DAY'&&worked?(workStatus==='FULL'?500:250):0);
 const overtime=blocks*500;
 return {date,dayType,workStatus,exitTime,overtimeEnabled,blocks,regular,holiday,overtime,total:regular+holiday+overtime,note:typeof record.note==='string'?record.note:''};
}
export function earliest(tracking,records,today) { return Object.keys(records).filter(validDate).reduce((min,k)=>[cycleStart(k),min].sort()[0],[cycleStart(today),tracking].sort()[0]); }
export function summary(start,end,today,records,tracking=start) {
 const out={start,end,days:[],elapsed:0,remaining:0,regular:0,holiday:0,overtime:0,blocks:0,total:0,forecast:0};
 for(let date=start;date<=end;date=shift(date,1)) {
  if(date<tracking)continue;
  const d=dayPay(date,records[date]);out.days.push(d);
  if(date>today){out.remaining++;continue}
  out.elapsed++;for(const field of ['regular','holiday','overtime','blocks','total'])out[field]+=d[field];
 }
 out.forecast=out.total+out.remaining*500*18;return out;
}
const fmt=new Intl.NumberFormat('en-IN',{maximumFractionDigits:2});
export function money(units) {const paise=Math.round(units*100/18);return '₹'+fmt.format(paise/100)}
export function label(key,options={day:'numeric',month:'short'}) {return new Intl.DateTimeFormat('en-GB',{timeZone:'UTC',...options}).format(utc(key))}

