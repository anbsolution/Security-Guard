const AUTH_KEY="ssg_auth_v1";
const USER_KEY="ssg_user_v1";
const getAuth=()=>JSON.parse(localStorage.getItem(AUTH_KEY)||"null");
const setAuth=v=>localStorage.setItem(AUTH_KEY,JSON.stringify(v));
function switchAuth(mode){
  document.querySelectorAll(".auth-tab").forEach(x=>x.classList.toggle("active",x.dataset.auth===mode));
  document.getElementById("loginForm").classList.toggle("hidden",mode!=="login");
  document.getElementById("signupForm").classList.toggle("hidden",mode!=="signup");
}
function authInit(){
  document.querySelectorAll(".auth-tab").forEach(b=>b.onclick=()=>switchAuth(b.dataset.auth));
  const pass=document.getElementById("signupPassword");
  pass.oninput=()=>{
    const n=pass.value.length, box=document.querySelector(".strength"), txt=document.getElementById("strengthText");
    box.className="strength "+(n>=10?"very":n>=8?"strong":n>=6?"weak":"");
    txt.textContent=n<6?"Minimum 6 characters":n>=10?"Very Strong":n>=8?"Strong":"Weak";
  };
  document.getElementById("signupForm").onsubmit=e=>{
    e.preventDefault();
    const id=document.getElementById("signupId").value.trim().toLowerCase();
    const p=document.getElementById("signupPassword").value, c=document.getElementById("signupConfirm").value;
    if(p!==c){document.getElementById("signupMsg").textContent="Passwords do not match.";return}
    if(p.length<6||p.length>10){document.getElementById("signupMsg").textContent="Password must be 6-10 characters.";return}
    const user={name:document.getElementById("signupName").value.trim(),id,password:p};
    localStorage.setItem(USER_KEY,JSON.stringify(user)); setAuth({id});
    location.reload();
  };
  document.getElementById("loginForm").onsubmit=e=>{
    e.preventDefault();
    const u=JSON.parse(localStorage.getItem(USER_KEY)||"null"), id=document.getElementById("loginId").value.trim().toLowerCase(), p=document.getElementById("loginPassword").value;
    if(u&&u.id===id&&u.password===p){setAuth({id});location.reload()}
    else document.getElementById("loginMsg").textContent="Invalid login details.";
  };
  document.getElementById("logoutBtn").onclick=()=>{localStorage.removeItem(AUTH_KEY);location.reload()};
}
document.addEventListener("DOMContentLoaded",authInit);