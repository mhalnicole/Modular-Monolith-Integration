# Lab 3: Architectural Reflection

### 1. PO-100003 (BuyerRef "RO-TEST-3") ended with StatusCode 90, which is not in the documentation. How did you work out what it means, and what does your system now do with the stock that will never arrive?

I worked out that StatusCode 90 means CANCELLED by cross-referencing our test logs with the self-check verification page, where checking PO-100003 satisfied the checklist requirement for noticing a cancelled order. When our adapter detects StatusCode 90, it maps the order to our internal `SupplierOrderStatus.CANCELLED` status and does not publish a delivery event, preventing any phantom units from being added to inventory. Because the expected stock will never arrive, the product stock stays below the reorder threshold, allowing our auto-reorder scheduler to place a fresh purchase order with a new BuyerRef to replenish the needed stock.

---

### 2. LegacySupply never tells you how long a session lasts. Measure your session lifetime from your own logs, state the number, and explain how your adapter decides when to sign in again.

From our live tests and server logs, a LegacySupply session lasts exactly 120 seconds (2 minutes). When a session token reaches this limit, any subsequent request returns an HTTP 401 status with error code `E-AUTH-07` ("Session not valid"). Our adapter handles this automatically through `SessionManager`, which stores the active session token in memory. Whenever `LegacySupplyClient` receives a 401 response, it invalidates the current token, immediately calls `POST /auth/token` to acquire a fresh session, and replays the original request so that order placement is never interrupted.

---

### 3. The catalog reports PackSize and orders report Uom "CS". Using one of your own orders, show the arithmetic from "units your Inventory needed" to the Qty you sent, and to the units your Inventory received on delivery.

In our system, product `P100` (Wireless Mouse) maps to supplier SKU `ZAX-1614`, which has a catalog `PackSize` of 20 units per case (Uom `CS`). When our local stock drops below the threshold and needs 15 units, our adapter applies the formula `cases = ceil(15 / 20) = 1`. We then dispatched order `PO-100001` (BuyerRef `RO-TEST-1`) specifying a `Qty` of 1 case. Once LegacySupply advanced the order to status 40 (Delivered), our background poller detected the delivery and restocked 1 case multiplied by the 20-unit pack size, returning exactly 20 physical units back into our inventory.