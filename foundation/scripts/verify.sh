#!/usr/bin/env bash
# 对本机已启动的底座做接口验证。演示口令与种子脚本一致，只用于本地。
set -euo pipefail
BASE="${BASE:-http://127.0.0.1:18080}"
PASSWORD="${DEMO_PASSWORD:-admin123}"

curl_api() {
  curl --noproxy '*' -sS "$@"
}

login() {
  curl_api -X POST "$BASE/api/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"$1\",\"password\":\"$PASSWORD\"}"
}

token_of() {
  python3 -c 'import json,sys; print(json.load(sys.stdin)["data"]["token"])'
}

echo "--- anonymous members ---"
curl_api -o /tmp/foundation-anon.json -w "status:%{http_code}\n" "$BASE/api/members"
python3 -c 'import json; print(json.load(open("/tmp/foundation-anon.json"))["message"])'

echo "--- admin login and org count ---"
ADMIN=$(login admin)
echo "$ADMIN" | python3 -c 'import json,sys; u=json.load(sys.stdin)["data"]["user"]; print(u["username"], u["role"], u["canWrite"])'
ADMIN_TOKEN=$(echo "$ADMIN" | token_of)
curl_api "$BASE/api/orgs/tree" -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -c 'import json,sys
def walk(nodes):
    names=[]
    for n in nodes:
        names.append(n["name"]+"/"+n["orgType"])
        names += walk(n["children"])
    return names
data=json.load(sys.stdin)["data"]
names=walk(data)
print("orgs", len(names))
print(" | ".join(names))'

echo "--- admin creates member ---"
curl_api -X POST "$BASE/api/members" \
  -H "Authorization: Bearer $ADMIN_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"验证新增","gender":"女","phone":"13900000000","memberNo":"DY-VERIFY","orgId":6,"status":"ACTIVE","joinedAt":"2024-06-01"}' \
  | python3 -c 'import json,sys; d=json.load(sys.stdin)["data"]; print(d["id"], d["name"], d["orgName"])'

echo "--- admin query by name ---"
curl_api "$BASE/api/members?name=%E9%AA%8C%E8%AF%81%E6%96%B0%E5%A2%9E" -H "Authorization: Bearer $ADMIN_TOKEN" \
  | python3 -c 'import json,sys; rows=json.load(sys.stdin)["data"]; print("matched", len(rows), rows[0]["memberNo"])'

echo "--- general_admin scope ---"
GENERAL=$(login general_admin)
GENERAL_TOKEN=$(echo "$GENERAL" | token_of)
curl_api "$BASE/api/members" -H "Authorization: Bearer $GENERAL_TOKEN" \
  | python3 -c 'import json,sys; rows=json.load(sys.stdin)["data"]; print("members", [(r["name"], r["orgName"]) for r in rows])'
echo -n "cross-branch member status:"
curl_api -o /tmp/foundation-cross.json -w "%{http_code}\n" "$BASE/api/members/3" -H "Authorization: Bearer $GENERAL_TOKEN"
python3 -c 'import json; print(json.load(open("/tmp/foundation-cross.json"))["message"])'
echo -n "create outside scope status:"
curl_api -o /tmp/foundation-out.json -w "%{http_code}\n" -X POST "$BASE/api/members" \
  -H "Authorization: Bearer $GENERAL_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"越权","orgId":6,"status":"ACTIVE"}'
python3 -c 'import json; print(json.load(open("/tmp/foundation-out.json"))["message"])'

echo "--- branch_viewer cannot write ---"
VIEWER=$(login branch_viewer)
VIEWER_TOKEN=$(echo "$VIEWER" | token_of)
echo -n "viewer write status:"
curl_api -o /tmp/foundation-viewer.json -w "%{http_code}\n" -X POST "$BASE/api/members" \
  -H "Authorization: Bearer $VIEWER_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"只读写入","orgId":4,"status":"ACTIVE"}'
python3 -c 'import json; print(json.load(open("/tmp/foundation-viewer.json"))["message"])'
curl_api "$BASE/api/members" -H "Authorization: Bearer $VIEWER_TOKEN" \
  | python3 -c 'import json,sys; rows=json.load(sys.stdin)["data"]; print("viewer members", [r["name"] for r in rows])'

echo "--- branch_admin sibling denied ---"
BRANCH=$(login branch_admin)
BRANCH_TOKEN=$(echo "$BRANCH" | token_of)
echo -n "sibling create status:"
curl_api -o /tmp/foundation-sibling.json -w "%{http_code}\n" -X POST "$BASE/api/members" \
  -H "Authorization: Bearer $BRANCH_TOKEN" -H 'Content-Type: application/json' \
  -d '{"name":"兄弟支部","orgId":5,"status":"ACTIVE"}'
python3 -c 'import json; print(json.load(open("/tmp/foundation-sibling.json"))["message"])'

echo "VERIFY_DONE"
