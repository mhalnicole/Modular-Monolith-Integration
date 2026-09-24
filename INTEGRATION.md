# LegacySupply Integration & Anti-Corruption Layer (ACL)

## 1. Product Mapping Table

| Internal Product ID | Product Name | LegacySupply SupplierSku | PackSize | Unit of Measure (Uom) | Unit Cost |
| :--- | :--- | :--- | :--- | :--- | :--- |
| P100 | Wireless Mouse | `ZAX-1614` | 20 | CS (Cases) | ₱450.00 |
| P200 | Mechanical Keyboard | `ZAX-1252` | 10 | CS (Cases) | ₱1,899.00 |
| P300 | USB-C Hub | `ZAX-4488` | 10 | CS (Cases) | ₱399.00 |

---

## 2. Session Mechanics & Lifespan

- **Authentication Endpoint:** `POST /auth/token`
- **Request Body (XML):**
  ```xml
  <AuthRequest>
    <ClientId>23-0065-102</ClientId>
    <ApiKey>LSK-BB9C86DC2BC5D210014E</ApiKey>
  </AuthRequest>
  ```
- **Response Body (XML):**
  ```xml
  <AuthResponse>
    <SessionToken>6a18efb55c4191b4b3b6b21056d7d96160fe</SessionToken>
    <IssuedAt>2026-09-24T10:55:25.090Z</IssuedAt>
  </AuthResponse>
  ```
- **Subsequent Requests Header:** `X-LS-Session: <SessionToken>`
- **Measured Session Lifespan:** A LegacySupply session lasts **exactly 120 seconds (2 minutes)**. Testing a token after 120 seconds returned `HTTP 401` with error `E-AUTH-07` (*"Session not valid"*).
- **Renewal Strategy:** Our `SessionManager` caches the session token in memory. When any downstream call receives an HTTP 401 with `E-AUTH-03` or `E-AUTH-07`, the adapter automatically invalidates the cached token, requests a fresh token via `POST /auth/token`, and retries the request without requiring any manual intervention.

---

## 3. Qty and Uom Explained

- **Qty (Quantity):** The order quantity in terms of supplier packaging units. It must be an integer between 1 and 99.
- **Uom (Unit of Measure):** The unit in which LegacySupply packages and ships the product (`CS` for Cases). LegacySupply does not sell loose individual pieces.
- **Worked Example from our Catalog:**
  - **Internal Requirement:** Stock for `P100` (Wireless Mouse) drops to 3 units, requiring an auto-reorder of **15 units**.
  - **Catalog Specification:** `ZAX-1614` has a `PackSize` of **20 units per case** (`CS`).
  - **Unit Conversion:** `cases = ceil(15 / 20) = 1 case`.
  - **XML Payload Sent:**
    ```xml
    <PurchaseOrder>
      <SupplierSku>ZAX-1614</SupplierSku>
      <Qty>1</Qty>
      <BuyerRef>RO-100001</BuyerRef>
    </PurchaseOrder>
    ```
  - **Delivery & Restock:** When LegacySupply updates the order to `StatusCode 40` (Delivered), our scheduled poller fires `SupplierOrderDeliveredEvent(P100, 20)`. The `Inventory` module receives this event and restocks **20 individual units** (`1 case * 20 pack size`) into stock.

---

## 4. Error Codes & Causes Reference

| Code | HTTP Status | Official Message | Root Cause in Practice |
| :--- | :---: | :--- | :--- |
| `E-AUTH-01` | 401 | Credentials rejected. | Invalid Client ID or API key passed in `AuthRequest`. |
| `E-AUTH-02` | 401 | Session header missing. | Request omitted the `X-LS-Session` header. |
| `E-AUTH-03` | 401 | Session not recognized. | Session token has expired or server restarted. |
| `E-AUTH-07` | 401 | Session not valid. | Malformed or tampered session token string. |
| `E-FMT-01` | 415 | Unsupported media. | Request body was not sent with `Content-Type: application/xml`. |
| `E-FMT-02` | 400 | Malformed document. | XML body has invalid tags or failed XML validation. |
| `E-REF-05` | 400 | BuyerRef invalid. | `BuyerRef` exceeded 40 characters or was left blank. |
| `E-SKU-02` | 422 | Item not recognized. | Requested `SupplierSku` does not belong to the partner's catalog. |
| `E-QTY-11` | 422 | Quantity invalid. | `Qty` was less than 1 or greater than 99. |
| `E-IDEM-04` | 409 | Request id reused with different content. | Same `X-Request-Id` header was sent with differing payload. |
| `E-PO-04` | 404 | Order not found. | Queried `PoNumber` does not exist on LegacySupply. |
| `E-QRY-06` | 400 | Query parameter required. | Missing query string (e.g. `?buyerRef=`). |
| `E-RATE-03` | 429 | Request quota exceeded. | Client sent too many requests in a short time window. |
| `E-SYS-50` | 503 | Processing error. | Internal supplier service fault; retried via exponential backoff. |
| `E-SYS-99` | 503 | Service unavailable. Try later. | Outage simulation by instructor; order stays `PENDING`. |

---

## 5. Resilience & Handling Unexpected Statuses

- **Idempotency:** Every reorder maintains a unique `requestId` (UUID) stored in `supplier_orders` before the call is made. The exact same `requestId` and `buyerRef` are reused on all retries.
- **Timeout & Retries:** Outbound calls time out at 3 seconds and retry up to 3 times with exponential backoff (500ms, 1000ms, 2000ms).
- **Outage Protection:** Any order that fails due to 503 or network failure remains in `PENDING` state and is picked up by `@Scheduled(fixedDelay = 15000)` to ensure zero lost reorders.
- **Unexpected Status Handling:** If LegacySupply returns an unexpected status code outside 10, 20, 30, 40, our adapter marks the order as `SUBMITTED`, avoids triggering premature inventory restocks, and logs the unknown code for inspection.
