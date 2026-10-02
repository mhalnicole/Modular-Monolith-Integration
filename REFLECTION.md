# Lab 4: Tiangge Marketplace Reflection

### 1. Tiangge may deliver the same event more than once. Describe how your application recognises an event it has already handled, where that knowledge is stored, and whether it survives a restart.

Our application recognises redelivered events by tracking the unique `eventId` provided in each feed item. Before processing any incoming event, `ChannelFeedPoller` checks if the `eventId` already exists in our database through `ChannelProcessedEventRepository`. If the ID is already present, the event is immediately skipped without making duplicate orders or calls. This record is stored permanently in the `channel_processed_events` table inside our PostgreSQL database on Supabase. Because this deduplication state is persisted in the database rather than solely in memory, it completely survives application restarts.

---

### 2. Pick one order your application accepted. Trace it from the feed to your database to the stock update you sent Tiangge, naming each class it passes through.

When an order event like `TG-101` arrives, `ChannelFeedPoller` receives it from the `/feed` endpoint and forwards it to `ChannelOrderProcessor`. The processor checks product availability via `InventoryService` and creates a confirmed internal order through `OrderService.placeOrder`, which reserves the physical inventory and persists records in `OrderRepository` and `InventoryRepository`. Next, `ChannelOrderProcessor` dispatches an `ACCEPTED` decision with our generated `shopOrderId` back to Tiangge via `TianggeClient`, saving the mapping in `ChannelOrderRepository`. Finally, the stock decrease triggers a `StockChangedEvent`, causing `ChannelStockSyncListener` and `MarketplaceGatewayImpl` to send an immediate `PUT /stock` update to Tiangge so the marketplace stock remains in sync.

---

### 3. Your application now talks to two external systems with very different interfaces. Which of your modules know about Tiangge, which know about LegacySupply, and what would change if Tiangge were replaced by another marketplace?

In our architecture, only the `channel` module knows about Tiangge and its JSON REST protocol, while only the `supplier` module knows about LegacySupply and its XML RPC protocol. The core `shop` (Order) and `inventory` modules are completely unaware of either external system and only communicate through internal domain services and events. If Tiangge were replaced by another marketplace, our core business logic, order placement rules, inventory management, and supplier replenishment adapter would remain untouched. We would only need to adapt or replace the `channel` package with a new adapter tailored to the new marketplace's API contract and schemas.