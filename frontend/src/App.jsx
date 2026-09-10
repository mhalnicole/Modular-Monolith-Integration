import React, { useState, useEffect } from 'react';

const API_BASE_URL = 'http://localhost:8080/api';

const PRODUCT_DETAILS = {
  P100: {
    icon: '🖱️',
    price: '₱100.00',
    desc: 'Ergonomic 2.4GHz wireless mouse with silent clicks and high precision.'
  },
  P200: {
    icon: '⌨️',
    price: '₱200.00',
    desc: 'Tactile blue switches mechanical keyboard with RGB backlighting.'
  },
  P300: {
    icon: '🔌',
    price: '₱300.00',
    desc: '7-in-1 multi-port adapter hub with 4K HDMI and fast power delivery.'
  }
};

export default function App() {
  const [inventory, setInventory] = useState([
    { productId: 'P100', name: 'Wireless Mouse', stock: 25 },
    { productId: 'P200', name: 'Mechanical Keyboard', stock: 10 },
    { productId: 'P300', name: 'USB-C Hub', stock: 0 },
  ]);
  const [selectedProduct, setSelectedProduct] = useState('P100');
  const [quantity, setQuantity] = useState(1);
  const [loading, setLoading] = useState(false);
  const [ordersHistory, setOrdersHistory] = useState([]);

  // Fetch live inventory from Supabase via Spring Boot
  const fetchInventory = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/inventory`);
      if (res.ok) {
        const data = await res.json();
        if (Array.isArray(data) && data.length > 0) {
          // Sort strictly by productId P100, P200, P300
          const sorted = [...data].sort((a, b) => a.productId.localeCompare(b.productId));
          setInventory(sorted);
        }
      }
    } catch (err) {
      // Backend offline or loading
    }
  };

  // Fetch past orders from Supabase via Spring Boot
  const fetchOrders = async () => {
    try {
      const res = await fetch(`${API_BASE_URL}/orders`);
      if (res.ok) {
        const data = await res.json();
        setOrdersHistory(data);
      }
    } catch (err) {
      // Backend offline or loading
    }
  };

  useEffect(() => {
    fetchInventory();
    fetchOrders();
  }, []);

  const handlePlaceOrder = async (e) => {
    e.preventDefault();
    if (quantity <= 0) return;

    setLoading(true);

    const payload = {
      productId: selectedProduct,
      quantity: parseInt(quantity, 10),
    };

    try {
      const res = await fetch(`${API_BASE_URL}/orders`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify(payload),
      });

      if (!res.ok) {
        throw new Error(`Server returned HTTP ${res.status}`);
      }

      await res.json();

      // Refresh stock & orders table immediately
      fetchInventory();
      fetchOrders();
    } catch (err) {
      console.error('Order error:', err);
    } finally {
      setLoading(false);
    }
  };

  const currentItem = inventory.find((i) => i.productId === selectedProduct) || { stock: 0, name: '' };
  const currentDetails = PRODUCT_DETAILS[selectedProduct] || PRODUCT_DETAILS.P100;
  const totalStock = inventory.reduce((acc, curr) => acc + curr.stock, 0);

  return (
    <div className="dashboard-container">
      {/* Brand Header */}
      <header className="top-header">
        <div className="brand-title-group">
          <div className="brand-icon-box">🛍️</div>
          <div>
            <h1>Paton-og Shop</h1>
            <p>Computer Hardware &amp; Peripherals Store</p>
          </div>
        </div>

        <div className="header-chips">
          <div className="badge-chip">
            Total Stock: <strong>{totalStock} units</strong>
          </div>
          <div className="badge-chip">
            Orders Placed: <strong>{ordersHistory.length}</strong>
          </div>
        </div>
      </header>

      {/* Row 1: 2-Column Section (Available Products & Place an Order) */}
      <div className="main-two-col">
        {/* Left Column: Available Products Catalog */}
        <section className="panel">
          <div className="panel-title">
            <span>Available Products</span>
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: 500 }}>
              Click to select
            </span>
          </div>

          <div className="products-stack">
            {inventory.map((item) => {
              const details = PRODUCT_DETAILS[item.productId] || PRODUCT_DETAILS.P100;
              const isSelected = selectedProduct === item.productId;

              return (
                <div
                  key={item.productId}
                  className={`product-row-card ${isSelected ? 'active' : ''}`}
                  onClick={() => setSelectedProduct(item.productId)}
                >
                  <div className="product-left-info">
                    <div className="product-icon-wrap">{details.icon}</div>
                    <div>
                      <div className="product-name-text">{item.name}</div>
                      <div className="product-desc-text">{details.desc}</div>
                    </div>
                  </div>

                  <div className="product-right-info">
                    <div className="product-price-tag">{details.price}</div>
                    <span className={`stock-tag-pill ${item.stock > 0 ? 'in' : 'out'}`}>
                      {item.stock > 0 ? `${item.stock} in stock` : 'Out of stock'}
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        </section>

        {/* Right Column: Order Placement Form */}
        <section className="panel">
          <h2 className="panel-title">Place an Order</h2>

          <form onSubmit={handlePlaceOrder}>
            <div className="form-input-grid">
              <div>
                <label className="input-label" htmlFor="product-select">
                  Product
                </label>
                <select
                  id="product-select"
                  className="custom-select"
                  value={selectedProduct}
                  onChange={(e) => setSelectedProduct(e.target.value)}
                >
                  {inventory.map((item) => {
                    const details = PRODUCT_DETAILS[item.productId] || PRODUCT_DETAILS.P100;
                    return (
                      <option key={item.productId} value={item.productId}>
                        {item.name} ({details.price}) — {item.stock > 0 ? `${item.stock} left` : 'Out of stock'}
                      </option>
                    );
                  })}
                </select>
              </div>

              <div>
                <label className="input-label" htmlFor="quantity-input">
                  Quantity
                </label>
                <input
                  id="quantity-input"
                  type="number"
                  min="1"
                  className="custom-input"
                  value={quantity}
                  onChange={(e) => setQuantity(Math.max(1, parseInt(e.target.value) || 1))}
                  required
                />
              </div>
            </div>

            <div className="selection-summary">
              <span>Selected: <strong>{currentItem.name}</strong> &bull; Price: <strong>{currentDetails.price}</strong></span>
              <span>
                Available Stock:{' '}
                <strong style={{ color: currentItem.stock > 0 ? 'var(--success)' : 'var(--danger)' }}>
                  {currentItem.stock} units
                </strong>
              </span>
            </div>

            <button type="submit" className="btn-place-order" disabled={loading}>
              {loading ? 'Processing Order...' : `Submit Order (${quantity} unit${quantity > 1 ? 's' : ''})`}
            </button>
          </form>
        </section>
      </div>

      {/* Row 2: Recent Orders Table (Full Width across the bottom!) */}
      {ordersHistory.length > 0 && (
        <section className="panel" style={{ marginTop: '1.25rem' }}>
          <h2 className="panel-title">Recent Orders</h2>
          <div className="table-wrap">
            <table className="history-table">
              <thead>
                <tr>
                  <th>Order ID</th>
                  <th>Product</th>
                  <th>Quantity</th>
                  <th>Status</th>
                  <th>Order Details / Reason</th>
                  <th>Time</th>
                </tr>
              </thead>
              <tbody>
                {ordersHistory.map((ord) => (
                  <tr key={ord.orderId}>
                    <td><code>{ord.orderId}</code></td>
                    <td><strong>{ord.productId}</strong></td>
                    <td>{ord.quantity}</td>
                    <td>
                      <span className={ord.status === 'CONFIRMED' ? 'pill-confirmed' : 'pill-rejected'}>
                        {ord.status}
                      </span>
                    </td>
                    <td>{ord.reason}</td>
                    <td style={{ color: 'var(--text-muted)' }}>
                      {ord.createdAt ? new Date(ord.createdAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' }) : 'Recent'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      )}
    </div>
  );
}
