#!/usr/bin/env python3
"""SaaS API Test Suite v2"""
import urllib.request, urllib.error, json, sys

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
    except Exception as e:
        return 0, {"code":0, "msg":str(e)}

def t(desc, exp_code, status, body):
    global P, F
    code = body.get("code", 0) if body else 0
    if status == exp_code and code == exp_code:
        print(f"  PASS: {desc}")
        P += 1
    else:
        msg = body.get("msg","") if body else ""
        print(f"  FAIL: {desc} (HTTP={status} biz={code} exp={exp_code}) {msg[:80]}")
        F += 1

def getd(body, key, default=None):
    if not body: return default
    d = body.get("data")
    if not d: return default
    return d.get(key, default)

print("="*55)
print(" SaaS Framework - API Test Suite")
print("="*55)

# ---- 1. Auth ----
print("\n[1. Auth]")
s,b = api("POST","/auth/login",{"username":"admin","password":"wrong"})
t("Wrong password -> HTTP 400", 400, s, b)

s,b = api("POST","/auth/login",{"username":"admin","password":"123456"})
ATOKEN = getd(b,"token","")
print(f"  Admin login: {'OK' if ATOKEN else 'FAIL'}")
P += 1 if ATOKEN else 0; F += 0 if ATOKEN else 1

s,b = api("GET","/auth/info", token=ATOKEN)
t("User info (super admin)", 200, s, b)

s,b = api("GET","/permission/tree", token=ATOKEN)
t("Permission tree", 200, s, b)

# ---- 2. Tenant ----
print("\n[2. Tenant Mgmt]")
s,b = api("POST","/tenant",{"name":"School Alpha","code":"alpha"},token=ATOKEN)
t1u = getd(b,"adminUsername",""); t1p = getd(b,"adminPassword","")
t("Create tenant A", 200, s, b)

s,b = api("POST","/tenant",{"name":"School Beta","code":"beta"},token=ATOKEN)
t2u = getd(b,"adminUsername",""); t2p = getd(b,"adminPassword","")
t("Create tenant B", 200, s, b)

s,b = api("GET","/tenant/page?page=1&size=10", token=ATOKEN)
ttl = getd(b,"total",-1)
recs = getd(b,"records",[])
names = [r["name"] for r in recs] if recs else []
t(f"Tenant list (total={ttl}, names={names})", 200, s, b)

# ---- 3. Tenant Login ----
print("\n[3. Tenant Login]")
s,b = api("POST","/auth/login",{"username":t1u,"password":t1p})
T1TOKEN = getd(b,"token","")
t1_tid = getd(b,"userInfo",{}).get("tenantId","?") if b else "?"
print(f"  Tenant A: {'OK' if T1TOKEN else 'FAIL'} (tenantId={t1_tid})")
P += 1 if T1TOKEN else 0; F += 0 if T1TOKEN else 1

s,b = api("POST","/auth/login",{"username":t2u,"password":t2p})
T2TOKEN = getd(b,"token","")
t2_tid = getd(b,"userInfo",{}).get("tenantId","?") if b else "?"
print(f"  Tenant B: {'OK' if T2TOKEN else 'FAIL'} (tenantId={t2_tid})")
P += 1 if T2TOKEN else 0; F += 0 if T2TOKEN else 1

# ---- 4. Assign permissions to tenant admin role ----
print("\n[4. Assign Permissions]")
# Super admin gets role list to find tenant A's admin role
s,b = api("GET","/role/page?page=1&size=50", token=ATOKEN)
roles = getd(b,"records",[])
print(f"  Found {len(roles)} roles")
for r in roles:
    print(f"    Role: id={r['id']} name={r['name']} tenantId={r['tenantId']}")

# Find tenant A's admin role (by tenantId from login)
# Assign student permissions to it
t1_role = None
t2_role = None
for r in roles:
    if r.get("tenantId") == t1_tid:
        t1_role = r
    elif r.get("tenantId") == t2_tid:
        t2_role = r

if t1_role and t2_role:
    # Update role with student permissions (41=student:list, 42=add, 43=edit, 44=delete)
    s,b = api("PUT",f"/role/{t1_role['id']}",
        {"name": t1_role['name'], "permissionIds": [41,42,43,44]},
        token=ATOKEN)
    t(f"Assign student perms to tenant A role", 200, s, b)

    # Also assign to tenant B's role
    s,b = api("PUT",f"/role/{t2_role['id']}",
        {"name": t2_role['name'], "permissionIds": [41,42,43,44]},
        token=ATOKEN)
    t(f"Assign student perms to tenant B role", 200, s, b)

    # Re-login tenant admins to get updated permissions
    s,b = api("POST","/auth/login",{"username":t1u,"password":t1p})
    T1TOKEN = getd(b,"token","")

    s,b = api("POST","/auth/login",{"username":t2u,"password":t2p})
    T2TOKEN = getd(b,"token","")
else:
    print("  WARNING: Tenant admin role not found, skipping permission assignment")

# ---- 5. Student CRUD + Tenant Isolation ----
print("\n[5. Student CRUD & Isolation]")
s,b = api("POST","/student",{"name":"Alice","studentNo":"A001","grade":"Grade1","phone":"111"},token=T1TOKEN)
t("Tenant A add Alice", 200, s, b)

s,b = api("POST","/student",{"name":"Alice2","studentNo":"A002","grade":"Grade2"},token=T1TOKEN)
t("Tenant A add Alice2", 200, s, b)

s,b = api("POST","/student",{"name":"Bob","studentNo":"B001","grade":"Grade1"},token=T2TOKEN)
t("Tenant B add Bob", 200, s, b)

# Check Tenant A isolation
s,b = api("GET","/student/page?page=1&size=10", token=T1TOKEN)
recs = getd(b,"records",[]) or []
a_names = [r["name"] for r in recs]; a_total = getd(b,"total",0)
has_bob_in_a = "Bob" in a_names
print(f"  Tenant A: total={a_total} names={a_names}")
if a_total == 2 and not has_bob_in_a:
    print(f"  PASS: Tenant A isolated (sees 2, no Bob)")
    P += 1
else:
    print(f"  FAIL: Tenant A isolation (has_bob={has_bob_in_a})")
    F += 1

# Check Tenant B isolation
s,b = api("GET","/student/page?page=1&size=10", token=T2TOKEN)
recs = getd(b,"records",[]) or []
b_names = [r["name"] for r in recs]; b_total = getd(b,"total",0)
has_alice_in_b = "Alice" in b_names
print(f"  Tenant B: total={b_total} names={b_names}")
if b_total == 1 and not has_alice_in_b:
    print(f"  PASS: Tenant B isolated (sees 1, no Alice)")
    P += 1
else:
    print(f"  FAIL: Tenant B isolation (has_alice={has_alice_in_b})")
    F += 1

# Super admin sees all
s,b = api("GET","/student/page?page=1&size=10", token=ATOKEN)
sa_total = getd(b,"total",0)
print(f"  Super Admin: total={sa_total}")
if sa_total >= 3:
    print(f"  PASS: Super admin sees all students")
    P += 1
else:
    print(f"  FAIL: Super admin (total={sa_total})")
    F += 1

# ---- 6. Security ----
print("\n[6. Security]")
s,b = api("GET","/student/page?page=1&size=10")
t("Unauthorized -> 401", 401, s, b)

s,b = api("POST","/tenant",{"name":"Hack","code":"hack"},token=T1TOKEN)
t("Tenant user blocked from tenant mgmt -> 403", 403, s, b)

# ---- Summary ----
print("\n" + "="*55)
print(f"  Results: {P}/{P+F} passed, {F}/{P+F} failed")
print("="*55)
sys.exit(0 if F == 0 else 1)
