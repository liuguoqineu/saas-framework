#!/usr/bin/env python3
"""Core business logic verification"""
import urllib.request, json, sys

BASE = "http://localhost:8080/api"
P = 0; F = 0

def api(method, path, data=None, token=None):
    url = f"{BASE}{path}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, method=method, data=body)
    req.add_header("Content-Type", "application/json")
    if token: req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return r.status, json.loads(r.read().decode())
    except urllib.error.HTTPError as e:
        return e.code, json.loads(e.read().decode())
    except Exception as ex:
        return 0, {"code":0, "msg":str(ex)}

def check(name, exp, status, body):
    global P, F
    code = body.get("code", 0)
    msg = body.get("msg", "")
    if status == exp and code == exp:
        print(f"  PASS: {name}")
        P += 1
    else:
        print(f"  FAIL: {name} (HTTP={status} biz={code} exp={exp}) {msg[:60]}")
        F += 1
    return status, body

print("="*55)
print(" SaaS Core Business Logic Test")
print("="*55)

# ==== 1. Auth ====
print("\n-- 1. Login & Auth --")
s,b = api("POST","/auth/login",{"username":"admin","password":"123456"})
ATOKEN = (b.get("data") or {}).get("token","")
perms = (b.get("data") or {}).get("userInfo",{}).get("permissions",[])
check("Admin login", 200, s, b)
print(f"    permissions: {len(perms)}")

check("Wrong password->400", 400, *api("POST","/auth/login",{"username":"admin","password":"wrong"}))
check("Bad token->401", 401, *api("GET","/auth/info", token="xxx"))

# ==== 2. Tenant Management ====
print("\n-- 2. Tenant Management --")
s,b = api("POST","/tenant",{"name":"SchoolA","code":"test_a"},token=ATOKEN)
TAU = (b.get("data") or {}).get("adminUsername","")
TAP = (b.get("data") or {}).get("adminPassword","")
check("Create tenant A", 200, s, b)
print(f"    admin={TAU} pass={TAP}")

s,b = api("POST","/tenant",{"name":"SchoolB","code":"test_b"},token=ATOKEN)
TBU = (b.get("data") or {}).get("adminUsername","")
TBP = (b.get("data") or {}).get("adminPassword","")
check("Create tenant B", 200, s, b)

# Tenant A admin login
s,b = api("POST","/auth/login",{"username":TAU,"password":TAP})
T1TOK = (b.get("data") or {}).get("token","")
t1_tid = (b.get("data") or {}).get("userInfo",{}).get("tenantId",0)
check("Tenant A login", 200, s, b)
print(f"    tenantId={t1_tid}")

# Tenant B admin login
s,b = api("POST","/auth/login",{"username":TBU,"password":TBP})
T2TOK = (b.get("data") or {}).get("token","")
t2_tid = (b.get("data") or {}).get("userInfo",{}).get("tenantId",0)
check("Tenant B login", 200, s, b)

# ==== 3. Assign permissions to tenant admins ====
print("\n-- 3. Permission Assignment --")
s,b = api("GET","/role/page?page=1&size=50", token=ATOKEN)
roles = (b.get("data") or {}).get("records",[])

for r in roles:
    # Assign student+role+user perms to tenant admin roles (tenantId matches)
    if r.get("tenantId", 0) > 0:
        api("PUT",f"/role/{r['id']}",
            {"name":r['name'],"permissionIds":[21,22,23,24, 31,32,33,34, 41,42,43,44]},
            token=ATOKEN)
print("    Assigned perms (role+user+student) to tenant admin roles")

# Re-login tenants to get new permissions
s,b = api("POST","/auth/login",{"username":TAU,"password":TAP})
T1TOK = (b.get("data") or {}).get("token","")
t1p = (b.get("data") or {}).get("userInfo",{}).get("permissions",[])
print(f"    Tenant A re-login: {len(t1p)} permissions")

s,b = api("POST","/auth/login",{"username":TBU,"password":TBP})
T2TOK = (b.get("data") or {}).get("token","")
t2p = (b.get("data") or {}).get("userInfo",{}).get("permissions",[])
print(f"    Tenant B re-login: {len(t2p)} permissions")

# ==== 4. Tenant Isolation (Student CRUD) ====
print("\n-- 4. Tenant Isolation --")
check("T-A add ZhangSan", 200,
    *api("POST","/student",{"name":"ZhangSan","studentNo":"Z001","grade":"G1"},token=T1TOK))
check("T-A add LiSi-A", 200,
    *api("POST","/student",{"name":"LiSi-A","studentNo":"L001","grade":"G2"},token=T1TOK))
check("T-B add LiSi-B", 200,
    *api("POST","/student",{"name":"LiSi-B","studentNo":"L001","grade":"G2"},token=T2TOK))

# Tenant A isolation
s,b = api("GET","/student/page?page=1&size=10", token=T1TOK)
recs = (b.get("data") or {}).get("records", [])
names = [r["name"] for r in recs]
has_b = "LiSi-B" in names
if not has_b and "ZhangSan" in names:
    print(f"  PASS: T-A sees only own data ({names})")
    P += 1
else:
    print(f"  FAIL: T-A isolation failed ({names})")
    F += 1

# Tenant B isolation
s,b = api("GET","/student/page?page=1&size=10", token=T2TOK)
recs = (b.get("data") or {}).get("records", [])
names = [r["name"] for r in recs]
has_a = "ZhangSan" in names
if not has_a:
    print(f"  PASS: T-B sees only own data ({names})")
    P += 1
else:
    print(f"  FAIL: T-B isolation failed ({names})")
    F += 1

# Super admin sees all
s,b = api("GET","/student/page?page=1&size=10", token=ATOKEN)
total = (b.get("data") or {}).get("total", 0)
if total >= 3:
    print(f"  PASS: Super admin sees all students (total={total})")
    P += 1
else:
    print(f"  FAIL: Super admin (total={total})")
    F += 1

# ==== 5. Tenant Disable ====
print("\n-- 5. Tenant Disable --")
s,b = api("GET","/tenant/page?page=1&size=50", token=ATOKEN)
tenants = (b.get("data") or {}).get("records", [])
ta_id = next((t["id"] for t in tenants if t.get("code")=="test_a"), None)

if ta_id:
    api("PUT",f"/tenant/{ta_id}/status",{"status":0}, token=ATOKEN)
    s,b = api("POST","/auth/login",{"username":TAU,"password":TAP})
    msg = b.get("msg","")
    if b.get("code") != 200 and ("租户" in msg or "禁用" in msg):
        print(f"  PASS: Disabled tenant login blocked: {msg}")
        P += 1
    else:
        print(f"  FAIL: Disabled tenant still can login (code={b.get('code')})")
        F += 1
    api("PUT",f"/tenant/{ta_id}/status",{"status":1}, token=ATOKEN)

# ==== 6. Permission Inheritance ====
print("\n-- 6. Permission Inheritance --")
# Super admin creates role with limited perms
s,b = api("POST","/role",{"name":"LimitedRole","permissionIds":[41,42]},token=ATOKEN)
check("Create limited role", 200, s, b)

# Get that role's ID
s,b = api("GET","/role/page?page=1&size=50", token=ATOKEN)
all_r = (b.get("data") or {}).get("records", [])
lim_id = next((r["id"] for r in all_r if r.get("name")=="LimitedRole"), None)

# Tenant A admin tries to create a role with student:delete (44)
# which the admin DOES have now (from the 12 perms assigned earlier)
# First verify admin has role:add and student:delete
# Then restrict: remove student:delete but keep role management perms
for r in all_r:
    if r.get("tenantId") == t1_tid:
        # Give all perms EXCEPT student:delete (44)
        api("PUT",f"/role/{r['id']}",
            {"name":r['name'],"permissionIds":[21,22,23,24,31,32,33,34,41,42,43]},
            token=ATOKEN)

# Re-login T-A admin (now has 11 perms, missing student:delete)
s,b = api("POST","/auth/login",{"username":TAU,"password":TAP})
T1TOK = (b.get("data") or {}).get("token","")
t1p = (b.get("data") or {}).get("userInfo",{}).get("permissions",[])
print(f"    T-A admin now has {len(t1p)} perms (no student:delete)")

# T-A admin tries to create role WITH student:delete (44) which they don't have
# They still have role:add (22) so the permission check should run
s,b = api("POST","/role",{"name":"SuperRole","permissionIds":[41,44]},token=T1TOK)
if b.get("code") != 200:
    print(f"  PASS: Blocked excess permission assignment: {b.get('msg','')}")
    P += 1
else:
    print(f"  FAIL: Should block excess permission assignment")
    F += 1

# T-A admin creates role WITHIN their permission scope (only [41,42])
s,b = api("POST","/role",{"name":"ValidRole","permissionIds":[41,42]},token=T1TOK)
check("Create role within permission scope", 200, s, b)

# ==== 7. Role deletion with users ====
print("\n-- 7. Role Delete Protection --")
# Try to delete the tenant admin role (has users assigned)
for r in all_r:
    if r.get("tenantId") == t1_tid:
        s,b = api("DELETE",f"/role/{r['id']}",token=ATOKEN)
        if b.get("code") != 200:
            print(f"  PASS: Cannot delete role with users: {b.get('msg','')}")
            P += 1
        else:
            print(f"  FAIL: Should block role deletion with users")
            F += 1
        break

# ==== 8. Security ====
print("\n-- 8. Security --")
# Tenant admin cannot access tenant management
check("Tenant user blocked 403", 403,
    *api("GET","/tenant/page?page=1&size=10", token=T1TOK))

# Super admin cannot access employee management
check("Super admin blocked from user mgmt 403", 403,
    *api("GET","/user/page?page=1&size=10", token=ATOKEN))

# No student:add permission = blocked
check("Invalid token->401 not 403", 401,
    *api("POST","/student",{"name":"Hack","studentNo":"H","grade":"G"}, token="bad_token_xyz"))

# ==== Summary ====
print("\n" + "="*55)
print(f"  Results: {P}/{P+F} passed, {F}/{P+F} failed")
print("="*55)
sys.exit(0 if F == 0 else 1)
