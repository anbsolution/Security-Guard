const KEY='ssg_user_v2';
const state={shift:localStorage.getItem('ssg_shift')||'day',page:'dashboard',attendance:JSON.parse(localStorage.getItem('ssg_att')||'{}'),punch:JSON.parse(localStorage.getItem('ssg_punch')||'{}')};
const $=s=>document.querySelector(s);
function toast(t){const x=document.createElement('div');x.className='toast';x.textContent=t;document.body.appendChild(x);setTimeout(()=>x.remove(),1800)}
function user(){return JSON.parse(localStorage.getItem(KEY)||'null')}
function saveUser(u){localStorage.setItem(KEY,JSON.stringify(u))}
function render(){user()?renderApp():renderAuth()}
function renderAuth(){
document.body.innerHTML=`<div class="auth"><div class="authbox">
<div class="logo">SSG</div><h1>Security Guard</h1><p class="muted">Offline-first duty & payroll app</p>
<div id="authform">${localStorage.getItem('ssg_started')?loginForm():signupForm()}</div></div></div>`}
function signupForm(){return `<h2>Create Profile</h2><label>Name</label><input id="name" placeholder="Guard name"><label>Employee ID</label><input id="eid" placeholder="SG001"><label>Password</label><input id="pw" type="password" minlength="6" maxlength="10" placeholder="6–10 characters"><label>Monthly Salary</label><input id="salary" type="number" placeholder="15500"><button class="btn" style="margin-top:16px" onclick="signup()">Create & Continue</button><p class="muted" style="margin-top:14px">Already registered? <a href="#" onclick="localStorage.setItem('ssg_started','1');render()">Login</a></p>`}
function loginForm(){return `<h2>Welcome Back</h2><label>Employee ID</label><input id="eid" placeholder="SG001"><label>Password</label><input id="pw" type="password" placeholder="Password"><button class="btn" style="margin-top:16px" onclick="login()">Login</button><p class="muted" style="margin-top:14px"><a href="#" onclick="localStorage.removeItem('ssg_started');render()">Create new profile</a></p>`}
function signup(){const name=$('#name').value.trim(),eid=$('#eid').value.trim(),pw=$('#pw').value,salary=Number($('#salary').value);if(!name||!eid||pw.length<6||pw.length>10||!salary)return toast('Fill all fields correctly');saveUser({name,eid,pw,salary});localStorage.setItem('ssg_started','1');toast('Profile created');render()}
function login(){const u=user();if(u&&$('#eid').value.trim()===u.eid&&$('#pw').value===u.pw){renderApp()}else toast('Invalid login')}
function renderApp(){
document.body.innerHTML=`<div class="top"><div class="brand">🛡️ SSG - Security Guard</div><div class="brand-sub">Simple • Smart • Complete</div><div class="muted" style="margin-top:5px">${user().name} • ${user().eid}</div></div><main class="container"><div id="content"></div></main><nav class="nav">${['dashboard','attendance','salary','profile'].map(p=>`<button class="${state.page===p?'active':''}" onclick="go('${p}')">${icon(p)}<br>${p[0].toUpperCase()+p.slice(1)}</button>`).join('')}</nav>`;
drawPage();
}
function icon(p){return {dashboard:'🏠',attendance:'📅',salary:'₹',profile:'👤'}[p]}
function go(p){state.page=p;renderApp()}
function drawPage(){
const c=$('#content');const u=user();
if(state.page==='dashboard')c.innerHTML=dashboard();
if(state.page==='attendance')c.innerHTML=attendance();
if(state.page==='salary')c.innerHTML=salary();
if(state.page==='profile')c.innerHTML=profile();
}
function dashboard(){const u=user(), night=state.shift==='night';return `<div class="title">Dashboard</div><div class="card section-green"><div class="screen-head green">🏠 Dashboard</div><div class="row between"><div><b>${u.name}</b><div class="muted">${u.eid}</div></div><span class="pill ${night?'purple':'green'}">${night?'NIGHT':'DAY'} SHIFT</span></div><div class="grid" style="margin-top:14px"><div class="stat"><small>Monthly Salary</small><b>₹${u.salary.toLocaleString('en-IN')}</b></div><div class="stat"><small>Attendance</small><b>${presentCount()}</b></div></div></div>
<div class="card"><h3>Shift Selection</h3><select onchange="changeShift(this.value)"><option value="day" ${!night?'selected':''}>Day Shift</option><option value="night" ${night?'selected':''}>Night Shift</option></select></div>
<div class="card"><h3>Duty Punch</h3><div class="row"><button class="btn" onclick="punch('in')">Punch In</button><button class="btn secondary" onclick="punch('out')">Punch Out</button></div><p class="muted">Today: ${state.punch[today()]?.in||'—'} → ${state.punch[today()]?.out||'—'}</p></div>
${night?`<div class="card"><h3>🌙 Night Patrol</h3><p class="muted">Round management is available for Night Shift.</p><button class="btn" onclick="patrol()">Open Patrol</button></div>`:''}
<div class="card"><h3>Today's Plan</h3><p>✓ Gate Check &nbsp; ✓ Area Monitoring</p><p>${night?'✓ Patrol Round':'✓ Routine Duty'}</p></div>`}
function changeShift(v){state.shift=v;localStorage.setItem('ssg_shift',v);drawPage();toast(v==='night'?'Night Patrol enabled':'Day Shift: Patrol hidden')}
function today(){return new Date().toISOString().slice(0,10)}
function punch(type){state.punch[today()]=state.punch[today()]||{};state.punch[today()][type]=new Date().toLocaleTimeString([],{hour:'2-digit',minute:'2-digit'});localStorage.setItem('ssg_punch',JSON.stringify(state.punch));drawPage();toast(type==='in'?'Punch In saved':'Punch Out saved')}
function attendance(){let days='';for(let i=1;i<=31;i++){const k=`2026-09-${String(i).padStart(2,'0')}`,v=state.attendance[k]||'';days+=`<div class="day ${v.toLowerCase()}" onclick="manual('${k}')">${i}</div>`}return `<div class="title">Attendance</div><div class="card section-blue"><div class="screen-head blue">📅 Attendance</div><div class="row between"><b>September 2026</b><span class="pill">Tap a date</span></div><div class="calendar" style="margin-top:12px">${days}</div><p class="muted">Green Present • Red Absent • Yellow Leave • Purple Off</p></div><div class="card"><h3>Manual Attendance</h3><input id="adate" type="date" value="${today()}"><select id="ast"><option>present</option><option>absent</option><option>leave</option><option>off</option></select><button class="btn" style="margin-top:10px" onclick="saveAtt()">Save Attendance</button></div>`}
function manual(d){$('#adate').value=d}
function saveAtt(){const d=$('#adate').value,v=$('#ast').value;state.attendance[d]=v;localStorage.setItem('ssg_att',JSON.stringify(state.attendance));drawPage();toast('Attendance saved')}
function presentCount(){return Object.values(state.attendance).filter(x=>x==='present').length}
function salary(){const u=user(),p=presentCount(),base=u.salary,per=base/26,earned=Math.max(0,p*per),pf=earned*.12,esi=earned*.02,net=earned-pf-esi;return `<div class="title">Salary</div><div class="card section-orange"><div class="screen-head orange">₹ Salary Slip</div><h3>Monthly Calculation</h3><div class="grid"><div class="stat"><small>Monthly</small><b>₹${base.toFixed(0)}</b></div><div class="stat"><small>Present Days</small><b>${p}</b></div><div class="stat"><small>PF 12%</small><b>₹${pf.toFixed(0)}</b></div><div class="stat"><small>ESI 2%</small><b>₹${esi.toFixed(0)}</b></div></div><hr><div class="row between"><b>Net Salary</b><b>₹${net.toFixed(0)}</b></div></div><div class="card"><h3>Salary Slip</h3><p class="muted">Basic/HRA/allowances can be expanded later according to your final salary rules.</p><button class="btn secondary" onclick="toast('PDF module ready for next build')">Generate Salary Slip</button></div>`}
function profile(){const u=user();return `<div class="title">Profile</div><div class="card section-purple"><div class="screen-head purple">👤 Profile</div><label>Name</label><input id="pn" value="${u.name}"><label>Employee ID</label><input value="${u.eid}" disabled><label>Monthly Salary</label><input id="ps" type="number" value="${u.salary}"><button class="btn" style="margin-top:14px" onclick="saveProfile()">Save Profile</button></div><div class="card"><button class="btn danger" onclick="logout()">Logout</button></div>`}
function saveProfile(){const u=user();u.name=$('#pn').value.trim()||u.name;u.salary=Number($('#ps').value)||u.salary;saveUser(u);renderApp();toast('Profile updated')}
function logout(){localStorage.removeItem('ssg_started');render()}
function patrol(){toast('Patrol module opened — round engine next')}
render();
