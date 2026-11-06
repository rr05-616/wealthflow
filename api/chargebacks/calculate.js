import {db,json,requireUser} from '../_lib.js';
import {allocate} from '../_rounding.js';
export default async function handler(req,res){
 if(req.method!=='POST')return json(res,{error:'METHOD_NOT_ALLOWED'},405);
 const auth=await requireUser(req,['labUser','billingOfficer']);if(auth.error)return json(res,auth.error,auth.error.status);
 try{const {totalAmount,allocations,rateVersion=null}=req.body||{};const result={chargebackId:crypto.randomUUID(),status:'CALCULATED',currency:'INR',...allocate(totalAmount,allocations),rateVersion};result.exactShares=result.lines.map(({id,share,exactShare})=>({id,share,exactShare}));await db('CHARGEBACK')`INSERT INTO chargebacks(id,owner_id,payload,status) VALUES(${result.chargebackId},${auth.user.id},${JSON.stringify(result)}::jsonb,'CALCULATED')`;return json(res,result,201);}catch(e){return json(res,{error:'VALIDATION_ERROR',message:e.message},400);}
}