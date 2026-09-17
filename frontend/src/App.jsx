import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080/api';

const Icons = {
  Shop: () => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z" />
      <line x1="3" y1="6" x2="21" y2="6" />
      <path d="M16 10a4 4 0 0 1-8 0" />
    </svg>
  ),
  Mouse: () => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <rect x="6" y="3" width="12" height="18" rx="6" />
      <line x1="12" y1="7" x2="12" y2="11" />
    </svg>
  ),
  Keyboard: () => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="4" width="20" height="16" rx="3" />
      <line x1="6" y1="8" x2="6.01" y2="8" />
      <line x1="10" y1="8" x2="10.01" y2="8" />
      <line x1="14" y1="8" x2="14.01" y2="8" />
      <line x1="18" y1="8" x2="18.01" y2="8" />
      <line x1="6" y1="12" x2="6.01" y2="12" />
      <line x1="10" y1="12" x2="10.01" y2="12" />
      <line x1="14" y1="12" x2="14.01" y2="12" />
      <line x1="18" y1="12" x2="18.01" y2="12" />
      <line x1="7" y1="16" x2="17" y2="16" />
    </svg>
  ),
  Hub: () => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="8" width="18" height="8" rx="2" />
      <line x1="7" y1="4" x2="7" y2="8" />
      <line x1="17" y1="4" x2="17" y2="8" />
      <circle cx="7.5" cy="12" r="1" fill="currentColor" />
      <circle cx="12" cy="12" r="1" fill="currentColor" />
      <circle cx="16.5" cy="12" r="1" fill="currentColor" />
    </svg>
  ),
  Cart: () => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="9" cy="21" r="1" />
      <circle cx="20" cy="21" r="1" />
      <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6" />
    </svg>
  ),
  Box: () => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z" />
      <polyline points="3.27 6.96 12 12.01 20.73 6.96" />
      <line x1="12" y1="22.08" x2="12" y2="12" />
    </svg>
  ),
  Activity: () => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.75" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="22 12 18 12 15 21 9 3 6 12 2 12" />
    </svg>
  ),
  Close: () => (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  )
};

const PRODUCT_DETAILS = {
  P100: {
    Icon: Icons.Mouse,
    priceNum: 100,
    price: '₱100.00',
    desc: 'Ergonomic 2.4GHz wireless optical mouse with quiet switches.'
  },
  P200: {
    Icon: Icons.Keyboard,
    priceNum: 200,
    price: '₱200.00',
    desc: 'Mechanical keyboard with tactile switches and backlighting.'
  },
  P300: {
    Icon: Icons.Hub,
    priceNum: 300,
    price: '₱300.00',
    desc: '7-in-1 multi-port adapter hub with 4K HDMI and Power Delivery.'
  }
};

export default function App() {
  const [inventory, setInventory] = useState([
    { productId: 'P100', name: 'Wireless Mouse', stock: 25 },
    { productId: 'P200', name: 'Mechanical Keyboard', stock: 10 },
    { productId: 'P300', name: 'USB-C Hub', stock: 0 },
  ]);
  const [cart, setCart] = useState([]);
  const [ordersHistory, setOrdersHistory] = useState([]);
  const [notifications, setNotifications] = useState([]);
  const [loading, setLoading] = useState(false);
  const [cancellingOrderId, setCancellingOrderId] = useState(null);

  const fetchInventory = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/inventory`);
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          const sorted = [...data].sort((a, b) => a.productId.localeCompare(b.productId));
          setInventory(sorted);
        }
      }
    } catch (err) {
    }
  };

  const fetchOrders = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/orders`);
      if (res.ok) {
        const data = await res.json();
        setOrdersHistory(data);
      }
    } catch (err) {
    }
  };

  const fetchNotifications = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/notifications`);
      if (res.ok) {
        const data = await res.json();
        setNotifications(data);
      }
    } catch (err) {
    }
  };

  const refreshAll = () => {
    fetchInventory();
    fetchOrders();
    fetchNotifications();
  };

  useEffect(() => {
    refreshAll();
  }, []);

  const addToCart = (productId) => {
    const existing = cart.find(item => item.productId === productId);
    if (existing) {
      setCart(cart.map(item =>
        item.productId === productId
          ? { ...item, quantity: item.quantity + 1 }
          : item
      ));
    } else {
      const product = inventory.find(i => i.productId === productId);
      const details = PRODUCT_DETAILS[productId] || PRODUCT_DETAILS.P100;
      setCart([...cart, {
        productId,
        name: product ? product.name : productId,
        priceNum: details.priceNum,
        quantity: 1
      }]);
    }
  };

  const updateCartQuantity = (productId, delta) => {
    setCart(cart.map(item => {
      if (item.productId === productId) {
        const newQty = item.quantity + delta;
        return newQty > 0 ? { ...item, quantity: newQty } : null;
      }
      return item;
    }).filter(Boolean));
  };

  const removeFromCart = (productId) => {
    setCart(cart.filter(item => item.productId !== productId));
  };

  const handleSubmitOrder = async (e) => {
    e.preventDefault();
    if (cart.length === 0) return;

    setLoading(true);

    const payload = {
      items: cart.map(item => ({
        productId: item.productId,
        quantity: item.quantity
      }))
    };

    try {
      const res = await fetch(`${API_BASE_URL}/orders`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      if (res.ok) {
        const data = await res.json();
        if (data.status === 'CONFIRMED') {
          setCart([]);
        }
      }
    } catch (err) {
      console.error('Order submission error:', err);
    } finally {
      setLoading(false);
      refreshAll();
    }
  };

  const handleCancelOrder = async (orderId) => {
    setCancellingOrderId(orderId);
    try {
      const res = await fetch(`${API_BASE_URL}/orders/${orderId}/cancel`, {
        method: 'POST'
      });
      if (res.ok) {
        refreshAll();
      }
    } catch (err) {
      console.error('Cancellation error:', err);
    } finally {
      setCancellingOrderId(null);
    }
  };

  const totalStock = inventory.reduce((acc, curr) => acc + curr.stock, 0);
  const cartTotalQty = cart.reduce((acc, curr) => acc + curr.quantity, 0);
  const cartTotalPrice = cart.reduce((acc, curr) => acc + (curr.priceNum * curr.quantity), 0);

  return (
    <div className="dashboard-container">
      <header className="top-header">
        <div className="brand-title-group">
          <div className="brand-icon-box">
            <Icons.Shop />
          </div>
          <div>
            <h1>Paton-og Shop</h1>
            <p>Computer Hardware &amp; Peripherals</p>
          </div>
        </div>

        <div className="header-chips">
          <div className="badge-chip">
            <span className="chip-dot"></span>
            Total Stock: <strong>{totalStock} units</strong>
          </div>
          <div className="badge-chip">
            Orders: <strong>{ordersHistory.length}</strong>
          </div>
          <div className="badge-chip">
            Activity: <strong>{notifications.length}</strong>
          </div>
        </div>
      </header>

      <div className="main-two-col">
        <section className="panel">
          <div className="panel-header">
            <h2 className="panel-title">
              <Icons.Box />
              <span>Available Products</span>
            </h2>
            <span className="panel-badge-count">{inventory.length} items</span>
          </div>

          <div className="products-stack">
            {inventory.map((item) => {
              const details = PRODUCT_DETAILS[item.productId] || PRODUCT_DETAILS.P100;
              const isLowStock = item.stock > 0 && item.stock <= 5;
              const isOutOfStock = item.stock === 0;
              const cartItem = cart.find(c => c.productId === item.productId);
              const ProductIcon = details.Icon;

              return (
                <div
                  key={item.productId}
                  className={`product-card ${isOutOfStock ? 'out-of-stock' : isLowStock ? 'low-stock' : ''}`}
                >
                  <div className="product-left-info">
                    <div className="product-icon-wrap">
                      <ProductIcon />
                    </div>
                    <div>
                      <div className="product-name-row">
                        <span className="product-name-text">{item.name}</span>
                        <span className="product-code-pill">{item.productId}</span>
                      </div>
                      <div className="product-desc-text">{details.desc}</div>
                    </div>
                  </div>

                  <div className="product-right-info">
                    <div className="product-price-tag">{details.price}</div>
                    <div>
                      {isOutOfStock ? (
                        <span className="stock-pill out">
                          <span className="status-dot"></span> Out of Stock
                        </span>
                      ) : isLowStock ? (
                        <span className="stock-pill low">
                          <span className="status-dot"></span> Low Stock: {item.stock} left
                        </span>
                      ) : (
                        <span className="stock-pill in">
                          <span className="status-dot"></span> {item.stock} in stock
                        </span>
                      )}
                    </div>
                    <button
                      className={`btn-add-cart ${cartItem ? 'in-cart' : ''}`}
                      onClick={() => addToCart(item.productId)}
                    >
                      {cartItem ? `In Cart (${cartItem.quantity})` : 'Add to Cart'}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h2 className="panel-title">
              <Icons.Cart />
              <span>Shopping Cart</span>
            </h2>
            <span className="panel-badge-count">
              {cart.length > 0 ? `${cartTotalQty} item${cartTotalQty > 1 ? 's' : ''}` : '0 items'}
            </span>
          </div>

          {cart.length === 0 ? (
            <div className="cart-empty-state">
              <Icons.Cart />
              <strong style={{ color: 'var(--text-primary)', fontSize: '0.88rem' }}>Your cart is empty</strong>
              <span>Add products from the catalog to build an order.</span>
            </div>
          ) : (
            <form onSubmit={handleSubmitOrder}>
              <div className="cart-items-list">
                {cart.map((item) => (
                  <div key={item.productId} className="cart-item-card">
                    <div>
                      <div className="cart-item-title">{item.name}</div>
                      <div className="cart-item-subtitle">₱{item.priceNum}.00 &times; {item.quantity} = <strong>₱{item.priceNum * item.quantity}.00</strong></div>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
                      <div className="cart-stepper">
                        <button
                          type="button"
                          className="btn-stepper"
                          onClick={() => updateCartQuantity(item.productId, -1)}
                        >
                          -
                        </button>
                        <span className="cart-qty-text">{item.quantity}</span>
                        <button
                          type="button"
                          className="btn-stepper"
                          onClick={() => updateCartQuantity(item.productId, 1)}
                        >
                          +
                        </button>
                      </div>
                      <button
                        type="button"
                        className="btn-remove-item"
                        title="Remove item"
                        onClick={() => removeFromCart(item.productId)}
                      >
                        <Icons.Close />
                      </button>
                    </div>
                  </div>
                ))}
              </div>

              <div className="cart-breakdown">
                <div>
                  <span style={{ fontSize: '0.74rem', color: 'var(--text-secondary)' }}>Order Total</span>
                  <div className="cart-breakdown-total">₱{cartTotalPrice}.00</div>
                </div>
                <div style={{ textAlign: 'right', fontSize: '0.74rem', color: 'var(--text-secondary)' }}>
                  Total Items: <strong>{cartTotalQty}</strong>
                </div>
              </div>

              <button
                type="submit"
                className="btn-submit-order"
                disabled={loading || cart.length === 0}
              >
                {loading ? 'Processing Order...' : `Submit Order • ₱${cartTotalPrice}.00`}
              </button>
            </form>
          )}
        </section>
      </div>

      <div className="bottom-two-col">
        <section className="panel">
          <div className="panel-header">
            <h2 className="panel-title">
              <Icons.Box />
              <span>Order History</span>
            </h2>
            <span className="panel-badge-count">{ordersHistory.length} orders</span>
          </div>

          <div className="table-wrap">
            {ordersHistory.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', padding: '1.5rem 0', textAlign: 'center' }}>
                No orders placed yet.
              </p>
            ) : (
              <table className="history-table">
                <thead>
                  <tr>
                    <th>Order ID</th>
                    <th>Items</th>
                    <th>Status</th>
                    <th>Details</th>
                    <th>Action</th>
                  </tr>
                </thead>
                <tbody>
                  {ordersHistory.map((ord) => {
                    const isConfirmed = ord.status === 'CONFIRMED';
                    const isCancelled = ord.status === 'CANCELLED';
                    const isRejected = ord.status === 'REJECTED';

                    return (
                      <tr key={ord.orderId}>
                        <td>
                          <span className="order-code-badge">{ord.orderId}</span>
                        </td>
                        <td>
                          {ord.items && ord.items.length > 0 ? (
                            ord.items.map(it => (
                              <span key={it.productId} className="item-chip">
                                {it.productId} &times; {it.quantity}
                              </span>
                            ))
                          ) : (
                            <span style={{ color: 'var(--text-secondary)' }}>—</span>
                          )}
                        </td>
                        <td>
                          {isConfirmed && <span className="status-pill confirmed">CONFIRMED</span>}
                          {isRejected && <span className="status-pill rejected">REJECTED</span>}
                          {isCancelled && <span className="status-pill cancelled">CANCELLED</span>}
                        </td>
                        <td style={{ fontSize: '0.72rem', color: 'var(--text-secondary)', maxWidth: '240px' }}>
                          {ord.reason}
                        </td>
                        <td>
                          {isConfirmed ? (
                            <button
                              className="btn-cancel-order"
                              onClick={() => handleCancelOrder(ord.orderId)}
                              disabled={cancellingOrderId === ord.orderId}
                            >
                              {cancellingOrderId === ord.orderId ? 'Restocking...' : 'Cancel'}
                            </button>
                          ) : (
                            <span style={{ fontSize: '0.72rem', color: 'var(--text-tertiary)' }}>—</span>
                          )}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            )}
          </div>
        </section>

        <section className="panel">
          <div className="panel-header">
            <h2 className="panel-title">
              <Icons.Activity />
              <span>Activity Feed</span>
            </h2>
            <span className="panel-badge-count">{notifications.length} events</span>
          </div>

          <div className="notif-feed-list">
            {notifications.length === 0 ? (
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', padding: '1.5rem 0', textAlign: 'center' }}>
                No events recorded yet.
              </p>
            ) : (
              notifications.map((n) => {
                const isReorder = n.message.toLowerCase().includes('reorder');
                const isRejected = n.message.toLowerCase().includes('rejected');
                const isConfirmed = n.message.toLowerCase().includes('confirmed');

                const typeClass = isReorder ? 'reorder' : isRejected ? 'rejected' : 'confirmed';
                const tagLabel = isReorder ? 'ALERT' : isRejected ? 'REJECTED' : 'CONFIRMED';

                return (
                  <div key={n.notificationId} className={`notif-card ${typeClass}`}>
                    <div className="notif-header">
                      <span className={`notif-tag ${typeClass}`}>
                        {tagLabel}
                      </span>
                      <span className="notif-time">
                        {n.createdAt ? new Date(n.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : 'Recent'}
                      </span>
                    </div>
                    <div className="notif-msg">{n.message}</div>
                  </div>
                );
              })
            )}
          </div>
        </section>
      </div>
    </div>
  );
}
