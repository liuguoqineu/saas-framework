#!/usr/bin/env python3
"""SaaS 多租户框架 - 完整 API 测试套件"""
import urllib.request, urllib.error, json, sys

BASE = "http://localhost:8080/api"
passed = 0
failed = 0

def api(method, path, data=None, token=None):
    url = f"{BASE}{path}"
    body = json.dumps(data).encode() if data else None
    req = urllib.request.Request(url, method=method, data=body)
    req.add_header("Content-Type", "application/json")
    if token:
        req.add_header("Authorization", f"Bearer {token}")
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        body = json.loads(e.read().decode())
        return e.code, body
    except Exception as e:
        return 0, {"code": 0, "msg": str(e), "data": None}

def test(desc, expected_code, status, body, extra=""):
    global passed, failed
    biz_code = body.get("code", 0)
    if status == expected_code and biz_code == expected_code:
        print(f"  PASS: {desc}")
        passed += 1
    else:
        msg = body.get("msg", "")
        print(f"  FAIL: {desc} (HTTP={status} biz={biz_code} expected={expected_code}) msg={msg} {extra}")
        failed += 1

def get_token(status, body):
    return body.get("data", {}).get("token", "") if status == 200 else ""

print("=" * 60)
print("  SaaS Multi-Tenant Framework - API Test Suite")
print("=" * 60)

# ===== 1. Authentication =====
print("\n--- 1. Authentication ---")

# 1.1 Login with wrong password
status, body = api("POST", "/auth/login", {"username": "admin", "password": "wrong"})
test("Wrong password returns 400", 400, status, body)

# 1.2 Login with correct credentials
status, body = api("POST", "/auth/login", {"username": "admin", "password": "123456"})
admin_token = get_token(status, body)
biz = body.get("code")
if biz == 200 and admin_token:
    print(f"  PASS: admin login OK")
    print(f"  Token: {admin_token[:50]}...")
    user_info = body.get("data", {}).get("userInfo", {})
    print(f"  TenantId: {user_info.get('tenantId')}, Permissions: {len(user_info.get('permissions', []))}")
    passed += 1
else:
    print(f"  FAIL: admin login - {body}")
    failed += 1

# 1.3 Get current user info
status, body = api("GET", "/auth/info", token=admin_token)
test("User info (tenantId=0)", 200, status, body,
     f"tenantId={body.get('data',{}).get('tenantId')}")

# ===== 2. Permission Tree =====
print("\n--- 2. Permission Tree ---")
status, body = api("GET", "/permission/tree", token=admin_token)
tree = body.get("data", [])
node_count = len(tree)
test(f"Permission tree ({node_count} root nodes)", 200, status, body)

# ===== 3. Tenant Management =====
print("\n--- 3. Tenant Management ---")

# Create tenant A
status, body = api("POST", "/tenant",
    {"name": "School Alpha", "code": "alpha"}, token=admin_token)
t1_user = body.get("data", {}).get("adminUsername", "")
t1_pass = body.get("data", {}).get("adminPassword", "")
test("Create tenant A", 200, status, body, f"admin={t1_user} pass={t1_pass}")

# Create tenant B
status, body = api("POST", "/tenant",
    {"name": "School Beta", "code": "beta"}, token=admin_token)
t2_user = body.get("data", {}).get("adminUsername", "")
t2_pass = body.get("data", {}).get("adminPassword", "")
test("Create tenant B", 200, status, body, f"admin={t2_user} pass={t2_pass}")

# Tenant list
status, body = api("GET", "/tenant/page?page=1&size=10", token=admin_token)
total = body.get("data", {}).get("total", 0)
test(f"Tenant list (total={total})", 200, status, body)

# ===== 4. Tenant Admin Login =====
print("\n--- 4. Tenant Admin Login ---")

status, body = api("POST", "/auth/login",
    {"username": t1_user, "password": t1_pass})
t1_token = get_token(status, body)
biz = body.get("code")
if biz == 200 and t1_token:
    t1_info = body.get("data", {}).get("userInfo", {})
    print(f"  PASS: Tenant A login ({t1_user}, tenantId={t1_info.get('tenantId')})")
    passed += 1
else:
    print(f"  FAIL: Tenant A login - {body}")
    failed += 1

status, body = api("POST", "/auth/login",
    {"username": t2_user, "password": t2_pass})
t2_token = get_token(status, body)
biz = body.get("code")
if biz == 200 and t2_token:
    t2_info = body.get("data", {}).get("userInfo", {})
    print(f"  PASS: Tenant B login ({t2_user}, tenantId={t2_info.get('tenantId')})")
    passed += 1
else:
    print(f"  FAIL: Tenant B login - {body}")
    failed += 1

# ===== 5. Student CRUD & Tenant Isolation =====
print("\n--- 5. Student CRUD & Tenant Isolation ---")

# Tenant A adds student Alice
status, body = api("POST", "/student",
    {"name": "Alice", "studentNo": "A001", "grade": "Grade1", "phone": "111"}, token=t1_token)
test("Tenant A add Alice", 200, status, body)

# Tenant A adds student Alice2
status, body = api("POST", "/student",
    {"name": "Alice2", "studentNo": "A002", "grade": "Grade2", "phone": "112"}, token=t1_token)
test("Tenant A add Alice2", 200, status, body)

# Tenant B adds student Bob
status, body = api("POST", "/student",
    {"name": "Bob", "studentNo": "B001", "grade": "Grade1", "phone": "222"}, token=t2_token)
test("Tenant B add Bob", 200, status, body)

# Tenant A: list students (should see 2, no Bob)
status, body = api("GET", "/student/page?page=1&size=10", token=t1_token)
a_records = body.get("data", {}).get("records", [])
a_names = [r["name"] for r in a_records]
a_total = body.get("data", {}).get("total", 0)
has_bob = "Bob" in a_names
if a_total == 2 and not has_bob:
    print(f"  PASS: Tenant A sees 2 students (Alice, Alice2) - ISOLATED")
    passed += 1
else:
    print(f"  FAIL: Tenant A isolation (total={a_total} names={a_names} has_bob={has_bob})")
    failed += 1

# Tenant B: list students (should see 1, no Alice)
status, body = api("GET", "/student/page?page=1&size=10", token=t2_token)
b_records = body.get("data", {}).get("records", [])
b_names = [r["name"] for r in b_records]
b_total = body.get("data", {}).get("total", 0)
has_alice = "Alice" in b_names
if b_total == 1 and not has_alice:
    print(f"  PASS: Tenant B sees 1 student (Bob) - ISOLATED")
    passed += 1
else:
    print(f"  FAIL: Tenant B isolation (total={b_total} names={b_names} has_alice={has_alice})")
    failed += 1

# Super admin: sees ALL students
status, body = api("GET", "/student/page?page=1&size=10", token=admin_token)
sa_total = body.get("data", {}).get("total", 0)
if sa_total >= 3:
    print(f"  PASS: Super admin sees ALL students (total={sa_total})")
    passed += 1
else:
    print(f"  FAIL: Super admin student view (total={sa_total})")
    failed += 1

# ===== 6. Security Tests =====
print("\n--- 6. Security ---")

# No token
status, body = api("GET", "/student/page?page=1&size=10")
test("No token -> 401", 401, status, body)

# Tenant user cannot access tenant management
status, body = api("POST", "/tenant",
    {"name": "Hack", "code": "hack"}, token=t1_token)
test("Tenant user cannot create tenant -> 403", 403, status, body)

# ===== 7. Role Management =====
print("\n--- 7. Role Management ---")

# Super admin creates a role
status, body = api("POST", "/role",
    {"name": "Student Manager", "permissionIds": [41, 42, 43, 44]}, token=admin_token)
test("Super admin create role", 200, status, body)

# Role list
status, body = api("GET", "/role/page?page=1&size=10", token=admin_token)
r_total = body.get("data", {}).get("total", 0)
test(f"Role list (total={r_total})", 200, status, body)

# ===== 8. Employee Management =====
print("\n--- 8. Employee Management ---")

# Tenant admin creates employee
status, body = api("POST", "/user",
    {"username": "employee_a1", "realName": "Employee A1", "roleId": 2, "password": "123456"},
    token=t1_token)
test("Tenant A create employee", 200, status, body)

# Employee login
status, body = api("POST", "/auth/login",
    {"username": "employee_a1", "password": "123456"})
emp_token = get_token(status, body)
biz = body.get("code")
emp_info = body.get("data", {}).get("userInfo", {}) if biz == 200 else {}
emp_tenant = emp_info.get("tenantId", "N/A")
if biz == 200 and emp_tenant != 0:
    print(f"  PASS: Employee login (tenantId={emp_tenant})")
    passed += 1
else:
    print(f"  FAIL: Employee login - biz={biz} tenant={emp_tenant} - {body}")
    failed += 1

# Employee list
status, body = api("GET", "/user/page?page=1&size=10", token=t1_token)
u_total = body.get("data", {}).get("total", 0)
test(f"Tenant A employee list (total={u_total})", 200, status, body)

# ===== Summary =====
print("\n" + "=" * 60)
total = passed + failed
print(f"  Results: {passed}/{total} passed, {failed}/{total} failed")
print("=" * 60)

sys.exit(0 if failed == 0 else 1)
