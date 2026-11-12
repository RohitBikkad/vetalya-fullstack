import test from 'node:test';
import assert from 'node:assert/strict';
import { createApiClient } from '../src/client.js';
import { createRequire } from 'node:module';
const require = createRequire(import.meta.url);
const appConfig = require('../../../apps/pet-owner-mobile/app.config.js');
function store() {
  let state = { accessToken: 'old-access', refreshToken: 'old-refresh' };
  return { getAccessToken: async () => state.accessToken, getRefreshToken: async () => state.refreshToken,
    saveTokens: async tokens => { state = { ...state, ...tokens }; }, clear: async () => { state = {}; }, state: () => state };
}
const response = (data, status = 200) => new Response(status === 204 ? null : JSON.stringify(data), { status });
test('native login uses same API with native transport and no cookies', async () => {
  const tokenStore=store(); let sent;
  const api=createApiClient({ baseUrl:'https://api.example.test/api',platform:'android',tokenStore,
    fetchImpl:async (url,options) => {sent={url,options}; return response({accessToken:'a',refreshToken:'r'});} });
  await api.login({ email:'owner@example.test', password:'sample', deviceId:'device-1',clientType:'web' });
  assert.equal(sent.url,'https://api.example.test/api/auth/login');
  assert.equal(sent.options.credentials,'omit');
  assert.equal(sent.options.headers.Authorization,undefined);
  assert.equal(JSON.parse(sent.options.body).clientType,'android');
  assert.equal(tokenStore.state().refreshToken,'r');
});
test('web refresh uses HttpOnly cookie and does not persist a body refresh token', async () => {
  const tokenStore=store(); let sent;
  const api=createApiClient({baseUrl:'/api',platform:'web',tokenStore,fetchImpl:async (url,options)=>{sent=options;return response({accessToken:'web'});}});
  await api.refreshSession();
  assert.equal(sent.credentials,'include'); assert.equal(sent.body,undefined);
  assert.equal(sent.headers.Authorization,undefined);
  assert.equal(tokenStore.state().accessToken,'web');
});
test('concurrent 401s share one refresh rotation and retry with the new access token', async () => {
  const tokenStore=store(); let rotations=0;
  const api=createApiClient({baseUrl:'https://api.example.test/api',platform:'ios',tokenStore,fetchImpl:async (url,options)=>{
    if(url.endsWith('/auth/refresh')) { assert.equal(options.headers.Authorization,undefined); rotations++; await new Promise(resolve=>setImmediate(resolve)); return response({accessToken:'fresh',refreshToken:'rotated'}); }
    return options.headers.Authorization==='Bearer fresh'?response({pets:[]}):response({detail:'Expired'},401);
  }});
  await Promise.all([api.request('/owner/pets'),api.request('/owner/appointments')]);
  assert.equal(rotations,1);assert.equal(tokenStore.state().refreshToken,'rotated');
});
test('offline refresh preserves secure credentials; revoked refresh clears them', async () => {
  const tokenStore=store(); let offline=true;
  const api=createApiClient({baseUrl:'https://api.example.test/api',platform:'ios',tokenStore,fetchImpl:async()=>{
    if(offline) throw new TypeError('Offline'); return response({detail:'Revoked'},401);
  }});
  await assert.rejects(api.refreshSession(),/Offline/);assert.equal(tokenStore.state().refreshToken,'old-refresh');
  offline=false;await assert.rejects(api.refreshSession(),/Revoked/);assert.deepEqual(tokenStore.state(),{});
});
test('empty mutation responses are successful and error details survive', async () => {
  const tokenStore=store(); const api=createApiClient({baseUrl:'/api',platform:'web',tokenStore,fetchImpl:async(url)=>url.endsWith('/ok')?response(null,204):response({detail:'Not assigned to hospital'},403)});
  assert.equal(await api.request('/ok',{method:'POST'}),null);
  await assert.rejects(api.request('/denied'),/Not assigned to hospital/);
});
test('native base URL and production HTTPS validation reject invalid configuration', () => {
  for(const baseUrl of ['/api','https://api.example.test','https://user:password@example.test/api','https://api.example.test/api?key=1'])
    assert.throws(()=>createApiClient({baseUrl,platform:'android',tokenStore:store()}));
  assert.throws(()=>createApiClient({baseUrl:'http://10.0.2.2:8080/api',platform:'android',production:true,tokenStore:store()}),/HTTPS/);
  assert.throws(()=>createApiClient({baseUrl:'/api',platform:'unknown',tokenStore:store()}),/Unsupported/);
});
test('logout clears local state even when revocation cannot reach the server', async () => {
  const tokenStore=store();const api=createApiClient({baseUrl:'https://api.example.test/api',platform:'ios',tokenStore,fetchImpl:async()=>{throw new Error('Offline');}});
  await assert.rejects(api.logout(),/Offline/);assert.deepEqual(tokenStore.state(),{});
});
test('Expo variants have distinct install identifiers and matching callback scheme', () => {
  const old=process.env.APP_VARIANT;
  try {process.env.APP_VARIANT='development';const dev=appConfig();process.env.APP_VARIANT='production';const prod=appConfig();
    assert.equal(dev.android.package,prod.android.package+'.development');assert.equal(prod.ios.bundleIdentifier,prod.android.package);assert.equal(dev.scheme,prod.scheme);
  } finally {if(old===undefined) delete process.env.APP_VARIANT;else process.env.APP_VARIANT=old;}
});
